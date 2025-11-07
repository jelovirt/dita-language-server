package com.elovirta.dita.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class XmlSerializerTest {

  private XmlSerializer serializer = new XmlSerializer();

  @Test
  void serialize() {
    String input = readResource("/serializer/src/test.xml");
    try (StringWriter output = new StringWriter()) {
      serializer.serialize(input, output);
      String act = output.toString();

      assertEquals(readResource("/serializer/exp/test.xml"), act);
    } catch (IOException e) {
      throw new RuntimeException(e);
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
