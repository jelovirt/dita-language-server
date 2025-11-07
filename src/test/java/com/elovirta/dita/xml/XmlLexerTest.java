package com.elovirta.dita.xml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class XmlLexerTest {

  private final XmlLexer lexer = new XmlLexer();

  @Test
  void tokenize() {
    lexer.setInput(readResource("/serializer/src/test.xml"));
    while (lexer.hasNext()) {
      XmlLexer.TokenType type = lexer.next();
      System.out.printf(
          "%s[%d:%d] '%s'%n",
          type, lexer.getLine(), lexer.getColumn(), new String(lexer.getText()));
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
