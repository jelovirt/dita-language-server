package com.elovirta.dita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.AfterEach;
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

    textDocumentService = new DitaTextDocumentService(server, new SmartDebouncer(0));
  }

  @AfterEach
  void tearDown() {
    clearInvocations(mockClient);
  }

  @Test
  void testValidDitaDocument() {
    var validDita = readResource("topics/valid.dita");
    textDocumentService.didOpen(createOpenParams("file:///topics/valid.dita", validDita));

    var captor = ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
    verify(mockClient).publishDiagnostics(captor.capture());
    var act = captor.getValue();

    assertEquals("file:///topics/valid.dita", act.getUri());
    assertTrue(act.getDiagnostics().isEmpty());
  }

  @Test
  void testInvalidDitaDocument()
      throws ExecutionException, InterruptedException, URISyntaxException {
    textDocumentService.setRootMapUri(getClass().getResource("/maps/keymap.ditamap").toURI());
    var invalidDita = readResource("invalid-keyref.dita");
    textDocumentService.didOpen(createOpenParams("file:///invalid-keyref.dita", invalidDita));
    var params = createChangeParams("file:///invalid-keyref.dita", invalidDita);
    textDocumentService.didChange(params);

    var captor = ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
    verify(mockClient, atLeast(2)).publishDiagnostics(captor.capture());
    var act = captor.getValue();

    assertEquals(
        new PublishDiagnosticsParams(
            "file:///invalid-keyref.dita",
            List.of(
                new Diagnostic(
                    new Range(new Position(4, 18), new Position(4, 22)),
                    "Cannot find definition for key 'xfoo'",
                    DiagnosticSeverity.Warning,
                    "dita-validator"))),
        act);
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

  private DidChangeTextDocumentParams createChangeParams(String uri, String text) {
    DidChangeTextDocumentParams params = new DidChangeTextDocumentParams();
    VersionedTextDocumentIdentifier document = new VersionedTextDocumentIdentifier();
    document.setUri(uri);
    document.setVersion(1);
    params.setTextDocument(document);
    params.setContentChanges(List.of(new TextDocumentContentChangeEvent(text)));
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
