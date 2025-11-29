package com.elovirta.dita.xml;

public class XmlFilter extends AbstractXmlFilter {

  public XmlFilter(XmlLexer parent) {
    super(parent);
  }

  @Override
  void filter() {
    if (getType() == XmlLexer.TokenType.EQUALS) {
      switch (peek()) {
        case WHITESPACE -> {
          clearPeek();
        }
        case NAME -> {
          pushToBuffer(XmlLexer.TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -1);
          pushPeekToBuffer();
          switch (peek()) {
            case ATTR_QUOTE -> {
              pushPeekToBuffer();
            }
            default -> {
              pushToBuffer(XmlLexer.TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -1);
              pushPeekToBuffer();
            }
          }
        }
        default -> {
          pushPeekToBuffer();
        }
      }
    } else if (getType() == XmlLexer.TokenType.ATTR_VALUE) {
      switch (peek()) {
        case ATTR_QUOTE -> {
            pushPeekToBuffer();
        }
        default -> {
          pushToBuffer(XmlLexer.TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -1);
            pushPeekToBuffer();
        }
      }
    }
  }
}
