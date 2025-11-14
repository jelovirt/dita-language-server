package com.elovirta.dita;

import static com.elovirta.dita.LocationEnrichingXNIHandler.LOC_NAMESPACE;
import static com.elovirta.dita.LocationEnrichingXNIHandler.LOC_PREFIX;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.streams.Steps;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DitaTextDocumentService implements TextDocumentService {

  private static final Logger logger = LoggerFactory.getLogger(DitaTextDocumentService.class);

  private final DitaLanguageServer server;
  private final Map<String, XdmNode> openDocuments = new ConcurrentHashMap<>();
  private String rootMapUri;
  private XdmNode rootMap;
  private final DitaParser parser;
  private final KeyManager keyManager;

  public DitaTextDocumentService(DitaLanguageServer server) {
    this.server = server;
    this.parser = new DitaParser();
    this.keyManager = new KeyManager();
    //        var resolver = this.parser.getCatalogResolver();
    //        try {
    //            var res = resolver.resolveEntity("-//OASIS//DTD DITA 1.3 Base Map//EN", null);
    //            System.err.println(res);
    //        } catch (SAXException | IOException e) {
    //            throw new RuntimeException(e);
    //        }
  }

  public void setRootMapUri(String uri) {
    rootMapUri = uri;
    System.err.println("Setting root map URI: " + uri);
    try {
      var content = Files.readString(Paths.get(URI.create(uri)));
      rootMap = parser.parse(content);
      openDocuments.put(uri, rootMap);
    } catch (Exception e) {
      System.err.println("Failed to parse map document: " + e.getMessage());
    }
  }

  @Override
  public CompletableFuture<DocumentDiagnosticReport> diagnostic(DocumentDiagnosticParams params) {
    String uri = params.getTextDocument().getUri();

    // Get the content from storage
    //    XdmNode content = openDocuments.get(uri);
    //    if (content == null) {
    //      // Document not opened yet, return empty diagnostics
    //      return CompletableFuture.completedFuture(
    //          new DocumentDiagnosticReport(
    //              new RelatedFullDocumentDiagnosticReport(Collections.emptyList())));
    //    }

    if (uri.equals(rootMapUri) && rootMap != null) {
      System.err.println("Root map changed, do async validate");

      FullDocumentDiagnosticReport fullReport = new FullDocumentDiagnosticReport();
      RelatedFullDocumentDiagnosticReport report =
          new RelatedFullDocumentDiagnosticReport(fullReport.getItems());

      var diagnostics =
          openDocuments.entrySet().stream()
              .filter(entry -> !entry.getKey().equals(rootMap))
              .map(
                  entry ->
                      Map.entry(
                          entry.getKey(),
                          Either
                              .<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport>
                                  forLeft(
                                      new FullDocumentDiagnosticReport(
                                          doSlowValidation(entry.getValue())))))
              .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
      System.err.println(diagnostics);
      report.setRelatedDocuments(diagnostics);

      return CompletableFuture.completedFuture(new DocumentDiagnosticReport(report));
    } else {
      System.err.printf("Async validate %s skipped%n", uri);
      //          var fullReport = new FullDocumentDiagnosticReport();
      // fullReport.getItems()
      var report = new RelatedFullDocumentDiagnosticReport();
      return CompletableFuture.completedFuture(new DocumentDiagnosticReport(report));
    }
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    String uri = params.getTextDocument().getUri();
    String text = params.getTextDocument().getText();

    //    System.err.println("Document opened: " + uri);
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
    if (params.getContentChanges().size() != 1) {
      throw new RuntimeException("DidChange not supported for multiple changes: " + params);
    }
    String uri = params.getTextDocument().getUri();
    String text = params.getContentChanges().get(0).getText();

    CompletableFuture.supplyAsync(
            () -> {
              XdmNode doc = parser.parse(text);
              openDocuments.put(uri, doc);

              if (Objects.equals(rootMapUri, uri)) {
                  keyManager.read(doc);
              }

              return doc;
            })
        .thenAccept(
            doc -> {
              if (doc != null) {
                validateDocument(uri, doc);
              }
            })
        .exceptionally(
            ex -> {
              System.err.println("Failed to parse: " + ex.getMessage());
              return null;
            });
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    String uri = params.getTextDocument().getUri();
    //    System.err.println("Document closed: " + uri);
    openDocuments.remove(uri);
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    //    System.err.println("Document saved: " + params.getTextDocument().getUri());
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

    var diagnostics = doValidation(content);
    diagnostics.addAll(doSlowValidation(content));

    var publishParams = new PublishDiagnosticsParams(uri, diagnostics);
    client.publishDiagnostics(publishParams);
  }

  private List<Diagnostic> doSlowValidation(XdmNode content) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    System.err.println("Do slow validation");
    if (rootMap != null) {
      var keyrefs = content.select(Steps.descendant().then(Steps.attribute("keyref"))).toList();
      if (!keyrefs.isEmpty()) {
        for (XdmNode keyref : keyrefs) {
          if (!keyManager.containsKey(keyref.getStringValue())) {
            var range = getAttributeRange(keyref);
            diagnostics.add(
                new Diagnostic(
                    range,
                    "Cannot find definition for key '%s'".formatted(keyref.getStringValue()),
                    DiagnosticSeverity.Warning,
                    "dita-validator"));
          }
        }
      }

      var conkeyrefs =
          content.select(Steps.descendant().then(Steps.attribute("conkeyref"))).toList();
      if (!conkeyrefs.isEmpty()) {
        for (XdmNode conkeyref : conkeyrefs) {
          var conkeyrefValue = conkeyref.getStringValue();
          var keyref = conkeyrefValue;
          var separator = conkeyrefValue.indexOf('/');
          if (separator != -1) {
            keyref = keyref.substring(0, separator);
          }
          if (!keyManager.containsKey(keyref)) {
            // FIXME range should match only the key name
            var range = getAttributeRange(conkeyref);
            diagnostics.add(
                new Diagnostic(
                    range,
                    "Cannot find definition for key '%s'".formatted(conkeyrefValue),
                    DiagnosticSeverity.Warning,
                    "dita-validator"));
          }
        }
      }
    }

    return diagnostics;
  }

  private List<Diagnostic> doValidation(XdmNode content) {
    List<Diagnostic> diagnostics = new ArrayList<>();

    var ids = content.select(Steps.descendant().then(Steps.attribute("id"))).toList();
    var idValues = ids.stream().map(XdmItem::getStringValue).toList();
    if (Set.copyOf(idValues).size() != idValues.size()) {
      Set<String> encountered = new HashSet<>();
      for (XdmNode id : ids) {
        var idValue = id.getStringValue();
        if (encountered.contains(idValue)) {
          diagnostics.add(
              new Diagnostic(
                  getAttributeRange(id),
                  "Duplicate id attribute value '%s'".formatted(id.getStringValue()),
                  DiagnosticSeverity.Error,
                  "dita-validator"));
        } else {
          encountered.add(idValue);
        }
      }
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

}
