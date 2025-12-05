package com.elovirta.dita;

import com.elovirta.dita.xml.XmlSerializer;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.*;
import net.sf.saxon.s9api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlresolver.*;
import org.xmlresolver.logging.DefaultLogger;

public class DitaParser {

  private static final Logger logger = LoggerFactory.getLogger(DitaParser.class);

  private final Resolver catalogResolver;
  private final Processor processor;
  private final XsltExecutable mergeExecutable;

  public DitaParser() {
    XMLResolverConfiguration config = new XMLResolverConfiguration();
    config.setFeature(ResolverFeature.PREFER_PUBLIC, true);
    config.setFeature(ResolverFeature.CACHE_DIRECTORY, null);
    config.setFeature(ResolverFeature.CACHE_UNDER_HOME, false);
    config.setFeature(
        ResolverFeature.RESOLVER_LOGGER_CLASS, DefaultLogger.class.getCanonicalName());
    config.setFeature(ResolverFeature.DEFAULT_LOGGER_LOG_LEVEL, "info");
    config.setFeature(ResolverFeature.CATALOG_FILES, List.of("classpath:/schemas/catalog.xml"));
    this.catalogResolver = new Resolver(config);

    Configuration configuration = Configuration.newConfiguration();
    //    configuration.setResourceResolver(new CatalogResourceResolver(catalogResolver));
    var resolver = new CatalogResourceResolver(catalogResolver);
    configuration.setResourceResolver(
        (ResourceRequest request) -> {
          logger.info("Resolve {} {}", request.baseUri, request.uri);
          return resolver.resolve(request);
        });
    this.processor = new Processor(configuration);
    try {
      this.mergeExecutable =
          processor
              .newXsltCompiler()
              .compile(Paths.get(getClass().getResource("/xslt/merge.xsl").toURI()).toFile());
    } catch (SaxonApiException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public XdmNode parse(String content, URI uri) {
    var contentWithLocation = addLocation(content);
    try (var in = new CharArrayReader(contentWithLocation)) {
      var src = processor.newDocumentBuilder().build(new StreamSource(in, uri.toString()));
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

  private char[] addLocation(String content) {
    var serializer = new XmlSerializer();
    try (CharArrayWriter output = new CharArrayWriter()) {
      serializer.serialize(content, output);
      return output.toCharArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
