package com.elovirta.dita.validator;

import static net.sf.saxon.s9api.streams.Steps.*;
import static org.junit.jupiter.api.Assertions.*;

import com.elovirta.dita.DitaParser;
import java.net.URI;
import java.util.ArrayList;
import java.util.stream.Stream;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.streams.Step;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SchematronValidatorTest {

  private final SchematronValidator schematronValidator;
  private final DitaParser parser;
  private XdmNode doc;

  public SchematronValidatorTest() {
    this.parser = new DitaParser();
    this.schematronValidator = new SchematronValidator(parser.getProcessor());
  }

  @BeforeEach
  void setUp() {
    doc =
        parser
            .parse(
                """
            <topic xmlns:ditaarch="http://dita.oasis-open.org/architecture/2005/"
                   ditaarch:DITAArchVersion="1.3"
                   id="topic">
             <title>Title</title>
             <body/>
            </topic>
            """,
                URI.create("file:///topic.dita"))
            .document();
  }

  @Test
  void validate_noSchematron() {
    var act = new ArrayList<Diagnostic>();

    schematronValidator.validate(doc, act);

    assertTrue(act.isEmpty());
  }

  //  @Test
  //  void validate() {
  //    schematronValidator.setSchematron(URI.create("classpath:topic.sch"));
  //    var act = new ArrayList<Diagnostic>();
  //
  //    schematronValidator.validate(document, act);
  //
  //    assertEquals(
  //        List.of(
  //            new Diagnostic(new Range(new Position(3, 4), new Position(3, 8)), "Error
  // message.")),
  //        act);
  //  }

  private static Stream<Arguments> parseLocationArguments() {
    return Stream.of(
        Arguments.of(
            "/Q{}topic[1]/Q{}body[1]", child("", "topic").at(0).then(child("", "body").at(0))),
        Arguments.of("/Q{}topic[1]/@id", child("topic").first().then(attribute("id"))),
        Arguments.of(
            "/Q{}topic[1]/@Q{http://dita.oasis-open.org/architecture/2005/}DITAArchVersion",
            child("topic")
                .first()
                .then(
                    attribute("http://dita.oasis-open.org/architecture/2005/", "DITAArchVersion"))),
        Arguments.of(
            "/Q{}topic[1]/Q{}title[1]/text()[1]",
            child("topic").first().then(child("title").first().then(text().first()))));
  }

  @ParameterizedTest
  @MethodSource("parseLocationArguments")
  void parseLocation(String src, Step<XdmNode> exp) {
    var act = schematronValidator.parsePattern(src);

    assertPatternEquals(exp, act);
  }

  private void assertPatternEquals(Step<XdmNode> exp, Step<XdmNode> act) {
    assertEquals(doc.select(exp).toList(), doc.select(act).toList());
  }
}
