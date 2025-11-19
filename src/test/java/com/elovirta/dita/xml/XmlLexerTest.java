package com.elovirta.dita.xml;

import static com.elovirta.dita.xml.XmlLexer.TokenType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class XmlLexerTest {

  private final XmlLexer lexer = new XmlLexer();

  @Test
  void tokenize() {
    lexer.setInput(readResource("/serializer/src/test.xml"));

    var act = new ArrayList<XmlLexer.TokenType>();
    lexer.forEachRemaining(act::add);

    assertEquals(
        List.of(
            XML_DECL_START,
            WHITESPACE,
            NAME,
            EQUALS,
            ATTR_QUOTE,
            ATTR_VALUE,
            ATTR_QUOTE,
            XML_DECL_END,
            WHITESPACE,
            DOCTYPE_START,
            WHITESPACE,
            NAME,
            WHITESPACE,
            NAME,
            WHITESPACE,
            ATTR_QUOTE,
            ATTR_VALUE,
            ATTR_QUOTE,
            WHITESPACE,
            ATTR_QUOTE,
            ATTR_VALUE,
            ATTR_QUOTE,
            DOCTYPE_END,
            WHITESPACE,
            ELEMENT_START,
            NAME,
            WHITESPACE,
            NAME,
            EQUALS,
            ATTR_QUOTE,
            ATTR_VALUE,
            ATTR_QUOTE,
            ELEMENT_END,
            WHITESPACE,
            ELEMENT_START,
            NAME,
            WHITESPACE,
            NAME,
            EQUALS,
            ATTR_QUOTE,
            ATTR_VALUE,
            ATTR_QUOTE,
            WHITESPACE,
            ELEMENT_END,
            NAME,
            WHITESPACE,
            NAME,
            ELEMENT_CLOSE,
            NAME,
            ELEMENT_END,
            WHITESPACE,
            COMMENT_START,
            COMMENT_BODY,
            COMMENT_END,
            WHITESPACE,
            ELEMENT_CLOSE,
            NAME,
            ELEMENT_END,
            WHITESPACE,
            EOF),
        act);
  }

  private String readResource(String name) {
    try (InputStream in = getClass().getResourceAsStream(name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
