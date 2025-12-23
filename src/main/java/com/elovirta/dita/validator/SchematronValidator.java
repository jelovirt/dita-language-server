package com.elovirta.dita.validator;

import static com.elovirta.dita.DitaTextDocumentService.SOURCE;
import static com.elovirta.dita.xml.XmlSerializer.LOC_NAMESPACE;
import static net.sf.saxon.s9api.streams.Steps.*;

import com.elovirta.dita.Utils;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.streams.Step;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchematronValidator {

  private static final QName SCHEMATRON_OUTPUT =
      QName.fromClarkName("{http://purl.oclc.org/dsdl/svrl}schematron-output");
  private static final QName FAILED_ASSERT =
      QName.fromClarkName("{http://purl.oclc.org/dsdl/svrl}failed-assert");
  private static final QName SUCCESSFUL_REPORT =
      QName.fromClarkName("{http://purl.oclc.org/dsdl/svrl}successful-report");
  private static final QName TEXT = QName.fromClarkName("{http://purl.oclc.org/dsdl/svrl}text");

  private static final QName SCHXSLT_PHASE =
      QName.fromClarkName("{http://dmaus.name/ns/2023/schxslt}phase");

  private static final Logger logger = LoggerFactory.getLogger(SchematronValidator.class);

  private final Processor processor;
  private final XsltExecutable schematronCompiler;
  private XsltExecutable schematron;

  public SchematronValidator(Processor processor) {
    this.processor = processor;

    try (var in = getClass().getResourceAsStream("/xslt/transpile.xsl")) {
      this.schematronCompiler =
          processor
              .newXsltCompiler()
              .compile(new StreamSource(in, "classpath:/xslt/transpile.xsl"));
    } catch (SaxonApiException | IOException e) {
      throw new RuntimeException("Failed to parse classpath:/xslt/transpile.xsl", e);
    }
  }

  public void setSchematron(URI srcUri) {
    logger.info("Setting schematron: {}", srcUri);
    try {
      Source src;
      if (srcUri.getScheme().equals("classpath")) {
        src =
            new StreamSource(getClass().getResourceAsStream("/" + srcUri.getSchemeSpecificPart()));
      } else {
        src = new StreamSource(srcUri.toString());
      }
      var dst = new XdmDestination();
      var compiler = schematronCompiler.load30();
      compiler.setStylesheetParameters(Map.of(SCHXSLT_PHASE, XdmValue.makeValue("#ALL")));
      compiler.transform(src, dst);
      schematron = processor.newXsltCompiler().compile(dst.getXdmNode().getUnderlyingNode());
    } catch (SaxonApiException e) {
      logger.error("Failed to compile schematron", e);
    }
  }

  public void validate(XdmNode content, List<Diagnostic> diagnostics) {
    logger.debug("Validating with schematron {}", schematron);
    if (schematron == null) {
      return;
    }
    try {
      var res = new XdmDestination();
      schematron.load30().transform(content.getUnderlyingNode(), res);
      var act = res.getXdmNode();
      act.select(
              child(SCHEMATRON_OUTPUT.getNamespace(), SCHEMATRON_OUTPUT.getLocalName())
                  .then(
                      child(FAILED_ASSERT.getNamespace(), FAILED_ASSERT.getLocalName())
                          .cat(
                              child(
                                  SUCCESSFUL_REPORT.getNamespace(),
                                  SUCCESSFUL_REPORT.getLocalName()))))
          .forEach(
              failedAssert -> {
                failedAssert
                    .select(child(TEXT.getNamespace(), TEXT.getLocalName()))
                    .findAny()
                    .ifPresent(
                        text -> {
                          logger.debug("{}", failedAssert);
                          var context =
                              content
                                  .select(parsePattern(failedAssert.attribute("location")))
                                  .firstItem();
                          // FIXME: Track text node locations with PIs
                          if (context.getNodeKind() == XdmNodeKind.TEXT) {
                            context = context.getParent();
                          }
                          var range =
                              Utils.parseRange(
                                  context.getAttributeValue(
                                      QName.fromClarkName("{" + LOC_NAMESPACE + "}elem")));
                          var severity =
                              switch (failedAssert.attribute("role")) {
                                case "error" -> DiagnosticSeverity.Error;
                                case "warning" -> DiagnosticSeverity.Warning;
                                default -> null;
                              };
                          diagnostics.add(
                              new Diagnostic(
                                  range, text.getStringValue().trim(), severity, SOURCE));
                        });
              });
      logger.info("Schematron validated: {}", act);
    } catch (SaxonApiException e) {
      logger.error("Failed to validate schematron", e);
    }
  }

  Step<XdmNode> parsePattern(String pattern) {
    var steps = PseudoXPathParser.parse(pattern);
    Collections.reverse(steps);
    return steps.stream()
        .map(
            step -> {
              if (step instanceof PseudoXPathParser.AttributeStep attr) {
                return attribute(attr.namespace(), attr.localName());
              } else if (step instanceof PseudoXPathParser.ElementStep elem) {
                return child(elem.namespace(), elem.localName()).at(elem.position() - 1);
              } else if (step instanceof PseudoXPathParser.TextStep text) {
                return text().at(text.position() - 1);
              } else {
                throw new IllegalArgumentException("Unrecognized step: " + step);
              }
            })
        .reduce(null, (acc, curr) -> acc == null ? curr : curr.then(acc));
  }
}
