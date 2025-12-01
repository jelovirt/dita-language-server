package com.elovirta.dita;

import static com.elovirta.dita.LocationEnrichingXNIHandler.LOC_NAMESPACE;
import static com.elovirta.dita.Utils.*;
import static net.sf.saxon.s9api.streams.Predicates.isElement;
import static net.sf.saxon.s9api.streams.Steps.attribute;
import static net.sf.saxon.s9api.streams.Steps.descendant;

import com.elovirta.dita.KeyManager.KeyDefinition;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DitaTextDocumentService implements TextDocumentService {

  private static final Logger logger = LoggerFactory.getLogger(DitaTextDocumentService.class);

  private static final String KEYREF_ATTR = "keyref";
  private static final String CONKEYREF_ATTR = "conkeyref";
  private static final String HREF_ATTR = "href";
  private static final String ID_ATTR = "id";

  public static final String SOURCE = "dita-validator";

  private final DitaLanguageServer server;
  private final DitaParser parser;
  private final DocumentManager documentManager;
  private final KeyManager keyManager;
  private final SmartDebouncer debouncer;

  private URI rootMapUri;
  private XdmNode rootMap;
  private ResourceBundle LOCALE;

  public DitaTextDocumentService(DitaLanguageServer server, SmartDebouncer debouncer) {
    this.server = server;
    this.parser = new DitaParser();
    this.documentManager = new DocumentManager();
    this.keyManager = new KeyManager();
    this.debouncer = debouncer;
    //        var resolver = this.parser.getCatalogResolver();
    //        try {
    //            var res = resolver.resolveEntity("-//OASIS//DTD DITA 1.3 Base Map//EN", null);
    //            System.err.println(res);
    //        } catch (SAXException | IOException e) {
    //            throw new RuntimeException(e);
    //        }
    this.LOCALE = ResourceBundle.getBundle("copy", Locale.ENGLISH);
  }

  public void setLocale(Locale locale) {
    this.LOCALE = ResourceBundle.getBundle("copy", locale);
  }

  public void setRootMapUri(URI uri) {
    rootMapUri = uri;
    System.err.println("Setting root map URI: " + uri);
    try {
      var content = Files.readString(Paths.get(uri));
      rootMap = parser.parse(content);
      documentManager.put(uri, rootMap);

      keyManager.read(uri, rootMap);

      revalidateAllOpenDocuments();
    } catch (Exception e) {
      System.err.println("Failed to parse map document: " + e.getMessage());
    }
  }

  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
      CompletionParams params) {
    var documentUri = URI.create(params.getTextDocument().getUri());
    var attr = findAttribute(documentUri, params.getPosition());
    if (attr != null) {
      var localName = attr.getNodeName().getLocalName();
      if (localName.equals(HREF_ATTR)) {
        List<CompletionItem> items = new ArrayList<>();
        try {
          var hrefValue = new URI(attr.getStringValue());
          var uri = stripFragment(documentUri.resolve(hrefValue));
          if (!documentManager.exists(stripFragment(uri))) {
            System.err.println("Don't suggest files, this is better left to editor");
          } else {
            var fragment = Objects.requireNonNullElse(hrefValue.getFragment(), "");
            var separator = fragment.indexOf('/');
            var topicId = separator != -1 ? fragment.substring(0, separator) : fragment;
            var elementId = separator != -1 ? fragment.substring(separator + 1) : null;
            if (elementId != null) {
              var elementIds = documentManager.listElementIds(uri, topicId);
              for (String id : elementIds) {
                CompletionItem item = new CompletionItem(id);
                item.setKind(CompletionItemKind.Reference);
                item.setDetail("ID " + id + " from href " + hrefValue);
                items.add(item);
              }
            } else {
              var elementIds = documentManager.listIds(uri);
              for (String id : elementIds) {
                CompletionItem item = new CompletionItem(id);
                item.setKind(CompletionItemKind.Reference);
                item.setDetail("ID " + id + " from href " + hrefValue);
                items.add(item);
              }
            }
          }
        } catch (URISyntaxException e) {
          // TODO: attempt to fix invalid URI
        }
        return CompletableFuture.completedFuture(Either.forLeft(items));
      } else if (localName.equals(KEYREF_ATTR) || localName.equals(CONKEYREF_ATTR)) {
        List<CompletionItem> items = new ArrayList<>();
        var value = attr.getStringValue();
        if (value.contains("/")) {
          var key = value.substring(0, value.indexOf("/"));
          var keyDefinition = keyManager.get(key);
          if (keyDefinition != null) {
            var uri = keyDefinition.target();
            for (String listId :
                documentManager.listElementIds(stripFragment(uri), uri.getFragment())) {
              CompletionItem item = new CompletionItem(listId);
              item.setKind(CompletionItemKind.Reference);
              item.setDetail("ID " + listId + " from key " + key);
              items.add(item);
            }
          }
        } else {
          for (Map.Entry<String, KeyDefinition> keyDef : keyManager.keys()) {
            var key = keyDef.getKey();
            var keyDefinition = keyDef.getValue();
            CompletionItem item = new CompletionItem(key);
            item.setKind(CompletionItemKind.Reference);
            item.setDetail("DITA key from root map");
            item.setDocumentation(
                keyDefinition.target() != null ? keyDefinition.target().toString() : null);
            items.add(item);
          }
        }
        return CompletableFuture.completedFuture(Either.forLeft(items));
      }
    }
    return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
  }

  @Override
  public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
      definition(DefinitionParams params) {
    var documentUri = URI.create(params.getTextDocument().getUri());
    var attr = findAttribute(documentUri, params.getPosition());
    if (attr != null) {
      var localName = attr.getNodeName().getLocalName();
      if (localName.equals(KEYREF_ATTR) || localName.equals(CONKEYREF_ATTR)) {
        if (rootMapUri != null) {
          var keyName = attr.getStringValue();
          var separator = keyName.indexOf('/');
          if (separator != -1) {
            keyName = keyName.substring(0, separator);
          }
          var keyDefinition = keyManager.get(keyName);
          if (keyDefinition != null) {
            return CompletableFuture.completedFuture(
                Either.forLeft(List.of(keyDefinition.location())));
          }
        } else {
          System.err.println("Cannot goto key definition because no root map defined");
        }
      }
    }
    return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
  }

  private XdmNode findAttribute(URI uri, Position position) {
    var doc = documentManager.get(uri).document();
    // TODO: extract this into a TreeMap or TreeSet
    return doc.select(
            descendant()
                .then(
                    attribute(
                        attr ->
                            attr.getNodeName().getNamespaceUri().toString().equals(LOC_NAMESPACE)
                                && attr.getNodeName().getLocalName().startsWith("attr-"))))
        .map(
            attr ->
                attr.getParent()
                    .select(
                        attribute(attr.getNodeName().getLocalName().substring("attr-".length())))
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
  public CompletableFuture<Hover> hover(HoverParams params) {
    var documentUri = URI.create(params.getTextDocument().getUri());
    var attr = findAttribute(documentUri, params.getPosition());
    if (attr != null) {
      var localName = attr.getNodeName().getLocalName();
      if (localName.equals(KEYREF_ATTR) || localName.equals(CONKEYREF_ATTR)) {
        if (rootMapUri != null) {
          var keyName = attr.getStringValue();
          var separator = keyName.indexOf('/');
          if (separator != -1) {
            keyName = keyName.substring(0, separator);
          }
          var keyDefinition = keyManager.get(keyName);
          if (keyDefinition != null) {
            MarkupContent content = null;
            XdmNode parent = attr.getParent();
            if (TOPIC_XREF.test(parent) || TOPIC_LINK.test(parent)) {
              if (keyDefinition.navtitle() != null) {
                content = new MarkupContent(MarkupKind.PLAINTEXT, keyDefinition.navtitle());
              } else if (keyDefinition.target() != null) {
                content =
                    new MarkupContent(MarkupKind.PLAINTEXT, keyDefinition.target().toString());
              }
            } else if (TOPIC_IMAGE.test(parent)) {
              if (keyDefinition.target() != null) {
                content =
                    new MarkupContent(
                        // MarkupKind.MARKDOWN, "![](" + keyDefinition.target() + ")");
                        MarkupKind.PLAINTEXT, keyDefinition.target().toString());
              }
            } else {
              if (keyDefinition.text() != null) {
                content = new MarkupContent(MarkupKind.PLAINTEXT, keyDefinition.text());
              } else if (keyDefinition.target() != null) {
                content =
                    new MarkupContent(MarkupKind.PLAINTEXT, keyDefinition.target().toString());
              }
            }
            if (content != null) {
              return CompletableFuture.completedFuture(new Hover(content));
            }
          }
        }
      }
    }
    return CompletableFuture.completedFuture(null);
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
    URI uri = URI.create(params.getTextDocument().getUri());
    String text = params.getTextDocument().getText();
    try {
      XdmNode doc = parser.parse(text);
      documentManager.put(uri, doc);

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
    URI uri = URI.create(params.getTextDocument().getUri());
    String text = params.getContentChanges().get(0).getText();

    CompletableFuture.supplyAsync(
            () -> {
              XdmNode doc = parser.parse(text);
              documentManager.put(uri, doc);

              if (Objects.equals(rootMapUri, uri)) {
                keyManager.read(uri, doc);
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
                    debouncer.debounce(uri.toString(), this::revalidateAllOpenDocuments, 500);
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
    URI uri = URI.create(params.getTextDocument().getUri());
    documentManager.remove(uri);
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {}

  public void revalidateAllOpenDocuments() {
    try {
      System.err.println("Revalidating all open documents");
      documentManager.forEach(this::validateDocument);
    } catch (Exception e) {
      System.err.println("Failed to revalidate all open documents: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  private void validateDocument(URI uri, XdmNode content) {
    try {
      LanguageClient client = server.getClient();
      if (client == null) {
        System.err.println("Client not yet connected, skipping validation for " + uri);
        return;
      }

      var diagnostics = doValidation(content);
      diagnostics.addAll(doSlowValidation(content, uri));

      var publishParams = new PublishDiagnosticsParams(uri.toString(), diagnostics);
      client.publishDiagnostics(publishParams);
    } catch (Exception e) {
      System.err.println("Failed to validate document: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  private List<Diagnostic> doSlowValidation(XdmNode content, URI documentUri) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    if (rootMap != null) {
      var keyrefs =
          content
              .select(
                  descendant(isElement())
                      .then(attribute(CONKEYREF_ATTR).cat(attribute(KEYREF_ATTR))))
              .toList();
      if (!keyrefs.isEmpty()) {
        for (XdmNode keyref : keyrefs) {
          var keyrefValue = keyref.getStringValue();
          var separator = keyrefValue.indexOf('/');
          var keyName = separator != -1 ? keyrefValue.substring(0, separator) : keyrefValue;
          var id = separator != -1 ? keyrefValue.substring(separator + 1) : null;
          var keyDefinition = keyManager.get(keyName);
          if (keyDefinition == null) {
            // FIXME range should match only the key name
            var range = Utils.getAttributeRange(keyref);
            diagnostics.add(
                new Diagnostic(
                    range,
                    LOCALE.getString("error.missing_key").formatted(keyrefValue),
                    DiagnosticSeverity.Warning,
                    SOURCE));
          } else {
            var uri = keyDefinition.target();
            if (uri == null) {
              var range = Utils.getAttributeRange(keyref);
              diagnostics.add(
                  new Diagnostic(
                      range,
                      LOCALE.getString("error.keyref_target_undefined"),
                      DiagnosticSeverity.Warning,
                      SOURCE));
            } else if (id != null) {
              var ids = documentManager.listElementIds(stripFragment(uri), uri.getFragment());
              if (!ids.contains(id)) {
                var range = Utils.getAttributeRange(keyref);
                diagnostics.add(
                    new Diagnostic(
                        range,
                        LOCALE.getString("error.keyref_id_missing").formatted(id),
                        DiagnosticSeverity.Warning,
                        SOURCE));
              }
            }
          }
        }
      }
    }

    var hrefs = content.select(descendant(isElement()).then(attribute(HREF_ATTR))).toList();
    if (!hrefs.isEmpty()) {
      for (XdmNode href : hrefs) {
        try {
          var hrefValue = new URI(href.getStringValue());
          var uri = stripFragment(documentUri.resolve(hrefValue));
          if (!documentManager.exists(uri)) {
            var range = Utils.getAttributeRange(href);
            diagnostics.add(
                new Diagnostic(
                    range,
                    LOCALE.getString("error.href_target_missing"),
                    DiagnosticSeverity.Warning,
                    SOURCE));
          } else {
            var fragment = hrefValue.getFragment();
            if (fragment != null) {
              var separator = fragment.indexOf('/');
              var topicId = separator != -1 ? fragment.substring(0, separator) : fragment;
              var elementId = separator != -1 ? fragment.substring(separator + 1) : null;
              if (elementId != null) {
                if (!documentManager.exists(stripFragment(uri), topicId)) {
                  var range = Utils.getAttributeRange(href);
                  diagnostics.add(
                      new Diagnostic(
                          range,
                          LOCALE.getString("error.keyref_id_missing").formatted(topicId),
                          DiagnosticSeverity.Warning,
                          SOURCE));
                } else if (!documentManager.exists(stripFragment(uri), topicId, elementId)) {
                  var range = Utils.getAttributeRange(href);
                  diagnostics.add(
                      new Diagnostic(
                          range,
                          LOCALE.getString("error.keyref_id_missing").formatted(elementId),
                          DiagnosticSeverity.Warning,
                          SOURCE));
                }
              } else if (!documentManager.exists(stripFragment(uri), topicId)) {
                var range = Utils.getAttributeRange(href);
                diagnostics.add(
                    new Diagnostic(
                        range,
                        LOCALE.getString("error.keyref_id_missing").formatted(topicId),
                        DiagnosticSeverity.Warning,
                        SOURCE));
              }
            }
          }
        } catch (URISyntaxException e) {
          var range = Utils.getAttributeRange(href);
          diagnostics.add(
              new Diagnostic(
                  range,
                  LOCALE.getString("error.href_invalid_uri"),
                  DiagnosticSeverity.Warning,
                  SOURCE));
        }
      }
    }

    return diagnostics;
  }

  private List<Diagnostic> doValidation(XdmNode content) {
    List<Diagnostic> diagnostics = new ArrayList<>();

    var ids = content.select(descendant().then(attribute(ID_ATTR))).toList();
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
                  SOURCE));
        } else {
          encountered.add(idValue);
        }
      }
    }

    return diagnostics;
  }
}
