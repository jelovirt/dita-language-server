package com.elovirta.dita;

import static com.elovirta.dita.Utils.ATTR_ID;
import static com.elovirta.dita.Utils.TOPIC_TOPIC;
import static com.elovirta.dita.xml.XmlSerializer.LOC_ATTR_PREFIX;
import static com.elovirta.dita.xml.XmlSerializer.LOC_NAMESPACE;
import static net.sf.saxon.s9api.streams.Predicates.*;
import static net.sf.saxon.s9api.streams.Steps.attribute;
import static net.sf.saxon.s9api.streams.Steps.descendant;

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
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentManager {

  private static final Logger logger = LoggerFactory.getLogger(DocumentManager.class);

  private final DitaParser ditaParser;
  private final Map<URI, DocumentCache> openDocuments = new ConcurrentHashMap<>();

  public DocumentManager(DitaParser ditaParser) {
    this.ditaParser = ditaParser;
  }

  public record DocumentCache(
      XdmNode document,
      Map<String, List<String>> ids,
      TreeMap<PositionKey, RangeValue<XdmNode>> attributeRanges) {
    public DocumentCache {
      Objects.requireNonNull(document);
      Objects.requireNonNull(ids);
    }

    public XdmNode getNode(Position position) {
      var entry = attributeRanges().floorEntry(new PositionKey(position));
      if (entry != null && Utils.contains(entry.getValue().range(), position)) {
        return entry.getValue().value();
      }
      return null;
    }
  }

  public DocumentCache get(URI uri) {
    var documentCache =
        openDocuments.computeIfAbsent(
            uri,
            u -> {
              try {
                logger.info("Parsing {}", u);
                var doc = ditaParser.parse(Files.readString(Paths.get(u)), uri);
                return new DocumentCache(doc, readIds(doc), readAttributeLocations(doc));
              } catch (IOException e) {
                logger.error("Error parsing {}", u, e);
                return null;
              }
            });
    if (documentCache == null) {
      return null;
    }
    return documentCache;
  }

  public void put(URI uri, XdmNode doc) {
    openDocuments.put(uri, new DocumentCache(doc, readIds(doc), readAttributeLocations(doc)));
  }

  public void remove(URI uri) {
    openDocuments.remove(uri);
  }

  public void forEach(BiConsumer<URI, XdmNode> action) {
    openDocuments.forEach((uri, cache) -> action.accept(uri, cache.document()));
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
              .select(descendant(TOPIC_TOPIC).first().then(attribute(ATTR_ID)))
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
        doc.select(descendant(TOPIC_TOPIC))
            .flatMap(
                topic -> {
                  var topicId = topic.attribute(ATTR_ID);
                  return topic
                      .select(
                          Steps.child(isElement())
                              .where(not(TOPIC_TOPIC))
                              .then(Steps.descendantOrSelf().then(attribute(ATTR_ID))))
                      .map(elementId -> Map.entry(topicId, elementId.getStringValue()));
                })
            .toList();
    return res.stream()
        .collect(
            Collectors.groupingBy(
                Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
  }

  record RangeValue<T>(Range range, T value) {}

  record PositionKey(int line, int character) implements Comparable<PositionKey> {
    public PositionKey(Position position) {
      this(position.getLine(), position.getCharacter());
    }

    @Override
    public int compareTo(@NotNull DocumentManager.PositionKey that) {
      var lineOrder = Integer.compare(line, that.line);
      if (lineOrder != 0) {
        return lineOrder;
      }
      return Integer.compare(character, that.character);
    }
  }

  private TreeMap<PositionKey, RangeValue<XdmNode>> readAttributeLocations(XdmNode doc) {
    final TreeMap<PositionKey, RangeValue<XdmNode>> res = new TreeMap<>();
    doc.select(
            descendant(isElement())
                .then(
                    attribute(
                        attr ->
                            attr.getNodeName().getNamespaceUri().toString().equals(LOC_NAMESPACE)
                                && attr.getNodeName().getLocalName().startsWith(LOC_ATTR_PREFIX))))
        .forEach(
            attr ->
                attr.getParent()
                    .select(
                        attribute(
                            attr.getNodeName().getLocalName().substring(LOC_ATTR_PREFIX.length())))
                    .asOptionalNode()
                    .ifPresent(
                        a -> {
                          var range = Utils.parseRange(attr.getStringValue());
                          res.put(new PositionKey(range.getStart()), new RangeValue<>(range, a));
                        }));
    return res;
  }
}
