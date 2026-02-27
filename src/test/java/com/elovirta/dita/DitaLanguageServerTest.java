package com.elovirta.dita;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DitaLanguageServerTest {

  private DitaLanguageServer server;
  private LanguageClient mockClient;

  @BeforeEach
  void setUp() {
    server = new DitaLanguageServer(new SmartDebouncer(0));
    mockClient = Mockito.mock(LanguageClient.class);
    server.connect(mockClient);
  }

  @Test
  void testInitialize() throws ExecutionException, InterruptedException {
    var params = new InitializeParams();
    params.setRootUri("file:///test/workspace");

    var result = server.initialize(params);
    InitializeResult initResult = result.get();

    assertNotNull(initResult);
    assertNotNull(initResult.getCapabilities());
    assertEquals(
        TextDocumentSyncKind.Full, initResult.getCapabilities().getTextDocumentSync().getLeft());
  }

  @Test
  void testDidOpen() {
    var params = new DidOpenTextDocumentParams();
    var document = new TextDocumentItem();
    document.setUri("file:///test.dita");
    document.setLanguageId("dita");
    document.setVersion(1);
    document.setText("<?xml version=\"1.0\"?>\n<topic id=\"test\"><title>Test</title></topic>");
    params.setTextDocument(document);

    // Should not throw
    assertDoesNotThrow(() -> server.getTextDocumentService().didOpen(params));

    // Verify diagnostics were published
    verify(mockClient, atLeastOnce()).publishDiagnostics(any(PublishDiagnosticsParams.class));
  }

  @Test
  @Disabled
  void testDiagnosticValidation() throws ExecutionException, InterruptedException {
    // Open document first
    var openParams = new DidOpenTextDocumentParams();
    var document = new TextDocumentItem();
    document.setUri("file:///test.dita");
    document.setLanguageId("dita");
    document.setVersion(1);
    document.setText("<p>Unclosed paragraph");
    openParams.setTextDocument(document);

    server.getTextDocumentService().didOpen(openParams);

    // Request diagnostics
    var diagnosticParams = new DocumentDiagnosticParams();
    diagnosticParams.setTextDocument(new TextDocumentIdentifier("file:///test.dita"));

    var diagnosticFuture = server.getTextDocumentService().diagnostic(diagnosticParams);
    var act = diagnosticFuture.get();

    assertEquals(new DocumentDiagnosticReport(new RelatedFullDocumentDiagnosticReport()), act);
  }

  @Test
  void testDidChange() {
    var valueCapture = ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
    doNothing().when(mockClient).publishDiagnostics(valueCapture.capture());

    // Open document
    var openParams = new DidOpenTextDocumentParams();
    var document = new TextDocumentItem();
    document.setUri("file:///test.dita");
    document.setLanguageId("dita");
    document.setVersion(1);
    document.setText("<topic id=\"test\"></topic>");
    openParams.setTextDocument(document);
    server.getTextDocumentService().didOpen(openParams);

    // Change document
    var changeParams = new DidChangeTextDocumentParams();
    changeParams.setTextDocument(new VersionedTextDocumentIdentifier("file:///test.dita", 2));

    var change = new TextDocumentContentChangeEvent();
    change.setText("<topic id=\"test\"><title>New Title</title></topic>");
    changeParams.getContentChanges().add(change);

    server.getTextDocumentService().didChange(changeParams);

    assertEquals(
        new PublishDiagnosticsParams(
            "file:///test.dita",
            List.of(
                new Diagnostic(
                    new Range(new Position(1, 8), new Position(1, 8)),
                    "Document is invalid: no grammar found.",
                    DiagnosticSeverity.Error,
                    "file:///test.dita"),
                new Diagnostic(
                    new Range(new Position(1, 8), new Position(1, 8)),
                    "Document root element \"topic\", must match DOCTYPE root \"null\".",
                    DiagnosticSeverity.Error,
                    "file:///test.dita"))),
        valueCapture.getValue());
  }

  @Test
  void testSetTrace() {
    var params = new SetTraceParams();
    params.setValue("verbose");

    assertDoesNotThrow(() -> server.setTrace(params));
  }
}
