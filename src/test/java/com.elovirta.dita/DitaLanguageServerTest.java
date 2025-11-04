package com.elovirta.dita;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DitaLanguageServerTest {

    private DitaLanguageServer server;
    private LanguageClient mockClient;

    @BeforeEach
    void setUp() {
        server = new DitaLanguageServer();
        mockClient = Mockito.mock(LanguageClient.class);
        server.connect(mockClient);
    }

    @Test
    void testInitialize() throws ExecutionException, InterruptedException {
        InitializeParams params = new InitializeParams();
        params.setRootUri("file:///test/workspace");

        CompletableFuture<InitializeResult> result = server.initialize(params);
        InitializeResult initResult = result.get();

        assertNotNull(initResult);
        assertNotNull(initResult.getCapabilities());
        assertEquals(TextDocumentSyncKind.Full,
                initResult.getCapabilities().getTextDocumentSync().getLeft());
    }

    @Test
    void testDidOpen() {
        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams();
        TextDocumentItem document = new TextDocumentItem();
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
        DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams();
        TextDocumentItem document = new TextDocumentItem();
        document.setUri("file:///test.dita");
        document.setLanguageId("dita");
        document.setVersion(1);
        document.setText("<p>Unclosed paragraph");
        openParams.setTextDocument(document);

        server.getTextDocumentService().didOpen(openParams);

        // Request diagnostics
        DocumentDiagnosticParams diagnosticParams = new DocumentDiagnosticParams();
        diagnosticParams.setTextDocument(new TextDocumentIdentifier("file:///test.dita"));

        CompletableFuture<DocumentDiagnosticReport> diagnosticFuture =
                server.getTextDocumentService().diagnostic(diagnosticParams);
        DocumentDiagnosticReport report = diagnosticFuture.get();

        assertNotNull(report);
//        assertTrue(report instanceof RelatedFullDocumentDiagnosticReport);

//        RelatedFullDocumentDiagnosticReport fullReport =
//                (RelatedFullDocumentDiagnosticReport) report;
//        assertFalse(fullReport.getItems().isEmpty(), "Should have diagnostics for unclosed tag");
    }

    @Test
    void testDidChange() {
        // Open document
        DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams();
        TextDocumentItem document = new TextDocumentItem();
        document.setUri("file:///test.dita");
        document.setLanguageId("dita");
        document.setVersion(1);
        document.setText("<topic id=\"test\"></topic>");
        openParams.setTextDocument(document);
        server.getTextDocumentService().didOpen(openParams);

        // Change document
        DidChangeTextDocumentParams changeParams = new DidChangeTextDocumentParams();
        changeParams.setTextDocument(new VersionedTextDocumentIdentifier("file:///test.dita", 2));

        TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent();
        change.setText("<topic id=\"test\"><title>New Title</title></topic>");
        changeParams.getContentChanges().add(change);

        assertDoesNotThrow(() -> server.getTextDocumentService().didChange(changeParams));

        // Verify diagnostics were published again
        verify(mockClient, atLeast(2)).publishDiagnostics(any(PublishDiagnosticsParams.class));
    }

    @Test
    void testSetTrace() {
        SetTraceParams params = new SetTraceParams();
        params.setValue("verbose");

        assertDoesNotThrow(() -> server.setTrace(params));
    }
}