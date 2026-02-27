package com.elovirta.dita;

import com.elovirta.dita.xml.DITAGrammarCacheManager;
import com.elovirta.dita.xml.XmlSerializer;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.*;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.s9api.*;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
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
    System.err.println(contentWithLocation);
    try (var in = new CharArrayReader(contentWithLocation)) {
      var inputSource = new InputSource(in);
      inputSource.setSystemId(uri.toString());
      var documentBuilder = processor.newDocumentBuilder();
      documentBuilder.setDTDValidation(true);

      return cacheManager.withParser(
          parser -> {
            var diagnostics = new ArrayList<Diagnostic>();
            parser.setErrorHandler(
                new ErrorHandler() {
                  @Override
                  public void warning(SAXParseException exception) throws SAXException {
                    diagnostics.add(toDiagnostic(exception, DiagnosticSeverity.Warning));
                  }

                  @Override
                  public void error(SAXParseException exception) throws SAXException {
                    if (!exception.getMessage().startsWith("Attribute \"xmlns:loc")
                        && !exception.getMessage().startsWith("Attribute \"loc")) {
                      diagnostics.add(toDiagnostic(exception, DiagnosticSeverity.Error));
                    }
                  }

                  @Override
                  public void fatalError(SAXParseException exception) throws SAXException {
                    //                  throw new SAXException(exception);
                    if (!exception.getMessage().startsWith("Attribute \"xmlns:loc")
                        && !exception.getMessage().startsWith("Attribute \"loc")) {
                      diagnostics.add(toDiagnostic(exception, DiagnosticSeverity.Error));
                    }
                    //          logger.error(exception.getMessage(), exception);
                  }
                });
            var doc = documentBuilder.build(new SAXSource(parser, inputSource));
            diagnostics.addAll(serializer.getDiagnostics());
            return new ParseResult(doc, diagnostics);
          });
    } catch (SaxonApiException e) {
      throw new RuntimeException(e);
    }
  }

  private static Diagnostic toDiagnostic(SAXParseException exception, DiagnosticSeverity severity) {
    var start = new Position(exception.getLineNumber() - 1, exception.getColumnNumber());
    var end = new Position(exception.getLineNumber() - 1, exception.getColumnNumber());
    var msg = exception.getMessage();
    if (msg.startsWith("The content of element type")) {
      // The content of element type "topic" must match
      // "(title,titlealts?,(shortdesc|abstract)?,prolog?,body?,related-links?,topic*)".
      // TODO: position is the close delimiter of end tag. Should be start tag?
    } else if (msg.startsWith("Element type") && msg.endsWith(" must be declared.")) {
      // TODO: position is the close delimiter of end tag. Should be start tag?
    } else if (msg.startsWith("Attribute")
        && msg.contains("is required and must be specified for element type")) {
      // Attribute "id" is required and must be specified for element type "topic".
      // TODO: position is the close delimiter of end tag. Should be start tag?
    } else if (msg.startsWith("Attribute") && msg.contains("must be declared for element type")) {
      // Attribute "x" must be declared for element type "topic".
      // TODO: position is the close delimiter of end tag. Should be start tag?
    } else if (msg.startsWith("Document root element") && msg.contains("must match DOCTYPE root")) {
      // Document root element "topic", must match DOCTYPE root "tcopic".
      // TODO: position is the close delimiter of start tag. Should be start tag?
    }

    return new Diagnostic(new Range(start, end), msg, severity, exception.getSystemId());
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
