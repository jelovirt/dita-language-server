package com.elovirta.dita;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.CatalogResourceResolver;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;
import org.xmlresolver.Resolver;
import org.xmlresolver.ResolverFeature;
import org.xmlresolver.XMLResolverConfiguration;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringReader;
import java.util.List;

public class DitaParser {

    private final Resolver catalogResolver;
    private final Processor processor;

    public DitaParser() {
        XMLResolverConfiguration config = new XMLResolverConfiguration();
        config.setFeature(ResolverFeature.PREFER_PUBLIC, true);
//            config.setFeature(ResolverFeature.CACHE_DIRECTORY, null);
//            config.setFeature(ResolverFeature.CACHE_UNDER_HOME, false);
        config.setFeature(ResolverFeature.RESOLVER_LOGGER_CLASS, "org.xmlresolver.logging.DefaultLogger");
        config.setFeature(ResolverFeature.DEFAULT_LOGGER_LOG_LEVEL, "debug");
//            config.setFeature(ResolverFeature.CLASSPATH_CATALOGS, true);

        config.setFeature(ResolverFeature.CATALOG_FILES, List.of("classpath:/schemas/catalog.xml"));
//            config.setFeature(ResolverFeature.CATALOG_FILES, List.of("file:/Users/jarno.elovirta/work/github.com/jelovirt/dita-lsp/src/main/resources/schemas/catalog.xml"));


//            config.addCatalog("classpath:/schemas/catalog.xml");
        this.catalogResolver = new Resolver(config);

        Configuration configuration = Configuration.newConfiguration();
//        configuration.setResourceResolver(new CatalogResourceResolver(catalogResolver));
        configuration.setResourceResolver(new ResourceResolver() {
            @Override
            public Source resolve(ResourceRequest request) throws XPathException {
                try {
                    return catalogResolver.resolve(request.uri, request.baseUri);
                } catch (TransformerException e) {
                    throw new RuntimeException(e);
                }
            }
        });
//        configureSaxonExtensions(config);
//        configureSaxonCollationResolvers(config);;
        this.processor = new Processor(configuration);
    }

    public XdmNode parse(String content) {
        try (var in = new StringReader(content)) {
            return processor.newDocumentBuilder().build(new StreamSource(in));
        } catch (SaxonApiException e) {
            throw new RuntimeException(e);
        }
    }

}
