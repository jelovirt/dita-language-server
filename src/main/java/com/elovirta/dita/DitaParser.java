package com.elovirta.dita;

import com.elovirta.dita.xml.DITAGrammarCacheManager;
import com.elovirta.dita.xml.XmlSerializer;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.*;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.s9api.*;
import org.eclipse.lsp4j.Diagnostic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xmlresolver.*;
import org.xmlresolver.logging.DefaultLogger;

public class DitaParser {

  private static final Logger logger = LoggerFactory.getLogger(DitaParser.class);

  private final Resolver catalogResolver;
  private final Processor processor;
  private final XsltExecutable mergeExecutable;
  private final DITAGrammarCacheManager cacheManager;

  public DitaParser() {
    XMLResolverConfiguration config = new XMLResolverConfiguration();
    config.setFeature(ResolverFeature.PREFER_PUBLIC, true);
    //    config.setFeature(ResolverFeature.CACHE_DIRECTORY, null);
    //    config.setFeature(ResolverFeature.CACHE_UNDER_HOME, false);
    config.setFeature(
        ResolverFeature.RESOLVER_LOGGER_CLASS, DefaultLogger.class.getCanonicalName());
    //    config.setFeature(ResolverFeature.DEFAULT_LOGGER_LOG_LEVEL, "info");
    config.setFeature(ResolverFeature.LOGGER_LOG_LEVEL, "info");
    config.setFeature(ResolverFeature.CATALOG_FILES, List.of("classpath:/schemas/catalog.xml"));
    this.catalogResolver = new Resolver(config);
    this.cacheManager = new DITAGrammarCacheManager(this.catalogResolver);
    Configuration configuration = Configuration.newConfiguration();
    //    configuration.setResourceResolver(new CatalogResourceResolver(catalogResolver));
    var resolver = new CatalogResourceResolver(catalogResolver);
    configuration.setResourceResolver(
        (ResourceRequest request) -> {
          var uri = URI.create(request.uri);
          if (uri.getScheme().equals("file")) {
            var extension = Utils.getExtension(uri.getPath());
            if (extension != null && (extension.equals("dita") || extension.equals("ditamap"))) {
              try {
                var content = Files.readString(Paths.get(uri)).toCharArray();
                var doc = parseDocument(content, uri).document();
                return doc.getUnderlyingNode();
              } catch (IOException e) {
                throw new UncheckedIOException("Failed to read " + request.uri, e);
              }
            }
          }
          return resolver.resolve(request);
        });
    this.processor = new Processor(configuration);
    try (var in = getClass().getResourceAsStream("/xslt/merge.xsl")) {
      this.mergeExecutable =
          processor.newXsltCompiler().compile(new StreamSource(in, "classpath:/xslt/merge.xsl"));
    } catch (SaxonApiException | IOException e) {
      throw new RuntimeException("Failed to parse classpath:/xslt/merge.xsl", e);
    }
  }

  public Processor getProcessor() {
    return processor;
  }

  public record ParseResult(XdmNode document, List<Diagnostic> diagnostics) {}

  public ParseResult parse(String content, URI uri) {
    return parseDocument(content.toCharArray(), uri);
  }

  public XdmNode mergeMap(XdmNode src) {
    try {
      var transformer = this.mergeExecutable.load();
      transformer.setSource(src.getUnderlyingNode());
      XdmDestination dst = new XdmDestination();
      transformer.setDestination(dst);
      transformer.transform();
      return dst.getXdmNode();
    } catch (SaxonApiException e) {
      throw new RuntimeException(e);
    }
  }

  private ParseResult parseDocument(char[] content, URI uri) {
    //    var contentWithLocation = addLocation(content);
    char[] contentWithLocation;
    var serializer = new XmlSerializer();
    try (CharArrayWriter output = new CharArrayWriter()) {
      serializer.serialize(content, output);
      contentWithLocation = output.toCharArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try (var in = new CharArrayReader(contentWithLocation)) {
      var inputSource = new InputSource(in);
      inputSource.setSystemId(uri.toString());
      var parser = cacheManager.createParser();
      return new ParseResult(
          processor
              .newDocumentBuilder()
              .build(
                  new SAXSource(parser, inputSource)
                  //                  new StreamSource(in, uri.toString())
                  ),
          serializer.getDiagnostics());
    } catch (SaxonApiException e) {
      throw new RuntimeException(e);
    }
  }

  //  private char[] addLocation(char[] content) {
  //    var serializer = new XmlSerializer();
  //    try (CharArrayWriter output = new CharArrayWriter()) {
  //      serializer.serialize(content, output);
  //      return output.toCharArray();
  //    } catch (IOException e) {
  //      throw new RuntimeException(e);
  //    }
  //  }
}
