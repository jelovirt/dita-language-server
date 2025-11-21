package com.elovirta.dita;

import static net.sf.saxon.s9api.streams.Predicates.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.streams.Steps;
import org.jetbrains.annotations.Nullable;

public class DocumentManager {

  private final DitaParser ditaParser = new DitaParser();
  private final Map<URI, XdmNode> openDocuments = new ConcurrentHashMap<>();
  private final Map<URI, Map<String, List<String>>> ids = new ConcurrentHashMap<>();

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
    ids.put(uri, readIds(node));
  }

  public void remove(URI uri) {
    openDocuments.remove(uri);
    ids.remove(uri);
  }

  public void forEach(BiConsumer<URI, XdmNode> action) {
    openDocuments.forEach(action);
  }

  public Collection<String> listIds(URI uri) {
    return ids.getOrDefault(uri, Collections.emptyMap()).keySet();
  }

  /** Return element IDs for a topic ID. */
  public Collection<String> listElementIds(URI uri, @Nullable String topicId) {
    var id = topicId;
    // FIXME: cache root ID
    if (id == null) {
      var doc = get(uri);
      if (doc == null) {
        return Collections.emptyList();
      }
      id =
          doc.select(Steps.descendant("topic").first().then(Steps.attribute("id")))
              .map(XdmNode::getStringValue)
              .findFirst()
              .orElse(null);
      if (id == null) {
        return Collections.emptyList();
      }
    }
    return ids.getOrDefault(uri, Collections.emptyMap()).getOrDefault(id, Collections.emptyList());
  }

  public boolean exists(URI uri, String topicId, String elementId) {
    return ids.getOrDefault(uri, Collections.emptyMap())
        .getOrDefault(topicId, Collections.emptyList())
        .contains(elementId);
  }

  public boolean exists(URI uri, String topicId) {
    return ids.getOrDefault(uri, Collections.emptyMap()).containsKey(topicId);
  }

  public boolean exists(URI uri) {
    return openDocuments.containsKey(uri) || Files.exists(Paths.get(uri));
  }

  private Map<String, List<String>> readIds(XdmNode doc) {
    var res =
        doc.select(Steps.descendant("topic"))
            .flatMap(
                topic -> {
                  var topicId = topic.attribute("id");
                  return topic
                      .select(
                          Steps.child(isElement())
                              .where(not(hasLocalName("topic")))
                              .then(Steps.descendantOrSelf().then(Steps.attribute("id"))))
                      .map(elementId -> Map.entry(topicId, elementId.getStringValue()));
                })
            .toList();
    return res.stream()
        .collect(
            Collectors.groupingBy(
                Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
  }
}
