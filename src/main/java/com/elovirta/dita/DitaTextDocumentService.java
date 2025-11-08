package com.elovirta.dita;

import static com.elovirta.dita.LocationEnrichingXNIHandler.LOC_NAMESPACE;
import static com.elovirta.dita.LocationEnrichingXNIHandler.LOC_PREFIX;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.streams.Steps;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DitaTextDocumentService implements TextDocumentService {

  private static final Logger logger = LoggerFactory.getLogger(DitaTextDocumentService.class);

  private final DitaLanguageServer server;
  private final Map<String, XdmNode> openDocuments = new ConcurrentHashMap<>();
  private XdmNode rootMap;
  private final DitaParser parser;

  public DitaTextDocumentService(DitaLanguageServer server) {
    this.server = server;
    this.parser = new DitaParser();
    //        var resolver = this.parser.getCatalogResolver();
    //        try {
    //            var res = resolver.resolveEntity("-//OASIS//DTD DITA 1.3 Base Map//EN", null);
    //            System.err.println(res);
    //        } catch (SAXException | IOException e) {
    //            throw new RuntimeException(e);
    //        }
  }

  @Override
  public CompletableFuture<DocumentDiagnosticReport> diagnostic(DocumentDiagnosticParams params) {
    String uri = params.getTextDocument().getUri();

    // Get the content from storage
    XdmNode content = openDocuments.get(uri);
    if (content == null) {
      // Document not opened yet, return empty diagnostics
      return CompletableFuture.completedFuture(
          new DocumentDiagnosticReport(
              new RelatedFullDocumentDiagnosticReport(Collections.emptyList())));
    }

    // This should do all slow validations
    List<Diagnostic> diagnostics = doSlowValidation(content);

    FullDocumentDiagnosticReport fullReport = new FullDocumentDiagnosticReport(diagnostics);
    RelatedFullDocumentDiagnosticReport report =
        new RelatedFullDocumentDiagnosticReport(fullReport.getItems());

    return CompletableFuture.completedFuture(new DocumentDiagnosticReport(report));
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    String uri = params.getTextDocument().getUri();
    String text = params.getTextDocument().getText();

    System.err.println("Document opened: " + uri);
    try {
      //        openDocuments.put(uri, text);
      XdmNode doc = parser.parse(text);
      openDocuments.put(uri, doc);

      // Validate the document
      validateDocument(uri, doc);
    } catch (Exception e) {
      System.err.println("Failed to parse document: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    String uri = params.getTextDocument().getUri();
    String text = params.getContentChanges().get(0).getText();

    System.err.println("Document changed: " + uri);
    try {
      //        openDocuments.put(uri, text);
      XdmNode doc = parser.parse(text);
      openDocuments.put(uri, doc);

      // Re-validate
      validateDocument(uri, doc);
    } catch (Exception e) {
      System.err.println("Failed to parse document: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    String uri = params.getTextDocument().getUri();
    System.err.println("Document closed: " + uri);
    openDocuments.remove(uri);
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    System.err.println("Document saved: " + params.getTextDocument().getUri());
  }

  public void revalidateAllOpenDocuments() {
    System.err.println("Revalidating all open documents");
    openDocuments.forEach(this::validateDocument);
  }

  private void validateDocument(String uri, XdmNode content) {
    // Add null check
    LanguageClient client = server.getClient();
    if (client == null) {
      System.err.println("Client not yet connected, skipping validation for " + uri);
      return;
    }

    List<Diagnostic> diagnostics = doValidation(content);

    System.err.println("Publishing " + diagnostics.size() + " diagnostics for " + uri);

    // Send diagnostics to client
    PublishDiagnosticsParams publishParams = new PublishDiagnosticsParams(uri, diagnostics);
    client.publishDiagnostics(publishParams);
  }

  private List<Diagnostic> doSlowValidation(XdmNode content) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    return diagnostics;
  }

  private List<Diagnostic> doValidation(XdmNode content) {
    List<Diagnostic> diagnostics = new ArrayList<>();

    // Simple validation: check if it contains "topic" element
    //    if (!content.select(Steps.child("topic")).exists()) {
    //      diagnostics.add(
    //          new Diagnostic(
    //              new Range(new Position(0, 0), new Position(0, 1)),
    //              "DITA topic element not found",
    //              DiagnosticSeverity.Warning,
    //              "dita-validator"));
    //    }

    var ids = content.select(Steps.descendant().then(Steps.attribute("id"))).toList();
    var idValues = ids.stream().map(XdmItem::getStringValue).toList();
    if (Set.copyOf(idValues).size() != idValues.size()) {
      Set<String> encountered = new HashSet<>();
      for (XdmNode id : ids) {
        var idValue = id.getStringValue();
        if (encountered.contains(idValue)) {
          var range = getAttributeRange(id);
          diagnostics.add(
              new Diagnostic(
                  range,
                  "Duplicate id attribute value '" + id.getStringValue() + "'",
                  DiagnosticSeverity.Error,
                  "dita-validator"));
        } else {
          encountered.add(idValue);
        }
      }
    }
    // Check for common DITA errors
    //    if (content.contains("<p>") && !content.contains("</p>")) {
    //      Diagnostic diagnostic = new Diagnostic();
    //      diagnostic.setSeverity(DiagnosticSeverity.Error);
    //      diagnostic.setRange(new Range(new Position(0, 0), new Position(0, 1)));
    //      diagnostic.setMessage("Unclosed <p> element");
    //      diagnostic.setSource("dita-validator");
    //
    //      diagnostics.add(diagnostic);
    //    }

    // Get current root map from workspace service
    // Access from server directly
    //    String rootMapUri = server.getCurrentRootMapUri();

    if (rootMap != null) {
      // Validate keyrefs against root map
      //            validateKeyrefs(content, diagnostics, rootMapUri);
    }

    return diagnostics;
  }

  private Range getAttributeRange(XdmNode attr) {
    var loc =
        attr.getParent()
            .getAttributeValue(
                new QName(LOC_PREFIX, LOC_NAMESPACE, "attr-" + attr.getNodeName().getLocalName()));
    return parseRange(loc);
  }

  private Range parseRange(String loc) {
    var tokens = loc.split("[:\\-]");

    return new Range(
        new Position(Integer.parseInt(tokens[0]) - 1, Integer.parseInt(tokens[1]) - 1),
        new Position(Integer.parseInt(tokens[2]) - 1, Integer.parseInt(tokens[3])));
  }

  public void setRootMapUri(String uri) {
    try {
      rootMap = parser.parse(uri);
    } catch (Exception e) {
      System.err.println("Failed to parse map document: " + e.getMessage());
    }
  }
}
