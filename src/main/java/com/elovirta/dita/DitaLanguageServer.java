package com.elovirta.dita;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DitaLanguageServer implements LanguageServer, LanguageClientAware {

  private static final Logger logger = LoggerFactory.getLogger(DitaWorkspaceService.class);

  private final SmartDebouncer debouncer;
  private final DitaTextDocumentService textDocumentService;
  private final DitaWorkspaceService workspaceService;
  private final Properties properties;
  private LanguageClient client;

  private String currentRootMapUri = null;

  public DitaLanguageServer() {
    debouncer = new SmartDebouncer();
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
    System.err.println("DITA Language Server initializing...");
    //    System.err.println("Root URI: " + params.getWorkspaceFolders());

    // Declare server capabilities
    ServerCapabilities capabilities = new ServerCapabilities();
    // We support text document sync
    capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
    // We provide diagnostics (validation)
    capabilities.setDiagnosticProvider(new DiagnosticRegistrationOptions());
    capabilities.setCompletionProvider(new CompletionOptions());
    capabilities.setDefinitionProvider(new DefinitionOptions());
    capabilities.setHoverProvider(new HoverOptions());
    // Advertise custom command
    ExecuteCommandOptions commandOptions = new ExecuteCommandOptions(List.of("dita.setRootMap"));
    capabilities.setExecuteCommandProvider(commandOptions);

    ServerInfo serverInfo =
        new ServerInfo(properties.getProperty("description"), properties.getProperty("version"));

    InitializeResult result = new InitializeResult(capabilities, serverInfo);

    System.err.println("DITA Language Server initialized");
    return CompletableFuture.completedFuture(result);
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    System.err.println("DITA Language Server shutting down");
    return CompletableFuture.supplyAsync(
        () -> {
          debouncer.shutdown();
          return null;
        });
  }

  @Override
  public void exit() {
    System.err.println("DITA Language Server exiting");
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

  public String getCurrentRootMapUri() {
    return currentRootMapUri;
  }

  public void setCurrentRootMapUri(String uri) {
    this.currentRootMapUri = uri;
    textDocumentService.setRootMapUri(URI.create(uri));
    // Optionally trigger revalidation here
    textDocumentService.revalidateAllOpenDocuments();
  }

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
    System.err.println("Language client connected");
  }

  @Override
  public void setTrace(SetTraceParams params) {
    // Optional: implement trace logging based on params.getValue()
    // Values can be "off", "messages", or "verbose"
    System.err.println("Trace level set to: " + params.getValue());
  }

  public LanguageClient getClient() {
    return client;
  }

  // Main method - starts the language server
  public static void main(String[] args) {
    System.err.println("Starting DITA Language Server...");

    DitaLanguageServer server = new DitaLanguageServer();
    Launcher<LanguageClient> launcher =
        LSPLauncher.createServerLauncher(server, System.in, System.out);

    // This connects the client to the server
    server.connect(launcher.getRemoteProxy());

    System.err.println("DITA Language Server started and listening");
    launcher.startListening();
  }
}
