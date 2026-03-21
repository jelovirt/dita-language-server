package com.elovirta.dita.xml;

import java.util.ArrayList;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class XmlLexerTest extends TestUtils {

  private final XmlLexerImpl lexer = new XmlLexerImpl(true);

  public XmlLexerTest() {
    super("serializer/exp/lexer");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "test.xml",
        "element.xml",
        "element-invalid.xml",
        "attribute.xml",
        "attribute-missing-end-quote.xml",
        "attribute-missing-end-quote-empty-element.xml",
        "attribute-missing-end-quote-missing-gt.xml",
        "attribute-missing-quotes.xml",
        "attribute-missing-quotes-empty-element.xml",
        "attribute-missing-quotes-empty-element-with-space.xml",
        "attribute-missing-quotes-missing-gt.xml",
        "attribute-missing-start-quote.xml",
        "attribute-missing-equals.xml",
        "attribute-ns.xml",
        "comment.xml",
        "doctype.xml",
        "processing-instruction.xml",
        "xml-declaration.xml"
      })
  void tokenize(String file) {
    lexer.setInput(readResource("/serializer/src/" + file).toCharArray());

    var act = new ArrayList<Event>();
    lexer.forEachRemaining(
        e ->
            act.add(
                new Event(
                    e,
                    new String(lexer.getText()),
                    lexer.getLine(),
                    lexer.getColumn(),
                    lexer.getOffset())));
    assertJsonEquals(file, ".json", Event.class, act);
  }
}
