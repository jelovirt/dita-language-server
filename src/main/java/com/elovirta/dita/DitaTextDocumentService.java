package com.elovirta.dita;

import static com.elovirta.dita.LocationEnrichingXNIHandler.LOC_NAMESPACE;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
  private static final ResourceBundle LOCALE = ResourceBundle.getBundle("copy", Locale.ENGLISH);

  private static final String KEYREF_ELEM = "keyref";
  private static final String CONKEYREF_ELEM = "conkeyref";

  private final DitaLanguageServer server;
  private final Map<String, XdmNode> openDocuments = new ConcurrentHashMap<>();
  private String rootMapUri;
  private XdmNode rootMap;
  private final DitaParser parser;
  private final KeyManager keyManager;
  private final SmartDebouncer debouncer;

  public DitaTextDocumentService(DitaLanguageServer server, SmartDebouncer debouncer) {
    this.server = server;
    this.parser = new DitaParser();
    this.keyManager = new KeyManager();
    this.debouncer = debouncer;
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

      keyManager.read(rootMap);

      revalidateAllOpenDocuments();
    } catch (Exception e) {
      System.err.println("Failed to parse map document: " + e.getMessage());
    }
  }

  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
      CompletionParams params) {
    var attr = findAttribute(params.getTextDocument().getUri(), params.getPosition());
    //    System.err.println("Found attribute: " + attr);
    if (attr != null) {
      var localName = attr.getNodeName().getLocalName();
      if (localName.equals(KEYREF_ELEM)) {
        List<CompletionItem> items = new ArrayList<>();
        for (Map.Entry<String, XdmNode> keyDef : keyManager.keys()) {
          var key = keyDef.getKey();
          CompletionItem item = new CompletionItem(key);
          item.setKind(CompletionItemKind.Reference);
          item.setDetail("DITA key from root map");
          item.setDocumentation(keyDef.getValue().attribute("href"));
          items.add(item);
        }
        return CompletableFuture.completedFuture(Either.forLeft(items));
      }
      if (localName.equals(CONKEYREF_ELEM)) {
        List<CompletionItem> items = new ArrayList<>();
        var value = attr.getStringValue();
        if (value.contains("/")) {
          var key = value.substring(0, value.indexOf("/"));
          // FIXME: read all IDs from target topic
          CompletionItem item = new CompletionItem("id");
          item.setKind(CompletionItemKind.Reference);
          item.setDetail("ID from key " + key);
          //                  item.setDocumentation(keyDef.getValue().attribute("href"));
          items.add(item);
        } else {
          for (Map.Entry<String, XdmNode> keyDef : keyManager.keys()) {
            var key = keyDef.getKey();
            CompletionItem item = new CompletionItem(key);
            item.setKind(CompletionItemKind.Reference);
            item.setDetail("DITA key from root map");
            item.setDocumentation(keyDef.getValue().attribute("href"));
            items.add(item);
          }
        }
        return CompletableFuture.completedFuture(Either.forLeft(items));
      }
    }
    return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
  }

  private XdmNode findAttribute(String uri, Position position) {
    var doc = openDocuments.get(uri);
    // TODO: extract this into a TreeMap or TreeSet
    return doc.select(
            Steps.descendant()
                .then(
                    Steps.attribute(
                        attr ->
                            attr.getNodeName().getNamespaceUri().toString().equals(LOC_NAMESPACE)
                                && attr.getNodeName().getLocalName().startsWith("attr-"))))
        .map(
            attr ->
                attr.getParent()
                    .select(
                        Steps.attribute(
                            attr.getNodeName().getLocalName().substring("attr-".length())))
                    .asOptionalNode()
                    .map(a -> Map.entry(a, Utils.parseRange(attr.getStringValue())))
                    .orElse(null))
        .filter(Objects::nonNull)
        .filter(loc -> Utils.contains(loc.getValue(), position))
        .findFirst()
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  @Override
  public CompletableFuture<DocumentDiagnosticReport> diagnostic(DocumentDiagnosticParams params) {
    return CompletableFuture.completedFuture(null);
    //    String uri = params.getTextDocument().getUri();
    //
    //    // Get the content from storage
    //    //    XdmNode content = openDocuments.get(uri);
    //    //    if (content == null) {
    //    //      // Document not opened yet, return empty diagnostics
    //    //      return CompletableFuture.completedFuture(
    //    //          new DocumentDiagnosticReport(
    //    //              new RelatedFullDocumentDiagnosticReport(Collections.emptyList())));
    //    //    }
    //
    //    if (uri.equals(rootMapUri) && rootMap != null) {
    //      System.err.println("Root map changed, do async validate");
    //
    //      FullDocumentDiagnosticReport fullReport = new FullDocumentDiagnosticReport();
    //      RelatedFullDocumentDiagnosticReport report =
    //          new RelatedFullDocumentDiagnosticReport(fullReport.getItems());
    //
    //      var diagnostics =
    //          openDocuments.entrySet().stream()
    //              .filter(entry -> !entry.getKey().equals(rootMap))
    //              .map(
    //                  entry ->
    //                      Map.entry(
    //                          entry.getKey(),
    //                          Either
    //                              .<FullDocumentDiagnosticReport,
    // UnchangedDocumentDiagnosticReport>
    //                                  forLeft(
    //                                      new FullDocumentDiagnosticReport(
    //                                          doSlowValidation(entry.getValue())))))
    //              .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    ////      System.err.println(diagnostics);
    //      report.setRelatedDocuments(diagnostics);
    //
    //      return CompletableFuture.completedFuture(new DocumentDiagnosticReport(report));
    //    } else {
    //      System.err.printf("Async validate %s skipped%n", uri);
    //      //          var fullReport = new FullDocumentDiagnosticReport();
    //      // fullReport.getItems()
    //      var report = new RelatedFullDocumentDiagnosticReport();
    //      return CompletableFuture.completedFuture(new DocumentDiagnosticReport(report));
    //    }
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
                if (Objects.equals(rootMapUri, uri)) {
                  System.err.println("Root map changed, do debounced validate");
                  try {
                    debouncer.debounce(uri, this::revalidateAllOpenDocuments, 500);
                  } catch (Exception e) {
                    System.err.println("Failed to debounced validate: " + e.getMessage());
                    e.printStackTrace(System.err);
                  }
                }
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
    try {
      System.err.println("Revalidating all open documents");
      openDocuments.forEach(this::validateDocument);
    } catch (Exception e) {
      System.err.println("Failed to revalidate all open documents: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  private void validateDocument(String uri, XdmNode content) {
    try {
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
    } catch (Exception e) {
      System.err.println("Failed to validate document: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  private List<Diagnostic> doSlowValidation(XdmNode content) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    System.err.println("Do validation");
    if (rootMap != null) {
      System.err.println("Validate keyref");
      var keyrefs = content.select(Steps.descendant().then(Steps.attribute(KEYREF_ELEM))).toList();
      if (!keyrefs.isEmpty()) {
        for (XdmNode keyref : keyrefs) {
          if (!keyManager.containsKey(keyref.getStringValue())) {
            var range = Utils.getAttributeRange(keyref);
            diagnostics.add(
                new Diagnostic(
                    range,
                    LOCALE.getString("error.missing_key").formatted(keyref.getStringValue()),
                    DiagnosticSeverity.Warning,
                    "dita-validator"));
          }
        }
      }

      System.err.println("Validate conkeyref");
      var conkeyrefs =
          content.select(Steps.descendant().then(Steps.attribute(CONKEYREF_ELEM))).toList();
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
            var range = Utils.getAttributeRange(conkeyref);
            diagnostics.add(
                new Diagnostic(
                    range,
                    LOCALE.getString("error.missing_key").formatted(conkeyrefValue),
                    DiagnosticSeverity.Warning,
                    "dita-validator"));
          }
        }
      }
    }
    System.err.println("Validation done");
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
                  Utils.getAttributeRange(id),
                  LOCALE.getString("error.duplicate_id").formatted(id.getStringValue()),
                  DiagnosticSeverity.Error,
                  "dita-validator"));
        } else {
          encountered.add(idValue);
        }
      }
    }

    return diagnostics;
  }
}
