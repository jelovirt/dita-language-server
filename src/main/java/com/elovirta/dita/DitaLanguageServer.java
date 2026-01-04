package com.elovirta.dita;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DitaLanguageServer implements LanguageServer, LanguageClientAware {

  private static final Logger logger = LoggerFactory.getLogger(DitaLanguageServer.class);

  private final SmartDebouncer debouncer;
  private final DitaTextDocumentService textDocumentService;
  private final DitaWorkspaceService workspaceService;
  private final Properties properties;
  private LanguageClient client;

  public DitaLanguageServer() {
    this(new SmartDebouncer(1_000));
  }

  public DitaLanguageServer(SmartDebouncer debouncer) {
    this.debouncer = debouncer;
    textDocumentService = new DitaTextDocumentService(this, debouncer);
    workspaceService = new DitaWorkspaceService(this);
    properties = new Properties();
    try (InputStream input =
        DitaWorkspaceService.class.getClassLoader().getResourceAsStream("version.properties")) {
      properties.load(input);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read configuration", e);
    }
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    logger.info("DITA Language Server initializing...");
    if (params.getLocale() != null) {
      textDocumentService.setLocale(Locale.forLanguageTag(params.getLocale()));
    }

    var capabilities = new ServerCapabilities();
    capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
    capabilities.setDiagnosticProvider(new DiagnosticRegistrationOptions());
    capabilities.setCompletionProvider(new CompletionOptions());
    capabilities.setDefinitionProvider(new DefinitionOptions());
    capabilities.setHoverProvider(new HoverOptions());

    var commandOptions = new ExecuteCommandOptions(List.of("dita.setRootMap"));
    capabilities.setExecuteCommandProvider(commandOptions);

    var serverInfo =
        new ServerInfo(properties.getProperty("description"), properties.getProperty("version"));

    var result = new InitializeResult(capabilities, serverInfo);

    logger.info("DITA Language Server initialized");
    return CompletableFuture.completedFuture(result);
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    logger.info("DITA Language Server shutting down");
    return CompletableFuture.supplyAsync(
        () -> {
          debouncer.shutdown();
          return null;
        });
  }

  @Override
  public void exit() {
    logger.info("DITA Language Server exiting");
    System.exit(0);
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return textDocumentService;
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return workspaceService;
  }

  public void setCurrentRootMapUri(String uri) {
    textDocumentService.setRootMapUri(URI.create(uri));
    //    textDocumentService.revalidateAllOpenDocuments();
  }

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
    logger.info("Language client connected");
  }

  @Override
  public void setTrace(SetTraceParams params) {
    logger.info("Trace level set to: {}", params.getValue());
  }

  public LanguageClient getClient() {
    return client;
  }

  public static void main(String[] args) {
    logger.info("Starting DITA Language Server...");

    var server = new DitaLanguageServer();
    var launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);

    server.connect(launcher.getRemoteProxy());

    logger.info("DITA Language Server started and listening");
    launcher.startListening();
  }
}
