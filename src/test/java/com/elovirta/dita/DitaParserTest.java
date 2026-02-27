package com.elovirta.dita;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class DitaParserTest {

  private final DitaParser parser = new DitaParser();

  @RepeatedTest(1)
  void parse() throws URISyntaxException {
    var src = readResource("topics/valid.dita");

    var act = parser.parse(src, getClass().getResource("/topics/valid.dita").toURI());

    //    System.out.println(act.toString());
  }

  @Test
  void parse_invalid() throws URISyntaxException {
    var src = readResource("topics/invalid.dita");

    var act = parser.parse(src, getClass().getResource("/topics/invalid.dita").toURI());

    System.err.println(act.diagnostics().toString());
  }

  @Test
  void mergeMap() throws URISyntaxException {
    var src = readResource("root.ditamap");

    var act = parser.parse(src, getClass().getResource("/root.ditamap").toURI());

    //    System.out.println(act.toString());
  }

  private String readResource(String path) {
    try (var in = getClass().getClassLoader().getResourceAsStream(path)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
