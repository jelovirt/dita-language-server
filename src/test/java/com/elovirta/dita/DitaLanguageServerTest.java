package com.elovirta.dita;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    assertDoesNotThrow(() -> server.getTextDocumentService().didChange(changeParams));

    // Verify diagnostics were published again
    verify(mockClient, atLeast(1)).publishDiagnostics(any(PublishDiagnosticsParams.class));
  }

  @Test
  void testSetTrace() {
    var params = new SetTraceParams();
    params.setValue("verbose");

    assertDoesNotThrow(() -> server.setTrace(params));
  }
}
