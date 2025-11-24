package com.elovirta.dita;

import static com.elovirta.dita.LocationEnrichingXNIHandler.LOC_NAMESPACE;
import static com.elovirta.dita.Utils.*;
import static net.sf.saxon.s9api.streams.Steps.*;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import org.eclipse.lsp4j.Location;

public class KeyManager {
  private static final String KEYS_ATTR = "keys";
  private static final String HREF_ATTR = "href";

  private volatile Map<String, KeyDefinition> keyDefinitions = Collections.emptyMap();

  public void read(URI uri, XdmNode map) {
    System.err.println("Read key definitions " + uri);
    var keyDefs = map.select(descendant().then(attribute(KEYS_ATTR))).toList();
    if (!keyDefs.isEmpty()) {
      Map<String, KeyDefinition> buf = new ConcurrentHashMap<>();
      for (XdmNode keyDefAttr : keyDefs) {
        var keys = Set.of(keyDefAttr.getStringValue().trim().split("\\s+"));
        var keyDef = keyDefAttr.getParent();
        for (String key : keys) {
          if (!buf.containsKey(key)) {
            var target =
                keyDef.attribute(HREF_ATTR) != null
                    ? uri.resolve(keyDef.attribute(HREF_ATTR))
                    : null;
            var text =
                keyDef
                    .select(
                        child(MAP_TOPICMETA)
                            .then(child(TOPIC_KEYWORDS).then(child(TOPIC_KEYWORD).first())))
                    .asOptionalString()
                    .orElse(null);
            var navtitle =
                keyDef
                    .select(child(MAP_TOPICMETA).then(child(TOPIC_NAVTITLE).first()))
                    .asOptionalString()
                    .orElse(null);
            var keyDefinition = new KeyDefinition(uri, key, keyDef, target, text, navtitle);
            buf.put(key, keyDefinition);
          }
        }
      }
      keyDefinitions = buf;
      //      System.err.println("Keys: " + keyDefinitions.keySet());
    }
  }

  public KeyDefinition get(String key) {
    return keyDefinitions.get(key);
  }

  public boolean containsKey(String key) {
    return keyDefinitions.containsKey(key);
  }

  public Set<Map.Entry<String, KeyDefinition>> keys() {
    return keyDefinitions.entrySet();
  }

  public record KeyDefinition(
      URI mapUri, String key, XdmNode definition, URI target, String text, String navtitle) {
    public Location location() {
      return new Location(
          mapUri().toString(),
          Utils.parseRange(this.definition().getAttributeValue(new QName(LOC_NAMESPACE, "elem"))));
    }
  }
}
