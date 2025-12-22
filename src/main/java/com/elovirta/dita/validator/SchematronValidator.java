package com.elovirta.dita.validator;

import static com.elovirta.dita.xml.XmlSerializer.LOC_NAMESPACE;
import static net.sf.saxon.s9api.streams.Steps.attribute;
import static net.sf.saxon.s9api.streams.Steps.child;

import com.elovirta.dita.Utils;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.streams.Step;
import org.eclipse.lsp4j.Diagnostic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchematronValidator {

  private static final QName SCHEMATRON_OUTPUT =
      QName.fromClarkName("{http://purl.oclc.org/dsdl/svrl}schematron-output");
  private static final QName FAILED_ASSERT =
      QName.fromClarkName("{http://purl.oclc.org/dsdl/svrl}failed-assert");
  private static final QName TEXT = QName.fromClarkName("{http://purl.oclc.org/dsdl/svrl}text");

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
      schematronCompiler.load30().transform(src, dst);
      schematron = processor.newXsltCompiler().compile(dst.getXdmNode().getUnderlyingNode());
    } catch (SaxonApiException e) {
      logger.error("Failed to compile schematron", e);
    }
  }

  public void validate(XdmNode content, List<Diagnostic> diagnostics) {
    if (schematron == null) {
      return;
    }
    try {
      var res = new XdmDestination();
      schematron.load30().transform(content.getUnderlyingNode(), res);
      var act = res.getXdmNode();
      act.select(
              child(SCHEMATRON_OUTPUT.getNamespace(), SCHEMATRON_OUTPUT.getLocalName())
                  .then(child(FAILED_ASSERT.getNamespace(), FAILED_ASSERT.getLocalName())))
          .forEach(
              failedAssert -> {
                failedAssert
                    .select(child(TEXT.getNamespace(), TEXT.getLocalName()))
                    .findAny()
                    .ifPresent(
                        text -> {
                          logger.info("{}", failedAssert);
                          var context =
                              content
                                  .select(parsePattern(failedAssert.attribute("location")))
                                  .firstItem();
                          var range =
                              Utils.parseRange(
                                  context.getAttributeValue(
                                      QName.fromClarkName("{" + LOC_NAMESPACE + "}elem")));
                          diagnostics.add(new Diagnostic(range, text.getStringValue()));
                        });
              });
      logger.info("Schematron validated: {}", act);
    } catch (SaxonApiException e) {
      logger.error("Failed to validate schematron", e);
    }
  }

  Step<XdmNode> parsePattern(String pattern) {
    List<String> split = new ArrayList<>(Arrays.asList(pattern.split("/")));
    split.remove(0);
    Collections.reverse(split);
    Step<XdmNode> res = null;
    String resString = null;
    for (var step : split) {
      Step<XdmNode> current;
      String currentString;
      if (step.startsWith("@Q{")) {
        var nsIndex = step.indexOf('}');
        currentString = "attribute(%s, %s)".formatted(step.substring(0, nsIndex), step.substring(nsIndex + 1));
        current = attribute(step.substring(0, nsIndex), step.substring(nsIndex + 1));
      } else if (step.startsWith("@")) {
        currentString = "attribute(%s)".formatted(step.substring(1));
        current = attribute(step.substring(1));
      } else {
        var nsIndex = step.indexOf('}');
        var predicateIndex = step.indexOf('[');
        currentString = "child(%s, %s).at(%s)".formatted(
                step.substring(2, nsIndex),
                step.substring(nsIndex + 1, predicateIndex),
                Integer.parseInt(step.substring(predicateIndex + 1, step.length() - 1)) - 1);
        current = child(step.substring(2, nsIndex), step.substring(nsIndex + 1, predicateIndex)).at(Integer.parseInt(step.substring(predicateIndex + 1, step.length() - 1)) - 1);
      }
      if (res == null) {
        res = current;
        resString = currentString;
      } else {
        res = current.then(res);
        currentString = currentString + ".then(" + resString + ")";
        resString = currentString;
      }
    }
    logger.info("Parsed pattern: {}", resString);
    return res;
  }
}
