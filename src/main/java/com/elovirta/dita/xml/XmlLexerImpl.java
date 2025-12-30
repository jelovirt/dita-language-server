package com.elovirta.dita.xml;

import java.util.NoSuchElementException;

public class XmlLexerImpl implements XmlLexer {

  private final boolean errorCorrection;

  private enum State {
    CONTENT,
    DOCTYPE,
    COMMENT,
    XML_DECL,
    PI,
    START_ELEM,
    END_ELEM,
    ATTR_VALUE
  }

  private char[] input;
  private int pos;
  private int line;
  private int column;

  private TokenType currentType;
  private char[] currentText;
  private int currentLine;
  private int currentColumn;
  private int currentOffset;

  private boolean hasNext = true;

  private State state = State.CONTENT;
  private boolean inAttrValue = false;
  private char attrValueQuote = '\0';

  public XmlLexerImpl(boolean errorCorrection) {
    this.errorCorrection = errorCorrection;
  }

  public void setInput(char[] input) {
    this.input = input;
    this.pos = 0;
    this.line = 1;
    this.column = 1;
    this.hasNext = true;
    this.state = State.CONTENT;
    this.inAttrValue = false;
    this.attrValueQuote = '\0';
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public TokenType next() {
    if (!hasNext) {
      throw new NoSuchElementException();
    }

    TokenType type = nextToken();

    if (type == TokenType.EOF || type == TokenType.ERROR) {
      hasNext = false;
    }

    return type;
  }

  @Override
  public TokenType getType() {
    return currentType;
  }

  @Override
  public char[] getText() {
    return currentText;
  }

  @Override
  public int getLine() {
    return currentLine;
  }

  @Override
  public int getColumn() {
    return currentColumn;
  }

  @Override
  public int getOffset() {
    return currentOffset;
  }

  private TokenType nextToken() {
    if (pos >= input.length) {
      return setCurrentToken(TokenType.EOF, new char[0], line, column, pos);
    }

    char ch = peek();

    switch (state) {
      case COMMENT:
        if (ch == '-' && peek(1) == '-' && peek(2) == '>') {
          return scanCommentEnd();
        } else {
          return scanCommentBody();
        }
      case PI:
        if (ch == '?' && peek(1) == '>') {
          return scanPiEnd();
        } else if (isWhitespace(ch)) {
          return scanWhitespace();
        } else if (isNameStartChar(ch)) {
          return scanName();
        } else {
          return scanCharData();
        }
      case XML_DECL:
        if (ch == '?' && peek(1) == '>') {
          return scanXmlDeclEnd();
        } else if (isWhitespace(ch)) {
          return scanWhitespace();
        } else if (isNameStartChar(ch)) {
          return scanName();
        } else {
          return scanCharData();
        }
      case DOCTYPE:
        if (ch == '>') {
          return scanDocTypeEnd();
        } else if (inAttrValue) {
          if (ch == attrValueQuote) {
            return scanAttrValueClose();
          } else {
            return scanAttrValue();
          }
        } else if (isWhitespace(ch)) {
          return scanWhitespace();
        } else if (ch == '/' && peek(1) == '>') {
          return scanEmptyElementEnd();
        } else if (ch == '=') {
          return scanEquals();
        } else if (ch == '"' || ch == '\'') {
          return scanAttrValueOpen();
        } else if (ch == '&') {
          return scanReference();
        } else if (isNameStartChar(ch)) {
          return scanName();
        } else {
          return scanCharData();
        }
      case START_ELEM:
        if (isWhitespace(ch)) {
          return scanWhitespace();
        } else if (ch == '>') {
          return scanElementEnd();
        } else if (ch == '/' && peek(1) == '>') {
          return scanEmptyElementEnd();
        } else if (ch == '=') {
          return scanEquals();
        } else if (ch == '"' || ch == '\'') {
          state = State.ATTR_VALUE;
          return scanAttrValueOpen();
        } else if (isNameStartChar(ch)) {
          return scanName();
        } else {
          pos--;
          return scanElementEnd();
          //          throw new IllegalStateException();
        }
      case ATTR_VALUE:
        if (ch == attrValueQuote) {
          state = State.START_ELEM;
          return scanAttrValueClose();
        } else if (errorCorrection && ch == '>' && peek(1) == '<' || peek(1) == '\n') {
          return attrValueEnd();
        } else if (errorCorrection && ch == '/' && peek(1) == '>') {
          return attrValueEnd();
        } else if (errorCorrection && ch == '<') {
          return elementEnd();
        } else if (ch == '&') {
          return scanReference();
        } else {
          return scanAttrValue();
        }
      case END_ELEM:
        if (isWhitespace(ch)) {
          return scanWhitespace();
        } else if (ch == '>') {
          return scanElementEnd();
        } else if (isNameStartChar(ch)) {
          return scanName();
        } else if (ch == '<') {
          return scanElementStart();
        } else {
          throw new IllegalStateException("Unsupported character: " + ch);
        }
      case CONTENT:
        if (ch == '<') {
          if (peek(1) == '!') {
            if (peek(2) == '-' && peek(3) == '-') {
              return scanCommentStart();
            } else if (peek(2) == '[' && peekString(3, 6).equals("CDATA[")) {
              return scanCData();
            } else if (peekString(2, 7).equals("DOCTYPE")) {
              return scanDocTypeStart();
            }
          } else if (peek(1) == '?') {
            if (peekString(2, 3).equals("xml") && isWhitespaceOrEnd(5)) {
              return scanXmlDeclStart();
            } else {
              return scanPiStart();
            }
          } else if (peek(1) == '/') {
            return scanElementClose();
          } else {
            return scanElementStart();
          }
        } else if (ch == '&') {
          return scanReference();
        } else {
          return scanCharData();
        }
      default:
        throw new IllegalStateException("Unsupported character: " + ch);
    }
  }

  private TokenType scanWhitespace() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    while (pos < input.length && isWhitespace(peek())) {
      advance();
    }

    return setCurrentToken(
        TokenType.WHITESPACE, copyRange(startPos, pos), startLine, startCol, startPos);
  }

