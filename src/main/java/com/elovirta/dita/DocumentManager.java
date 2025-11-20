package com.elovirta.dita;

import static net.sf.saxon.s9api.streams.Predicates.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.streams.Steps;
import org.jetbrains.annotations.Nullable;

public class DocumentManager {

  private final DitaParser ditaParser = new DitaParser();
  private final Map<URI, XdmNode> openDocuments = new ConcurrentHashMap<>();

  public XdmNode get(URI uri) {
    return openDocuments.computeIfAbsent(
        uri,
        u -> {
          try {
            System.err.println("Parsing " + u);
            return ditaParser.parse(Files.readString(Paths.get(u)));
          } catch (IOException e) {
            return null;
          }
        });
  }

  public void put(URI uri, XdmNode node) {
    openDocuments.put(uri, node);
  }

  public void remove(URI uri) {
    openDocuments.remove(uri);
  }

  public void forEach(BiConsumer<URI, XdmNode> action) {
    openDocuments.forEach(action);
  }

  public List<String> listIds(URI uri) {
    var doc = get(uri);
    if (doc == null) {
      return Collections.emptyList();
    }
    return doc.select(Steps.descendant("topic").then(Steps.attribute("id")))
        .map(XdmNode::getStringValue)
        .toList();
  }

  /** Return element IDs for a topic ID. */
  public List<String> listElementIds(URI uri, @Nullable String topicId) {
    var doc = get(uri);
    if (doc == null) {
      return Collections.emptyList();
    }
    var topicSelector =
        topicId != null
            ? Steps.descendant("topic").where(attributeEq("id", topicId)).first()
            : Steps.descendant("topic").first();
    var idSelector =
        topicSelector
            .then(Steps.child(isElement()).where(not(hasLocalName("topic"))))
            .then(Steps.descendantOrSelf().then(Steps.attribute("id")));
    return doc.select(idSelector).map(XdmNode::getStringValue).toList();
  }

  public boolean exists(URI uri, String topicId, String elementId) {
    var doc = get(uri);
    if (doc == null) {
      return false;
    }
    var topicSelector = Steps.descendant("topic").where(attributeEq("id", topicId)).first();
    var idSelector =
        topicSelector
            .then(Steps.child(isElement()).where(not(hasLocalName("topic"))))
            .then(Steps.descendantOrSelf(isElement()).where(attributeEq("id", elementId)));
    return doc.select(idSelector).exists();
  }

  public boolean exists(URI uri, String topicId) {
    var doc = get(uri);
    if (doc == null) {
      return false;
    }
    var topicSelector = Steps.descendant("topic").where(attributeEq("id", topicId)).first();
    return doc.select(topicSelector).exists();
  }

  public boolean exists(URI uri) {
    return openDocuments.containsKey(uri) || Files.exists(Paths.get(uri));
  }
}
