package com.elovirta.dita;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class DitaParserTest {

  @Test
  void parse() {
    var parser = new DitaParser();
    var src = readResource("valid.dita");

    var act = parser.parse(src);

    System.out.println(act.toString());
  }

  private String readResource(String path) {
    try (var in = getClass().getClassLoader().getResourceAsStream(path)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
