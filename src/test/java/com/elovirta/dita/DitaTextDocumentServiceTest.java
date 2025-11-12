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
  void testValidDitaDocument() {
    String validDita = readResource("valid.dita");

    var params = createOpenParams("file:///valid.dita", validDita);
    textDocumentService.didOpen(params);

    var captor = ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
    verify(mockClient).publishDiagnostics(captor.capture());

    var diagnostics = captor.getValue();

    assertEquals("file:///valid.dita", diagnostics.getUri());
    assertTrue(diagnostics.getDiagnostics().isEmpty());
  }

  @Test
  void testInvalidDitaDocument() throws ExecutionException, InterruptedException {
    textDocumentService.setRootMapUri(getClass().getResource("/keymap.ditamap").toString());
    String invalidDita = readResource("invalid-conkeyref.dita");
    var params = createOpenParams("file:///invalid-conkeyref.dita", invalidDita);
    textDocumentService.didOpen(params);

    var act =
        textDocumentService.diagnostic(
            new DocumentDiagnosticParams(
                new TextDocumentIdentifier("file:///invalid-conkeyref.dita")));

    var diagnostics = act.get().getRelatedFullDocumentDiagnosticReport();

    assertEquals(
        List.of(
            new Diagnostic(
                new Range(new Position(4, 18), new Position(4, 21)),
                "Cannot find definition for key 'foo'",
                DiagnosticSeverity.Warning,
                "dita-validator")),
        diagnostics.getItems());
  }

  @Test
  void testInvalidDitaDocumentId() {
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
                new Range(new Position(6, 11), new Position(6, 17)),
                "Duplicate id attribute value 'second'",
                DiagnosticSeverity.Error,
                "dita-validator")),
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
