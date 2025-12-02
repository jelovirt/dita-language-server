package com.elovirta.dita;

import com.elovirta.dita.xml.XmlSerializer;
import java.io.*;
import java.util.List;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.*;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.xmlresolver.*;
import org.xmlresolver.logging.DefaultLogger;

public class DitaParser {

  private final Resolver catalogResolver;
  private final Processor processor;

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
    configuration.setResourceResolver(new CatalogResourceResolver(catalogResolver));
    this.processor = new Processor(configuration);
  }

  public XdmNode parse(String content) {
    var contentWithLocation = addLocation(content);
    try (var in = new CharArrayReader(contentWithLocation)) {
      return processor.newDocumentBuilder().build(new StreamSource(in));
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
