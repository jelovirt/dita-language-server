package com.elovirta.dita;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
                String rootMapUri = arguments.get(0).toString();

                server.setCurrentRootMapUri(rootMapUri);

                // Notify user
                server.getClient().showMessage(new MessageParams(
                        MessageType.Info,
                        "Root map set to: " + getFileName(rootMapUri)
                ));
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private String getFileName(String uri) {
        return uri.substring(uri.lastIndexOf('/') + 1);
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        System.err.println("Configuration changed");
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        System.err.println("Watched files changed: " + params.getChanges().size() + " files");
        for (FileEvent event : params.getChanges()) {
            System.err.println("  - " + event.getUri() + " (" + event.getType() + ")");
        }
    }
}