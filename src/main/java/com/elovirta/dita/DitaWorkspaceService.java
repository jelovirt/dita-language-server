package com.elovirta.dita;

import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DitaWorkspaceService implements WorkspaceService {

  private static final Logger logger = LoggerFactory.getLogger(DitaWorkspaceService.class);

  private final DitaLanguageServer server;
  private String currentRootMapUri = null;

  public DitaWorkspaceService(DitaLanguageServer server) {
    this.server = server;
  }

  @Override
  public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
    if ("dita.setRootMap".equals(params.getCommand())) {
      List<Object> arguments = params.getArguments();
      if (!arguments.isEmpty()) {
        if (arguments.get(0) instanceof JsonPrimitive json && json.isString()) {
          String rootMapUri = json.getAsString();
          server.setCurrentRootMapUri(rootMapUri);

          server
              .getClient()
              .showMessage(
                  new MessageParams(
                      MessageType.Info, "Root map set to: " + getFileName(rootMapUri)));
        }
      }
    }
    return CompletableFuture.completedFuture(null);
  }

  private String getFileName(String uri) {
    return uri.substring(uri.lastIndexOf('/') + 1);
  }

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    logger.info("Configuration changed");
  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    logger.info("Watched files changed: {} files", params.getChanges().size());
    for (FileEvent event : params.getChanges()) {
      logger.info("  - {} ({})", event.getUri(), event.getType());
    }
  }
}
