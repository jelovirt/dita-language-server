package com.elovirta.dita.xml;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlresolver.Resolver;

/**
 * Manages cached DTD grammars for SAX parsing. Grammars are cached by public identifier and lazily
 * loaded on first use.
 */
public class DITAGrammarCacheManager {

  private static final List<XMLDTDDescription> DESCRIPTIONS =
      List.of(
          new XMLDTDDescription("-//OASIS//DTD DITA Topic//EN", null, null, null, "topic"),
          new XMLDTDDescription(
              "-//OASIS//DTD DITA Subject Scheme Map//EN", null, null, null, "subjectScheme"));

  private final ConcurrentHashMap<String, Grammar> grammarsByPublicId;
  private final XMLGrammarPoolImpl sharedPool;
  private final ReadWriteLock poolLock;
  private final Resolver catalogResolver;

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
  }

  /**
   * Create a SAX parser configured to use cached grammars. The parser will automatically cache new
   * grammars as it encounters them.
   */
  public SAXParser createParser() {
    SAXParser parser = new SAXParser();

    try {
      parser.setProperty("http://apache.org/xml/properties/internal/grammar-pool", sharedPool);
      parser.setFeature("http://xml.org/sax/features/validation", true);
      parser.setFeature("http://xml.org/sax/features/namespaces", true);
      //      parser.setFeature("http://apache.org/xml/sax/features/external-general-entities",
      // true);
      //      parser.setFeature("http://apache.org/xml/sax/features/external-parameter-entities",
      // true);
      //      parser.setFeature("http://xml.org/sax/features/load-external-dtd", true);
    } catch (SAXException e) {
      throw new RuntimeException("Failed to configure SAX parser", e);
    }
    parser.setEntityResolver(catalogResolver);
    return parser;
  }
}
