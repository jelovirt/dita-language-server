package com.elovirta.dita;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.streams.Steps;

public class KeyManager {
  private static final String KEYS_ATTR = "keys";

  private volatile Map<String, KeyDefinition> keyDefinitions = Collections.emptyMap();

  public void read(URI uri, XdmNode map) {
    System.err.println("Read key definitions " + uri);
    var keyDefs = map.select(Steps.descendant().then(Steps.attribute(KEYS_ATTR))).toList();
    if (!keyDefs.isEmpty()) {
      Map<String, KeyDefinition> buf = new ConcurrentHashMap<>();
      for (XdmNode keyDefAttr : keyDefs) {
        var keys = Set.of(keyDefAttr.getStringValue().trim().split("\\s+"));
        var keyDef = keyDefAttr.getParent();
        for (String key : keys) {
          if (!buf.containsKey(key)) {
            var target =
                keyDef.attribute("href") != null ? uri.resolve(keyDef.attribute("href")) : null;
            var keyDefinition = new KeyDefinition(key, keyDef, target);
            buf.put(key, keyDefinition);
          }
        }
      }
      keyDefinitions = buf;
      System.err.println("Keys: " + keyDefinitions.keySet());
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

  public record KeyDefinition(String key, XdmNode definition, URI target) {}
}
