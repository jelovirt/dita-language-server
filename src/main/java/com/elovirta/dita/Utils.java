package com.elovirta.dita;

import static com.elovirta.dita.xml.XmlSerializer.LOC_NAMESPACE;
import static com.elovirta.dita.xml.XmlSerializer.LOC_PREFIX;
import static javax.xml.XMLConstants.NULL_NS_URI;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Predicate;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.type.Type;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class Utils {

  private Utils() {}

  private static Predicate<? super XdmNode> cls(String cls) {
    var name = cls.split("/")[1].trim();
    return item -> {
      var node = item.getUnderlyingNode();
      if (node.getNodeKind() != Type.ELEMENT) {
        return false;
      }
      var classValue = node.getAttributeValue(NULL_NS_URI, "class");
      if (classValue == null) {
        return node.getLocalPart().equals(name);
      }
      return classValue.contains(cls);
    };
  }

  public static final Predicate<? super XdmNode> TOPIC_TOPIC = cls(" topic/topic ");
  public static final Predicate<? super XdmNode> MAP_TOPICMETA = cls(" map/topicmeta ");
  public static final Predicate<? super XdmNode> TOPIC_KEYWORDS = cls(" topic/keywords ");
  public static final Predicate<? super XdmNode> TOPIC_KEYWORD = cls(" topic/keyword ");
  public static final Predicate<? super XdmNode> TOPIC_NAVTITLE = cls(" topic/navtitle ");
  public static final Predicate<? super XdmNode> TOPIC_IMAGE = cls(" topic/image ");
  public static final Predicate<? super XdmNode> TOPIC_XREF = cls(" topic/xref ");
  public static final Predicate<? super XdmNode> TOPIC_LINK = cls(" topic/link ");

  public static Range getAttributeRange(XdmNode attr) {
    var loc =
        attr.getParent()
            .getAttributeValue(
                new QName(LOC_PREFIX, LOC_NAMESPACE, "attr-" + attr.getNodeName().getLocalName()));
    return parseRange(loc);
  }

  public static Range parseRange(String loc) {
    var tokens = loc.split("[:\\-]");

    return new Range(
        new Position(Integer.parseInt(tokens[0]) - 1, Integer.parseInt(tokens[1]) - 1),
        new Position(Integer.parseInt(tokens[2]) - 1, Integer.parseInt(tokens[3])));
  }

  /** Test if position is included in range. */
  public static boolean contains(Range range, Position pos) {
    var start = range.getStart();
    var end = range.getEnd();

    return compare(pos, start) >= 0 && compare(pos, end) <= 0;
  }

  private static int compare(Position a, Position b) {
    if (a.getLine() < b.getLine()) {
      return -1;
    }
    if (a.getLine() > b.getLine()) {
      return 1;
    }
    return Integer.compare(a.getCharacter(), b.getCharacter());
  }

  public static URI stripFragment(URI uri) {
    if (uri.getFragment() == null) {
      return uri;
    }
    try {
      return new URI(
          uri.getScheme(),
          uri.getUserInfo(),
          uri.getHost(),
          uri.getPort(),
          uri.getPath(),
          uri.getQuery(),
          null);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
