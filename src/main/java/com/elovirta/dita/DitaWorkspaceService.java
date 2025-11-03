package com.elovirta.dita;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DitaWorkspaceService implements WorkspaceService {

    private static final Logger logger = LoggerFactory.getLogger(DitaWorkspaceService.class);
    
    private final DitaLanguageServer server;

    public DitaWorkspaceService(DitaLanguageServer server) {
        this.server = server;
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