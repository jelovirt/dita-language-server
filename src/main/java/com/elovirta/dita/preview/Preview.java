package com.elovirta.dita.preview;

import com.elovirta.dita.KeyManager;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.streams.Predicates;
import net.sf.saxon.s9api.streams.Steps;
import net.sf.saxon.sapling.SaplingElement;
import net.sf.saxon.sapling.Saplings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Preview {

  private static final Logger logger = LoggerFactory.getLogger(Preview.class);

  private final Processor processor;
  private final KeyManager keyManager;
  private final XsltExecutable previewCompiler;

  public Preview(Processor processor, KeyManager keyManager) {
    this.processor = processor;
    this.keyManager = keyManager;
    try (var in = getClass().getResourceAsStream("/xslt/preview.xsl")) {
      this.previewCompiler =
          processor.newXsltCompiler().compile(new StreamSource(in, "classpath:/xslt/preview.xsl"));
    } catch (SaxonApiException | IOException e) {
      throw new RuntimeException("Failed to parse classpath:/xslt/preview.xsl", e);
    }
  }

  public String generatePreview(XdmNode doc) {
    var previewTransformer = previewCompiler.load30();

    Set<Map.Entry<String, KeyManager.KeyDefinition>> keys = keyManager.keys();
    //    logger.info("Generating preview for {} keys", keys.size());
    //    var keys =
    // doc.select(Steps.descendantOrSelf(Predicates.isElement()).then(Steps.attribute("keyref")))
    //            .map(XdmItem::getStringValue)
    //            .flatMap(value -> Stream.of(value.trim().split("\\s+")))
    //            .collect(Collectors.toSet());
    var keysElems =
        doc.select(
                    Steps.descendantOrSelf(Predicates.isElement())
                        .where(
                            Predicates.hasAttribute("keyref")
                                .or(Predicates.hasAttribute("conkeyref"))))
                .exists()
            ? keys.stream()
                .map(Map.Entry::getValue)
                .map(
                    key -> {
                      SaplingElement keyref = Saplings.elem("keyref").withAttr("key", key.key());
                      if (key.navtitle() != null) {
                        keyref = keyref.withText(key.navtitle());
                      } else if (key.text() != null) {
                        keyref = keyref.withText(key.text());
                      }
                      if (key.target() != null) {
                        keyref = keyref.withAttr("href", key.target().toString());
                      }
                      return keyref;
                    })
                .toArray(SaplingElement[]::new)
            : null;

    try (var out = new StringWriter()) {
      var serializer = processor.newSerializer(out);
      if (keysElems != null && keysElems.length > 0) {
        var keyrefDoc =
            Saplings.doc()
                .withChild(Saplings.elem("keyrefs").withChild(keysElems))
                .toXdmNode(processor);
        previewTransformer.setStylesheetParameters(
            Map.of(QName.fromClarkName("{}keyrefs"), keyrefDoc));
      }
      previewTransformer.transform(doc.asSource(), serializer);
      return out.toString();
    } catch (SaxonApiException | IOException e) {
      throw new RuntimeException("Failed to run XSLT for preview: " + e.getMessage(), e);
    }
  }
}
