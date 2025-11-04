package com.elovirta.dita;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DitaTextDocumentService implements TextDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DitaTextDocumentService.class);

    private final DitaLanguageServer server;
    private final Map<String, String> openDocuments = new ConcurrentHashMap<>();

    public DitaTextDocumentService(DitaLanguageServer server) {
        this.server = server;
    }

    @Override
    public CompletableFuture<DocumentDiagnosticReport> diagnostic(DocumentDiagnosticParams params) {
        String uri = params.getTextDocument().getUri();

        // Get the content from storage
        String content = openDocuments.get(uri);
        if (content == null) {
            // Document not opened yet, return empty diagnostics
            return CompletableFuture.completedFuture(
                    new DocumentDiagnosticReport(
                            new RelatedFullDocumentDiagnosticReport(Collections.emptyList()))
            );
        }

        List<Diagnostic> diagnostics = doValidation(content);

        FullDocumentDiagnosticReport fullReport = new FullDocumentDiagnosticReport(diagnostics);
        RelatedFullDocumentDiagnosticReport report = new RelatedFullDocumentDiagnosticReport(fullReport.getItems());

        return CompletableFuture.completedFuture(
                new DocumentDiagnosticReport(report)
        );
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String text = params.getTextDocument().getText();

        System.err.println("Document opened: " + uri);
        openDocuments.put(uri, text);

        // Validate the document
        validateDocument(uri, text);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String text = params.getContentChanges().get(0).getText();

        System.err.println("Document changed: " + uri);
        openDocuments.put(uri, text);

        // Re-validate
        validateDocument(uri, text);
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

    private void validateDocument(String uri, String content) {
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

    private List<Diagnostic> doValidation(String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        // Simple validation: check if it contains "topic" element
        if (!content.contains("<topic")) {
            Diagnostic diagnostic = new Diagnostic();
            diagnostic.setSeverity(DiagnosticSeverity.Warning);
            diagnostic.setRange(new Range(new Position(0, 0), new Position(0, 1)));
            diagnostic.setMessage("DITA topic element not found");
            diagnostic.setSource("dita-validator");

            diagnostics.add(diagnostic);
        }

        // Check for common DITA errors
        if (content.contains("<p>") && !content.contains("</p>")) {
            Diagnostic diagnostic = new Diagnostic();
            diagnostic.setSeverity(DiagnosticSeverity.Error);
            diagnostic.setRange(new Range(new Position(0, 0), new Position(0, 1)));
            diagnostic.setMessage("Unclosed <p> element");
            diagnostic.setSource("dita-validator");

            diagnostics.add(diagnostic);
        }

        return diagnostics;
    }
}