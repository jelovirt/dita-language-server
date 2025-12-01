package com.elovirta.dita.xml;

public class XmlFilter extends AbstractXmlFilter {

  public XmlFilter(XmlLexerImpl parent) {
    super(parent);
  }

  @Override
  void filter() {
    if (getType() == XmlLexerImpl.TokenType.EQUALS) {
      switch (peek()) {
        case WHITESPACE -> {
          clearPeek();
        }
        case NAME -> {
          pushToBuffer(XmlLexerImpl.TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -1);
          pushPeekToBuffer();
          switch (peek()) {
            case ATTR_QUOTE -> {
              pushPeekToBuffer();
            }
            default -> {
              pushToBuffer(XmlLexerImpl.TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -1);
              pushPeekToBuffer();
            }
          }
        }
        default -> {
          pushPeekToBuffer();
        }
      }
    } else if (getType() == XmlLexerImpl.TokenType.ATTR_VALUE) {
      switch (peek()) {
        case ATTR_QUOTE -> {
          pushPeekToBuffer();
        }
        default -> {
          pushToBuffer(XmlLexerImpl.TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -1);
          pushPeekToBuffer();
        }
      }
    }
  }
}
