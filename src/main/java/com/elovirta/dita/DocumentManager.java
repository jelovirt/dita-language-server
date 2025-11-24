package com.elovirta.dita;

import static com.elovirta.dita.Utils.TOPIC_TOPIC;
import static net.sf.saxon.s9api.streams.Predicates.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.streams.Steps;
import org.jetbrains.annotations.Nullable;

public class DocumentManager {

  private final DitaParser ditaParser = new DitaParser();
  private final Map<URI, DocumentCache> openDocuments = new ConcurrentHashMap<>();

  //  private final Map<URI, XdmNode> openDocuments = new ConcurrentHashMap<>();
  //  private final Map<URI, Map<String, List<String>>> ids = new ConcurrentHashMap<>();

  public record DocumentCache(XdmNode document, Map<String, List<String>> ids) {
    public DocumentCache {
      Objects.requireNonNull(document);
      Objects.requireNonNull(ids);
    }
  }

  public DocumentCache get(URI uri) {
    var documentCache =
        openDocuments.computeIfAbsent(
            uri,
            u -> {
              try {
                System.err.println("Parsing " + u);
                var doc = ditaParser.parse(Files.readString(Paths.get(u)));
                return new DocumentCache(doc, readIds(doc));
              } catch (IOException e) {
                e.printStackTrace();
                return null;
              }
            });
    if (documentCache == null) {
      return null;
    }
    return documentCache;
  }

  public void put(URI uri, XdmNode node) {
    openDocuments.put(uri, new DocumentCache(node, readIds(node)));
    //    ids.put(uri, readIds(node));
  }

  public void remove(URI uri) {
    openDocuments.remove(uri);
    //    ids.remove(uri);
  }

  public void forEach(BiConsumer<URI, XdmNode> action) {
    openDocuments.forEach(
        (uri, cache) -> {
          action.accept(uri, cache.document());
        });
  }

  public Collection<String> listIds(URI uri) {
    var cache = get(uri);
    return cache != null ? cache.ids().keySet() : Collections.emptyList();
  }

  private Map<String, List<String>> getIds(URI uri) {
    var cache = get(uri);
    return cache != null ? cache.ids() : Collections.emptyMap();
  }

  /** Return element IDs for a topic ID. */
  public Collection<String> listElementIds(URI uri, @Nullable String topicId) {
    var id = topicId;
    var cache = get(uri);
    if (cache == null) {
      return Collections.emptyList();
    }
    // FIXME: cache root ID
    if (id == null) {
      id =
          cache
              .document()
              .select(Steps.descendant(TOPIC_TOPIC).first().then(Steps.attribute("id")))
              .map(XdmNode::getStringValue)
              .findFirst()
              .orElse(null);
      if (id == null) {
        return Collections.emptyList();
      }
    }
    return cache.ids().getOrDefault(id, Collections.emptyList());
  }

  public boolean exists(URI uri, String topicId, String elementId) {
    var cache = get(uri);
    if (cache == null) {
      return false;
    }
    return cache.ids().getOrDefault(topicId, Collections.emptyList()).contains(elementId);
  }

  public boolean exists(URI uri, String topicId) {
    var cache = get(uri);
    if (cache == null) {
      return false;
    }
    return cache.ids().containsKey(topicId);
  }

  public boolean exists(URI uri) {
    return openDocuments.containsKey(uri) || Files.exists(Paths.get(uri));
  }

  private Map<String, List<String>> readIds(XdmNode doc) {
    var res =
        doc.select(Steps.descendant(TOPIC_TOPIC))
            .flatMap(
                topic -> {
                  var topicId = topic.attribute("id");
                  return topic
                      .select(
                          Steps.child(isElement())
                              .where(not(TOPIC_TOPIC))
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
