package com.elovirta.dita.validator;

import static org.junit.jupiter.api.Assertions.*;

import com.elovirta.dita.DitaParser;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.sf.saxon.s9api.XdmNode;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        parser.parse(
            """
            <topic id="topic">
             <title>Title</title>
             <body>
               <pre/>
             </body>
            </topic>
            """,
            URI.create("file:///topic.dita"));
  }

  @Test
  void validate_noSchematron() {
    var act = new ArrayList<Diagnostic>();

    schematronValidator.validate(doc, act);

    assertTrue(act.isEmpty());
  }

  @Test
  void validate() {
    schematronValidator.setSchematron(URI.create("classpath:topic.sch"));
    var act = new ArrayList<Diagnostic>();

    schematronValidator.validate(doc, act);

    assertEquals(
        List.of(
            new Diagnostic(new Range(new Position(0, 0), new Position(0, 0)), "Error message.")),
        act);
  }
}
