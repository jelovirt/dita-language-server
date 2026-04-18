package com.elovirta.dita.preview;

import static org.junit.jupiter.api.Assertions.*;

import com.elovirta.dita.KeyManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.sapling.Saplings;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class PreviewTest {

  private final Processor processor;
  private final Preview preview;

  public PreviewTest() {
    KeyManager keyManager = new KeyManager();
    Configuration configuration = Configuration.newConfiguration();
    //    configuration.setResourceResolver(new CatalogResourceResolver(catalogResolver));
    //        var resolver = new CatalogResourceResolver(catalogResolver);
    processor = new Processor(configuration);
    preview = new Preview(processor, keyManager);
  }

  @Test
  void generatePreview() throws SaxonApiException, IOException {
    var src =
        Saplings.doc()
            .withChild(Saplings.elem("topic").withAttr("class", "- topic/topic "))
            .toXdmNode(processor);
    var act = preview.generatePreview(src);
    try {
      try (var in = getClass().getResourceAsStream("/preview/topic.html")) {
        var exp = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(exp, act);
      }
    } catch (AssertionFailedError e) {
      Files.writeString(Path.of("src/test/resources/preview/topic.html"), act);
      throw e;
    }
  }
}
