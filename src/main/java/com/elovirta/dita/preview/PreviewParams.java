package com.elovirta.dita.preview;

import org.eclipse.lsp4j.TextDocumentIdentifier;

public class PreviewParams {
  private TextDocumentIdentifier textDocument;

  public TextDocumentIdentifier getTextDocument() {
    return textDocument;
  }

  public void setTextDocument(TextDocumentIdentifier textDocument) {
    this.textDocument = textDocument;
  }
}
