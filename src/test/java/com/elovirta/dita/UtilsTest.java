package com.elovirta.dita;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.stream.Stream;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class UtilsTest {

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
}
