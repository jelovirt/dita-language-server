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
        case ATTR_NAME -> {
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
    } else if (getType() == XmlLexerImpl.TokenType.ELEMENT_CLOSE) {
      switch (peek()) {
        case ELEMENT_NAME -> {
          pushPeekToBuffer();
          switch (peek()) {
            case ELEMENT_END -> {
              pushPeekToBuffer();
            }
            case WHITESPACE -> {
              pushPeekToBuffer();
              switch (peek()) {
                case ELEMENT_END -> {
                  pushPeekToBuffer();
                }
                default -> {
                  pushToBuffer(TokenType.ELEMENT_END, new char[] {'>'}, -1, -1, -1);
                  pushPeekToBuffer();
                }
              }
            }
            default -> {
              pushToBuffer(TokenType.ELEMENT_END, new char[] {'>'}, -1, -1, -1);
              pushPeekToBuffer();
            }
          }
        }
        default -> {
          pushPeekToBuffer();
        }
      }
    }
  }
}
