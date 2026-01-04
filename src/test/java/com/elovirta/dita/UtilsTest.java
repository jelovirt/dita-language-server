package com.elovirta.dita;

import static net.sf.saxon.sapling.Saplings.elem;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class UtilsTest {
  private final Configuration config = new Configuration();
  private final Processor processor = new Processor(config);

  @Test
  void parseRange() {
    var act = Utils.parseRange("1:2-2:3");
    assertEquals(new Range(new Position(0, 1), new Position(1, 3)), act);
  }

  static Stream<Arguments> contains_trueArguments() {
    return Stream.of(
        Arguments.of(new Range(new Position(1, 1), new Position(2, 3)), new Position(1, 1)),
        Arguments.of(new Range(new Position(1, 1), new Position(2, 3)), new Position(1, 2)),
        Arguments.of(new Range(new Position(1, 1), new Position(2, 3)), new Position(1, 3)),
        Arguments.of(new Range(new Position(1, 1), new Position(2, 3)), new Position(1, 4)),
        Arguments.of(new Range(new Position(1, 1), new Position(2, 3)), new Position(2, 0)),
        Arguments.of(new Range(new Position(1, 1), new Position(2, 3)), new Position(2, 1)),
        Arguments.of(new Range(new Position(1, 1), new Position(2, 3)), new Position(2, 2)),
        Arguments.of(new Range(new Position(1, 1), new Position(2, 3)), new Position(2, 3)));
  }

  @ParameterizedTest
  @MethodSource("contains_trueArguments")
  void contains_inside(Range range, Position position) {
    assertTrue(Utils.contains(range, position));
  }

  static Stream<Arguments> contains_falseArguments() {
    return Stream.of(
        Arguments.of(new Range(new Position(1, 1), new Position(2, 3)), new Position(0, 1)),
        Arguments.of(new Range(new Position(1, 1), new Position(2, 3)), new Position(1, 0)),
        Arguments.of(new Range(new Position(1, 1), new Position(2, 3)), new Position(2, 4)),
        Arguments.of(new Range(new Position(1, 1), new Position(2, 3)), new Position(3, 1)));
  }

  @ParameterizedTest
  @MethodSource("contains_falseArguments")
  void contains_outside(Range range, Position position) {
    assertFalse(Utils.contains(range, position));
  }

  @ParameterizedTest
  @ValueSource(strings = {"file:///Users/foo/test.dita", "file:///Users/foo/test.dita#fragment"})
  void stripFragment(URI uri) {
    assertEquals(URI.create("file:///Users/foo/test.dita"), Utils.stripFragment(uri));
  }

  @ParameterizedTest
  @ValueSource(strings = {"file:///Users/foo/test.dita"})
  void getExtension(String uri) {
    assertEquals("dita", Utils.getExtension(uri));
  }

  @ParameterizedTest
  @ValueSource(strings = {"file:///Users/foo/test", "file:///Users/foo/"})
  void getExtension_noExtension(String uri) {
    assertNull(Utils.getExtension(uri));
  }

  @ParameterizedTest
  @ValueSource(strings = {"a", "ab", "abc"})
  void startsWith_success(String src) {
    var prefix = src.toCharArray();

    assertTrue(Utils.startsWith(new char[] {'a', 'b', 'c'}, prefix));
  }

  @ParameterizedTest
  @ValueSource(strings = {"b", "abcd"})
  void startsWith_failure(String src) {
    var prefix = src.toCharArray();

    assertFalse(Utils.startsWith(new char[] {'a', 'b', 'c'}, prefix));
  }

  @ParameterizedTest
  @MethodSource("isLocalDitaArguments")
  void isLocalDita(String format, String scope) throws SaxonApiException {
    var src = elem("foo");
    if (format != null) {
      src = src.withAttr("format", format);
    }
    if (scope != null) {
      src = src.withAttr("scope", scope);
    }
    assertTrue(Utils.isLocalDita().test(src.toXdmNode(processor)));
  }

  @ParameterizedTest
  @MethodSource("isNotLocalDitaArguments")
  void isNotLocalDita(String format, String scope) throws SaxonApiException {
    var src = elem("foo");
    if (format != null) {
      src = src.withAttr("format", format);
    }
    if (scope != null) {
      src = src.withAttr("scope", scope);
    }
    assertFalse(Utils.isLocalDita().test(src.toXdmNode(processor)));
  }

  static Stream<Arguments> isLocalDitaArguments() {
    return permutations(Arrays.asList(null, "dita"), Arrays.asList(null, "local", "peer"));
  }

  static Stream<Arguments> isNotLocalDitaArguments() {
    return Stream.concat(
        permutations(List.of("html"), Arrays.asList(null, "local", "peer")),
        permutations(Arrays.asList(null, "dita"), List.of("external")));
  }

  static Stream<Arguments> permutations(List<String> formats, List<String> scopes) {
    return formats.stream()
        .flatMap(format -> scopes.stream().map(scope -> Arguments.arguments(format, scope)));
  }
}
