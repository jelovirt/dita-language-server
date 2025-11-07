package com.elovirta.dita.xml;

import org.junit.jupiter.api.Test;

public class XmlLexerTest {

  private final XmlLexer lexer = new XmlLexer();

  @Test
  void tokenize() {
    String input =
        """
                <?xml version="1.0"?>
                <!DOCTYPE topic PUBLIC "-//OASIS//DTD DITA Topic//EN" "topic.dtd">
                <root attr="value">
                    <child>text content</child>
                    <!-- comment -->
                </root>
                """;
    XmlLexer lexer = new XmlLexer();
    lexer.setInput(input);

    while (lexer.hasNext()) {
      XmlLexer.TokenType type = lexer.next();
      //            if (type != XmlLexer.TokenType.WHITESPACE) {
      System.out.printf(
          "%s[%d:%d] '%s'%n",
          type, lexer.getLine(), lexer.getColumn(), new String(lexer.getText()));
      //            }
    }
  }
}
