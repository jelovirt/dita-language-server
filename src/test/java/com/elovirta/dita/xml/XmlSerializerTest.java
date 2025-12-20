package com.elovirta.dita.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class XmlSerializerTest {

  private final XmlSerializer serializer = new XmlSerializer();

  @ParameterizedTest
  @ValueSource(
      strings = {
        "test.xml",
        "element.xml",
        "attribute.xml",
        "attribute-missing-end-quote.xml",
        "attribute-missing-quotes.xml",
        "attribute-missing-start-quote.xml",
        "attribute-ns.xml",
        "comment.xml",
        "doctype.xml",
        "processing-instruction.xml",
        "xml-declaration.xml"
      })
  void serialize(String file) throws IOException {
    String act = null;
    try (StringWriter output = new StringWriter()) {
      var input = readResource("/serializer/src/" + file);
      serializer.serialize(input.toCharArray(), output);
      act = output.toString();

      assertEquals(readResource("/serializer/exp/" + file), act);
    } catch (Throwable e) {
      if (act != null) {
        Files.writeString(Paths.get("src/test/resources/serializer/exp", file), act);
      }
      throw e;
    }
  }

  private String readResource(String name) {
    try (InputStream in = getClass().getResourceAsStream(name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
