package com.elovirta.dita;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.streams.Steps;

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

  public List<String> listIds(String uri) {
    System.err.println("listIds " + uri);
    return openDocuments
        .get(uri)
        .select(Steps.descendant("topic").then(Steps.attribute("id")))
        .map(XdmNode::getStringValue)
        .toList();
  }
}
