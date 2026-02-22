package com.elovirta.dita.xml;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.xerces.impl.dtd.XMLDTDDescription;
import org.apache.xerces.parsers.SAXParser;
import org.apache.xerces.parsers.XMLGrammarPreparser;
import org.apache.xerces.util.XMLGrammarPoolImpl;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xmlresolver.Resolver;

/**
 * Manages cached DTD grammars for SAX parsing. Grammars are cached by public identifier and lazily
 * loaded on first use.
 */
public class DITAGrammarCacheManager {

  private static final Logger logger = LoggerFactory.getLogger(DITAGrammarCacheManager.class);

  private static final List<XMLDTDDescription> DESCRIPTIONS =
      List.of(
          new XMLDTDDescription("-//OASIS//DTD DITA Topic//EN", null, null, null, "topic"),
          new XMLDTDDescription(
              "-//OASIS//DTD DITA Subject Scheme Map//EN", null, null, null, "subjectScheme"));

  private final ConcurrentHashMap<String, Grammar> grammarsByPublicId;
  private final XMLGrammarPoolImpl sharedPool;
  private final ReadWriteLock poolLock;
  private final Resolver catalogResolver;

  private final BlockingQueue<SAXParser> pool;
  private final int capacity;
  private final long acquireTimeoutMs;
  private final AtomicInteger created = new AtomicInteger(0);

  public DITAGrammarCacheManager(Resolver catalogResolver) {
    this.catalogResolver = catalogResolver;
    this.grammarsByPublicId = new ConcurrentHashMap<>();
    this.sharedPool = new XMLGrammarPoolImpl();
    this.poolLock = new ReentrantReadWriteLock();

    XMLGrammarPreparser preparser = new XMLGrammarPreparser();
    preparser.registerPreparser(XMLGrammarDescription.XML_DTD, null);
    preparser.setGrammarPool(sharedPool);
    preparser.setEntityResolver(
        resourceIdentifier -> {
          try {
            InputSource inputSource =
                catalogResolver.resolveEntity(
                    resourceIdentifier.getPublicId(), resourceIdentifier.getExpandedSystemId());
            return new XMLInputSource(
                inputSource.getPublicId(),
                inputSource.getSystemId(),
                null,
                inputSource.getByteStream(),
                null);
          } catch (SAXException e) {
            throw new XNIException(e);
          }
        });

    for (XMLDTDDescription resource : DESCRIPTIONS) {
      try {
        var input =
            catalogResolver.resolveEntity(resource.getPublicId(), resource.getExpandedSystemId());
        XMLInputSource inputSource =
            new XMLInputSource(
                input.getPublicId(),
                input.getSystemId(),
                input.getSystemId(),
                input.getByteStream(),
                input.getEncoding());
        preparser.preparseGrammar(XMLGrammarDescription.XML_DTD, inputSource);
      } catch (IOException | SAXException e) {
        throw new RuntimeException(e);
      }
    }

    this.capacity = 10;
    this.acquireTimeoutMs = 2000;
    this.pool = new ArrayBlockingQueue<>(capacity);
  }

  /** Acquire a parser from a parser pool. */
  public <T, E extends Exception> T withParser(CheckedFunction<SAXParser, T, E> task) throws E {
    SAXParser parser = acquire();
    try {
      return task.apply(parser);
    } finally {
      release(parser);
    }
  }

  /**
   * Create a SAX parser configured to use cached grammars. The parser will automatically cache new
   * grammars as it encounters them.
   */
  private SAXParser createParser() throws SAXNotSupportedException, SAXNotRecognizedException {
    SAXParser parser = new SAXParser();
    var grammarPool = new XMLGrammarPoolImpl();
    grammarPool.cacheGrammars(
        XMLGrammarDescription.XML_DTD,
        sharedPool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_DTD));
    parser.setProperty("http://apache.org/xml/properties/internal/grammar-pool", grammarPool);
    parser.setFeature("http://xml.org/sax/features/validation", true);
    parser.setFeature("http://xml.org/sax/features/namespaces", true);
    //      parser.setFeature("http://apache.org/xml/sax/features/external-general-entities",
    // true);
    //      parser.setFeature("http://apache.org/xml/sax/features/external-parameter-entities",
    // true);
    //      parser.setFeature("http://xml.org/sax/features/load-external-dtd", true);
    parser.setEntityResolver(catalogResolver);
    return parser;
  }

  @FunctionalInterface
  public interface CheckedFunction<A, R, E extends Exception> {
    R apply(A a) throws E;
  }

  /**
   * Acquire a parser from the pool. Creates a new one if the pool is not yet at capacity and no
   * parsers are immediately available. Blocks up to acquireTimeoutMs if the pool is exhausted.
   *
   * @throws ParserUnavailableException if no parser becomes available within the timeout
   */
  private SAXParser acquire() {
    logger.debug("Acquire parser ({})", pool.size());
    SAXParser parser = pool.poll();
    if (parser != null) {
      return parser;
    }
    if (created.get() < capacity) {
      int slot = created.getAndIncrement();
      if (slot < capacity) {
        try {
          return createParser();
        } catch (SAXException e) {
          created.decrementAndGet();
          throw new ParserUnavailableException("Failed to create SAXParser", e);
        }
      } else {
        created.decrementAndGet();
      }
    }
    try {
      parser = pool.poll(acquireTimeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ParserUnavailableException("Interrupted while waiting for a SAXParser", e);
    }
    if (parser == null) {
      throw new ParserUnavailableException(
          "No SAXParser available within " + acquireTimeoutMs + "ms (pool size: " + capacity + ")");
    }
    return parser;
  }

  /**
   * Return a parser to the pool. The parser must have been acquired from this pool. Resetting
   * handler state is the caller's responsibility — the pool does not reset the parser on return.
   */
  private void release(SAXParser parser) {
    logger.debug("Releasing parser ({})", pool.size());
    if (parser == null) {
      return;
    }
    parser.reset();
    // TODO: Check if this parser parsed any new grammars and add them to shared pool
    boolean returned = pool.offer(parser);
    if (!returned) {
      logger.info("Pool is full, discard parser");
    }
  }

  public static class ParserUnavailableException extends RuntimeException {
    public ParserUnavailableException(String message, Throwable cause) {
      super(message, cause);
    }

    public ParserUnavailableException(String message) {
      super(message);
    }
  }
}
