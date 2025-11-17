package com.elovirta.dita;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.sf.saxon.s9api.XdmNode;

public class DocumentManager {

  private final Map<String, XdmNode> openDocuments = new ConcurrentHashMap<>();

  public XdmNode get(String uri) {
    return openDocuments.get(uri);
  }

  public void put(String uri, XdmNode node) {
    openDocuments.put(uri, node);
  }

  public void remove(String uri) {
    openDocuments.remove(uri);
  }

  public void forEach(BiConsumer<String, XdmNode> action) {
    openDocuments.forEach(action);
  }
}
