package com.elovirta.dita.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class XmlLexerTest {

  private final XmlLexerImpl lexer = new XmlLexerImpl(true);
  final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  @ParameterizedTest
  @ValueSource(
      strings = {
        "test.xml",
        "element.xml",
        "element-invalid.xml",
        "attribute.xml",
        "attribute-missing-end-quote.xml",
        "attribute-missing-quotes.xml",
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
    try {
      var exp =
          gson.fromJson(
              readResource("/serializer/exp/lexer/" + file + ".json"),
              new TypeToken<List<Event>>() {}.getType());

      assertEquals(exp, act);
    } catch (Throwable e) {
      try (var out =
          Files.newBufferedWriter(
              Paths.get("src/test/resources/serializer/exp/lexer", file + ".json"))) {
        gson.toJson(act, out);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      throw e;
    }
  }

  private record Event(
      XmlLexerImpl.TokenType type, String text, int line, int column, int offset) {}

  private String readResource(String name) {
    try (InputStream in = getClass().getResourceAsStream(name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
