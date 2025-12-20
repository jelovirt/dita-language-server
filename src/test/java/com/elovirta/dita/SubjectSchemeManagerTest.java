package com.elovirta.dita;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import net.sf.saxon.s9api.QName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubjectSchemeManagerTest {

  private SubjectSchemeManager subjectSchemeManager;
  private DitaParser ditaParser = new DitaParser();

  @BeforeEach
  void setUp() {
    var uri = URI.create("classpath:maps/subjectScheme.ditamap");
    var doc = ditaParser.parse(readResource("/maps/subjectScheme.ditamap"), uri);
    subjectSchemeManager = new SubjectSchemeManager();
    subjectSchemeManager.read(uri, doc);
  }

  @Test
  void get() {
    var act = subjectSchemeManager.values(QName.fromClarkName("users"), "p");
    assertNotNull(act);
  }

  //    @Test
  //    void containsKey() {
  //    }
  //
  //    @Test
  //    void keys() {
  //    }

  private String readResource(String name) {
    try (InputStream in = getClass().getResourceAsStream(name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
