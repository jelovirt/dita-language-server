package com.elovirta.dita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DitaTextDocumentServiceTest {

  private DitaLanguageServer server;
  private DitaTextDocumentService textDocumentService;
  private LanguageClient mockClient;

  @BeforeEach
  void setUp() {
    server = Mockito.mock(DitaLanguageServer.class);
    mockClient = Mockito.mock(LanguageClient.class);
    when(server.getClient()).thenReturn(mockClient);

    textDocumentService = new DitaTextDocumentService(server);
  }

  @Test
  void testValidDitaDocument() throws ExecutionException, InterruptedException {
    String validDita = readResource("valid.dita");

    DidOpenTextDocumentParams params = createOpenParams("file:///valid.dita", validDita);
    textDocumentService.didOpen(params);

    // Capture the diagnostics
    ArgumentCaptor<PublishDiagnosticsParams> captor =
        ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
    verify(mockClient).publishDiagnostics(captor.capture());

    PublishDiagnosticsParams diagnostics = captor.getValue();

    assertEquals("file:///valid.dita", diagnostics.getUri());
    assertTrue(diagnostics.getDiagnostics().isEmpty());
  }

  @Test
  void testInvalidDitaDocument() throws ExecutionException, InterruptedException {
    String invalidDita = readResource("invalid.dita");

    DidOpenTextDocumentParams params = createOpenParams("file:///invalid.dita", invalidDita);
    textDocumentService.didOpen(params);

    ArgumentCaptor<PublishDiagnosticsParams> captor =
        ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
    verify(mockClient).publishDiagnostics(captor.capture());

    PublishDiagnosticsParams diagnostics = captor.getValue();

    assertEquals("file:///invalid.dita", diagnostics.getUri());
    assertEquals(
        List.of(
            new Diagnostic(
                new Range(new Position(0, 0), new Position(0, 1)),
                "DITA topic element not found",
                DiagnosticSeverity.Warning,
                "dita-validator")
            //            new Diagnostic(
            //                new Range(new Position(0, 0), new Position(0, 1)),
            //                "Unclosed <p> element",
            //                DiagnosticSeverity.Error,
            //                "dita-validator")
            ),
        diagnostics.getDiagnostics());
  }

  @Test
  void testInvalidDitaDocumentId() throws ExecutionException, InterruptedException {
    String invalidDita = readResource("invalid-id.dita");

    DidOpenTextDocumentParams params = createOpenParams("file:///invalid-id.dita", invalidDita);
    textDocumentService.didOpen(params);

    ArgumentCaptor<PublishDiagnosticsParams> captor =
        ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
    verify(mockClient).publishDiagnostics(captor.capture());

    PublishDiagnosticsParams diagnostics = captor.getValue();

    assertEquals("file:///invalid-id.dita", diagnostics.getUri());
    assertEquals(
        List.of(
            new Diagnostic(
                new Range(new Position(7, 20), new Position(7, 20)),
                "Duplicate id attribute value 'second'",
                DiagnosticSeverity.Warning,
                "dita-validator")
            //            new Diagnostic(
            //                new Range(new Position(0, 0), new Position(0, 1)),
            //                "Unclosed <p> element",
            //                DiagnosticSeverity.Error,
            //                "dita-validator")
            ),
        diagnostics.getDiagnostics());
  }

  private DidOpenTextDocumentParams createOpenParams(String uri, String text) {
    DidOpenTextDocumentParams params = new DidOpenTextDocumentParams();
    TextDocumentItem document = new TextDocumentItem();
    document.setUri(uri);
    document.setLanguageId("dita");
    document.setVersion(1);
    document.setText(text);
    params.setTextDocument(document);
    return params;
  }

  private String readResource(String path) {
    try (var in = getClass().getClassLoader().getResourceAsStream(path)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
