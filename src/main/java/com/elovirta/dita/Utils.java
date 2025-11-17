package com.elovirta.dita;

import static com.elovirta.dita.LocationEnrichingXNIHandler.LOC_NAMESPACE;
import static com.elovirta.dita.LocationEnrichingXNIHandler.LOC_PREFIX;

import java.net.URI;
import java.net.URISyntaxException;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class Utils {

  private Utils() {}

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