  private TokenType scanElementStart() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '<'

    state = State.START_ELEM;
    return setCurrentToken(
        TokenType.ELEMENT_START, new char[] {'<'}, startLine, startCol, startPos);
  }

  private TokenType scanElementClose() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '<'
    advance(); // consume '/'

    state = State.END_ELEM;
    return setCurrentToken(
        TokenType.ELEMENT_CLOSE, new char[] {'<', '/'}, startLine, startCol, startPos);
  }

  private TokenType scanElementEnd() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '>'

    state = State.CONTENT;
    return setCurrentToken(TokenType.ELEMENT_END, new char[] {'>'}, startLine, startCol, startPos);
  }

  private TokenType elementEnd() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    state = State.CONTENT;
    return setCurrentToken(TokenType.ELEMENT_END, new char[] {'>'}, startLine, startCol, startPos);
  }

  private TokenType scanEmptyElementEnd() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '/'
    advance(); // consume '>'

    state = State.CONTENT;
    return setCurrentToken(
        TokenType.EMPTY_ELEMENT_END, new char[] {'/', '>'}, startLine, startCol, startPos);
  }

  private TokenType scanName() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    if (!isNameStartChar(peek())) {
      return setCurrentToken(
          TokenType.ERROR,
          "Invalid name start character".toCharArray(),
          startLine,
          startCol,
          startPos);
    }

    advance();

    while (pos < input.length && isNameChar(peek())) {
      advance();
    }

    return setCurrentToken(TokenType.NAME, copyRange(startPos, pos), startLine, startCol, startPos);
  }

  private TokenType scanEquals() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '='
    return setCurrentToken(TokenType.EQUALS, new char[] {'='}, startLine, startCol, startPos);
  }

  private TokenType scanAttrValueOpen() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    char quote = peek();
    advance(); // consume quote

    inAttrValue = true;
    attrValueQuote = quote;

    return setCurrentToken(TokenType.ATTR_QUOTE, new char[] {quote}, startLine, startCol, startPos);
  }

  private TokenType scanAttrValue() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    // Scan until we hit the closing quote or error
    while (pos < input.length) {
      char ch = peek();
      if (ch == attrValueQuote) {
        break;
      } else if (ch == '>') {
        var next = peek(1);
        if (next == '\n' || next == '<') {
          break;
        }
      } else if (ch == '<') {
        var next = peek(1);
        if (isNameStartChar(next) || next == '!' || next == '?' || next == '/') {
          break;
        } else {
          return setCurrentToken(
              TokenType.ERROR,
              "< not allowed in attribute value".toCharArray(),
              startLine,
              startCol,
              startPos);
        }
      } else if (ch == '&') {
        break;
      }
      advance();
    }

    return setCurrentToken(
        TokenType.ATTR_VALUE, copyRange(startPos, pos), startLine, startCol, startPos);
  }

  private TokenType scanAttrValueClose() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    char quote = peek();
    advance(); // consume quote

    inAttrValue = false;
    attrValueQuote = '\0';

    return setCurrentToken(TokenType.ATTR_QUOTE, new char[] {quote}, startLine, startCol, startPos);
  }

  private TokenType attrValueEnd() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    char quote = attrValueQuote;

    inAttrValue = false;
    attrValueQuote = '\0';

    state = State.START_ELEM;
    return setCurrentToken(TokenType.ATTR_QUOTE, new char[] {quote}, startLine, startCol, startPos);
  }

  private TokenType scanCommentStart() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '<'
    advance(); // consume '!'
    advance(); // consume '-'
    advance(); // consume '-'

    state = State.COMMENT;
    return setCurrentToken(
        TokenType.COMMENT_START, new char[] {'<', '!', '-', '-'}, startLine, startCol, startPos);
  }

  private TokenType scanCommentBody() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    while (pos < input.length) {
      if (peek() == '-' && peek(1) == '-' && peek(2) == '>') {
        break;
      }
      advance();
    }

    return setCurrentToken(
        TokenType.COMMENT_BODY, copyRange(startPos, pos), startLine, startCol, startPos);
  }

  private TokenType scanCommentEnd() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '-'
    advance(); // consume '-'
    advance(); // consume '>'

    state = State.CONTENT;
    return setCurrentToken(
        TokenType.COMMENT_END, new char[] {'-', '-', '>'}, startLine, startCol, startPos);
  }

  private TokenType scanCData() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    // Consume '<![CDATA['
    for (int i = 0; i < 9; i++) advance();

    return setCurrentToken(
        TokenType.CDATA_START,
        new char[] {'<', '!', '[', 'C', 'D', 'A', 'T', 'A', '['},
        startLine,
        startCol,
        startPos);
  }

  private TokenType scanPiStart() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // '<'
    advance(); // '?'

    state = State.PI;
    return setCurrentToken(
        TokenType.PI_START, new char[] {'<', '?'}, startLine, startCol, startPos);
  }

  private TokenType scanPiEnd() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '?'
    advance(); // consume '>'

    state = State.CONTENT;
    return setCurrentToken(TokenType.PI_END, new char[] {'?', '>'}, startLine, startCol, startPos);
  }

  private TokenType scanXmlDeclStart() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '<'
    advance(); // consume '?'
    advance(); // consume 'x'
    advance(); // consume 'm'
    advance(); // consume 'l'

    state = State.XML_DECL;
    return setCurrentToken(
        TokenType.XML_DECL_START,
        new char[] {'<', '?', 'x', 'm', 'l'},
        startLine,
        startCol,
        startPos);
  }

  private TokenType scanXmlDeclEnd() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '?'
    advance(); // consume '>'

    state = State.CONTENT;
    return setCurrentToken(
        TokenType.XML_DECL_END, new char[] {'?', '>'}, startLine, startCol, startPos);
  }

  private TokenType scanDocTypeStart() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    // Consume '<!DOCTYPE'
    for (int i = 0; i < 9; i++) advance();

    state = State.DOCTYPE;
    return setCurrentToken(
        TokenType.DOCTYPE_START,
        new char[] {'<', '!', 'D', 'O', 'C', 'T', 'Y', 'P', 'E'},
        startLine,
        startCol,
        startPos);
  }

  private TokenType scanDocTypeEnd() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '>'

    state = State.CONTENT;
    return setCurrentToken(TokenType.DOCTYPE_END, new char[] {'>'}, startLine, startCol, startPos);
  }

  private TokenType scanReference() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '&'

    if (peek() == '#') {
      advance(); // consume '#'
      boolean hex = false;

      if (peek() == 'x') {
        hex = true;
        advance();
      }

      while (pos < input.length && (hex ? isHexDigit(peek()) : isDigit(peek()))) {
        advance();
      }

      if (peek() == ';') {
        advance();
      }

      return setCurrentToken(
          TokenType.CHAR_REF, copyRange(startPos, pos), startLine, startCol, startPos);
    } else {
      while (pos < input.length && isNameChar(peek())) {
        advance();
      }

      if (peek() == ';') {
        advance();
      }

      return setCurrentToken(
          TokenType.ENTITY_REF, copyRange(startPos, pos), startLine, startCol, startPos);
    }
  }

  private TokenType scanCharData() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    while (pos < input.length) {
      char ch = peek();
      if (ch == '<' || ch == '&') {
        break;
      }
      // Check for ']]>' which is not allowed
      if (ch == ']' && peek(1) == ']' && peek(2) == '>') {
        break;
      }
      if (ch == '?' && peek(1) == '>') {
        break;
      }
      advance();
    }

    return setCurrentToken(
        TokenType.CHAR_DATA, copyRange(startPos, pos), startLine, startCol, startPos);
  }

  // Character classification based on XML 1.1 EBNF

  private boolean isWhitespace(char ch) {
    return ch == 0x20 || ch == 0x9 || ch == 0xD || ch == 0xA;
  }

  private boolean isNameStartChar(char ch) {
    return ch == ':'
        || ch == '_'
        || (ch >= 'A' && ch <= 'Z')
        || (ch >= 'a' && ch <= 'z')
        || (ch >= 0xC0 && ch <= 0xD6)
        || (ch >= 0xD8 && ch <= 0xF6)
        || (ch >= 0xF8 && ch <= 0x2FF)
        || (ch >= 0x370 && ch <= 0x37D)
        || (ch >= 0x37F && ch <= 0x1FFF)
        || (ch >= 0x200C && ch <= 0x200D)
        || (ch >= 0x2070 && ch <= 0x218F)
        || (ch >= 0x2C00 && ch <= 0x2FEF)
        || (ch >= 0x3001 && ch <= 0xD7FF)
        || (ch >= 0xF900 && ch <= 0xFDCF)
        || (ch >= 0xFDF0 && ch <= 0xFFFD);
  }

  private boolean isNameChar(char ch) {
    return isNameStartChar(ch)
        || ch == '-'
        || ch == '.'
        || (ch >= '0' && ch <= '9')
        || ch == 0xB7
        || (ch >= 0x0300 && ch <= 0x036F)
        || (ch >= 0x203F && ch <= 0x2040);
  }

  private boolean isDigit(char ch) {
    return ch >= '0' && ch <= '9';
  }

  private boolean isHexDigit(char ch) {
    return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
  }

  private boolean isWhitespaceOrEnd(int offset) {
    if (pos + offset >= input.length) return true;
    return isWhitespace(peek(offset));
  }

  // Position tracking helpers

  private char peek() {
    return peek(0);
  }

  private char peek(int offset) {
    int index = pos + offset;
    return index < input.length ? input[index] : '\0';
  }

  private String peekString(int offset, int length) {
    int start = pos + offset;
    int end = Math.min(start + length, input.length);
    return start < input.length ? new String(input, start, end - start) : "";
  }

  private char[] copyRange(int start, int end) {
    int length = end - start;
    char[] result = new char[length];
    System.arraycopy(input, start, result, 0, length);
    return result;
  }

  private void advance() {
    if (pos < input.length) {
      char ch = input[pos];
      pos++;

      if (ch == '\n') {
        line++;
        column = 1;
      } else if (ch == '\r') {
        // Handle \r\n as single line break
        if (pos < input.length && input[pos] == '\n') {
          pos++;
        }
        line++;
        column = 1;
      } else {
        column++;
      }
    }
  }

  private TokenType setCurrentToken(TokenType type, char[] text, int line, int column, int offset) {
    this.currentType = type;
    this.currentText = text;
    this.currentLine = line;
    this.currentColumn = column;
    this.currentOffset = offset;
    return type;
  }
}
