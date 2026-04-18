import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.xerces.parsers.AbstractSAXParser;
import org.cyberneko.dtd.DTDConfiguration;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.nio.file.Path;

public class DtdProcessor {

    private final Processor processor;
    private final XsltExecutable stylesheet;

    public DtdProcessor() throws Exception {
        processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();
        try (InputStream xsl = DtdProcessor.class.getResourceAsStream("/dtdx2dtd.xsl")) {
            if (xsl == null) {
                throw new IllegalStateException("Could not find dtdx2dtd.xsl on classpath");
            }
            stylesheet = compiler.compile(new StreamSource(xsl));
        }
    }

    public void process(Path inputDtd, Path outputFile) throws Exception {
        DTDConfiguration config = new DTDConfiguration();

        AbstractSAXParser saxParser = new AbstractSAXParser(config) {};

        SAXSource source = new SAXSource(
                saxParser,
                new InputSource(inputDtd.toUri().toString())
        );

        Serializer out = processor.newSerializer(outputFile.toFile());
        out.setOutputProperty(Serializer.Property.METHOD, "text");

        XsltTransformer transformer = stylesheet.load();
        transformer.setSource(source);
        transformer.setDestination(out);
        transformer.transform();
    }
}