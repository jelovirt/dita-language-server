package com.elovirta.dita;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import javax.xml.transform.sax.SAXSource;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import org.apache.xerces.parsers.SAXParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.InputSource;
import org.xmlresolver.Resolver;
import org.xmlresolver.ResolverFeature;
import org.xmlresolver.XMLResolverConfiguration;

class DocumentManagerTest {

  private final DocumentManager documentManager;

  public DocumentManagerTest() {
    try (var in = getClass().getClassLoader().getResourceAsStream("topics/valid.dita")) {
      documentManager = new DocumentManager(new DitaParser());
      var inputSource = new InputSource(in);
      inputSource.setSystemId("file:///topics/valid.dita");
      var reader = new SAXParser();
      XMLResolverConfiguration config = new XMLResolverConfiguration();
      config.setFeature(ResolverFeature.PREFER_PUBLIC, true);
      config.setFeature(ResolverFeature.CATALOG_FILES, List.of("classpath:/schemas/catalog.xml"));
      reader.setEntityResolver(new Resolver(config));
      var doc = new Processor().newDocumentBuilder().build(new SAXSource(reader, inputSource));
      documentManager.put(URI.create("file:///topics/valid.dita"), doc, null);
    } catch (SaxonApiException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void get() {
    assertNotNull(documentManager.get(URI.create("file:///topics/valid.dita")));
  }

  @Test
  void get_missing() {
    assertNull(documentManager.get(URI.create("file:///missing.dita")));
  }

  @Test
  void fileExists() {
    assertTrue(documentManager.exists(URI.create("file:///topics/valid.dita")));
  }

  @Test
  void fileExists_missing() {
    assertFalse(documentManager.exists(URI.create("file:///missing.dita")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"test", "nested"})
  void topicExists(String topicId) {
    assertTrue(documentManager.exists(URI.create("file:///topics/valid.dita"), topicId));
  }

  @ParameterizedTest
  @CsvSource({"file:///valid.dita,missing", "file:///missing.dita,missing"})
  void topicExists_missing(URI uri, String topicId) {
    assertFalse(documentManager.exists(uri, topicId));
  }

  @ParameterizedTest
  @CsvSource({"test,para", "nested,nested-para"})
  void elementExists(String topicId, String elementId) {
    assertTrue(documentManager.exists(URI.create("file:///topics/valid.dita"), topicId, elementId));
  }

  @ParameterizedTest
  @CsvSource({"test,missing", "missing,para", "nested,missing", "missing,nested-para"})
  void elementExists_missing(String topicId, String elementId) {
    assertFalse(
        documentManager.exists(URI.create("file:///topics/valid.dita"), topicId, elementId));
  }

  @Test
  void listElementIds() {
    var act = documentManager.listElementIds(URI.create("file:///topics/valid.dita"), "test");
    assertEquals(List.of("para", "pre"), act);
  }

  @Test
  void listElementIds_missing() {
    var act = documentManager.listElementIds(URI.create("file:///topics/valid.dita"), "missing");
    assertEquals(List.of(), act);
  }
}
