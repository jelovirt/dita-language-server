package com.elovirta.dita.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class XmlLexerTest {

  private final XmlLexer lexer = new XmlLexer();
  Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Test
  void tokenize() {
    lexer.setInput(readResource("/serializer/src/test.xml"));

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

    var exp =
        gson.fromJson(
            readResource("/serializer/exp/test.xml.json"),
            new TypeToken<List<Event>>() {}.getType());

    assertEquals(exp, act);
  }

  private record Event(XmlLexer.TokenType type, String text, int line, int column, int offset) {}

  private String readResource(String name) {
    try (InputStream in = getClass().getResourceAsStream(name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
