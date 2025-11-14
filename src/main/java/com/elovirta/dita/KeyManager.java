package com.elovirta.dita;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.streams.Steps;

public class KeyManager {
  private Map<String, XdmNode> keyDefinitions = new ConcurrentHashMap<>();

  public void read(XdmNode map) {
    var keyDefs = map.select(Steps.descendant().then(Steps.attribute("keys"))).toList();
    if (!keyDefs.isEmpty()) {
      for (XdmNode keyDef : keyDefs) {
        var keys = Set.of(keyDef.getStringValue().trim().split("\\s+"));
        for (String key : keys) {
          if (!keyDefinitions.containsKey(key)) {
            keyDefinitions.put(key, keyDef);
          }
        }
      }
    }
  }

  public XdmNode get(String key) {
    return keyDefinitions.get(key);
  }

  public boolean containsKey(String key) {
    return keyDefinitions.containsKey(key);
  }
}
