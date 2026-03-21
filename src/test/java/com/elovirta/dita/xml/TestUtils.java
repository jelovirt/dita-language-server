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
import java.util.List;

public abstract class TestUtils {

  final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  final String path;

  TestUtils(String path) {
    this.path = path;
  }

  <T> void assertJsonEquals(String file, String ext, Class<T> cls, List<T> act) {
    try {
      var exp =
          gson.fromJson(
              readResource("/" + path + "/" + file + ext),
              TypeToken.getParameterized(List.class, cls).getType());

      assertEquals(exp, act);
    } catch (Throwable e) {
      try (var out = Files.newBufferedWriter(Paths.get("src/test/resources/" + path, file + ext))) {
        gson.toJson(act, out);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      throw e;
    }
  }

  record Event(XmlLexer.TokenType type, String text, int line, int column, int offset) {}

  String readResource(String name) {
    try (InputStream in = getClass().getResourceAsStream(name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
