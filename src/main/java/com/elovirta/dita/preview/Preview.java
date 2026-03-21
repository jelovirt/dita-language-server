package com.elovirta.dita.preview;

import java.io.IOException;
import java.io.StringWriter;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Preview {

  private static final Logger logger = LoggerFactory.getLogger(Preview.class);

  private final Processor processor;
  private final XsltExecutable previewCompiler;

  public Preview(Processor processor) {
    this.processor = processor;
    try (var in = getClass().getResourceAsStream("/xslt/preview.xsl")) {
      this.previewCompiler =
          processor.newXsltCompiler().compile(new StreamSource(in, "classpath:/xslt/preview.xsl"));
    } catch (SaxonApiException | IOException e) {
      throw new RuntimeException("Failed to parse classpath:/xslt/preview.xsl", e);
    }
  }

  public String generatePreview(XdmNode doc) {
    var previewTransformer = previewCompiler.load30();
    try (var out = new StringWriter()) {
      var serializer = processor.newSerializer(out);
      previewTransformer.transform(doc.asSource(), serializer);
      return out.toString();
    } catch (SaxonApiException | IOException e) {
      throw new RuntimeException("Failed to run XSLT for preview: " + e.getMessage(), e);
    }
  }
}
