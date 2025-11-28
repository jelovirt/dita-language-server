package com.elovirta.dita.xml;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class XmlLexer implements Iterator<XmlLexer.TokenType> {

  public enum TokenType {
    // Structural tokens
    ELEMENT_START, // '<'
    ELEMENT_END, // '>'
    ELEMENT_CLOSE, // '</'
    EMPTY_ELEMENT_END, // '/>'

    // Names and values
    NAME, // Element or attribute name
    EQUALS, // '='
    ATTR_QUOTE, // '"' or '\''
    ATTR_VALUE, // Attribute value (without quotes)

    // Content
    CHAR_DATA, // Text content
    CDATA_START, // '<![CDATA['
    CDATA_CONTENT, // CDATA content
    CDATA_END, // ']]>'

    // Comments and PIs
    COMMENT_START, // '<!--'
    COMMENT_BODY, // Comment content
    COMMENT_END, // '-->'
    PI_START, // '<?'
    PI_TARGET, // PI target
    PI_CONTENT, // PI content
    PI_END, // '?>'

    // XML Declaration
    XML_DECL_START, // '<?xml'
    XML_DECL_END, // '?>'

    // DOCTYPE
    DOCTYPE_START, // '<!DOCTYPE'
    DOCTYPE_END, // '>'

    // References
    ENTITY_REF, // '&' Name ';'
    CHAR_REF, // '&#' digits ';'

    // Whitespace
    WHITESPACE, // S

    // Special
    EOF,
    ERROR
  }

  private enum State {
    ROOT,
    DOCTYPE,
    COMMENT,
    XML_DECL
  }

  private char[] input;
  private int pos;
  private int line;
  private int column;

  // Current token state
  private TokenType currentType;
  private char[] currentText;
  private int currentLine;
  private int currentColumn;
  private int currentOffset;

  private boolean hasNext = true;

  // State tracking for multi-token constructs
  private State state = State.ROOT;
  private boolean inAttrValue = false;
  private char attrValueQuote = '\0';

  public void setInput(String input) {
    this.input = input.toCharArray();
    this.pos = 0;
    this.line = 1;
    this.column = 1;
    this.hasNext = true;
    this.state = State.ROOT;
    this.inAttrValue = false;
    this.attrValueQuote = '\0';
  }

  // Iterator implementation
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

  // Getters for current token state
  public TokenType getType() {
    return currentType;
  }

  public char[] getText() {
    return currentText;
  }

  public int getLine() {
    return currentLine;
  }

  public int getColumn() {
    return currentColumn;
  }

  public int getOffset() {
    return currentOffset;
  }

  private TokenType nextToken() {
    if (pos >= input.length) {
      return setCurrentToken(TokenType.EOF, new char[0], line, column, pos);
    }

    // Check if we're inside a comment
    if (state == State.COMMENT) {
      if (peek() == '-' && peek(1) == '-' && peek(2) == '>') {
        return scanCommentEnd();
      } else {
        return scanCommentBody();
      }
    }

    // Check if we're inside an attribute value
    if (inAttrValue) {
      if (peek() == attrValueQuote) {
        return scanAttrValueClose();
      } else {
        return scanAttrValue();
      }
    }

    // Check if we're inside an XML declaration and see '?>'
    if (state == State.XML_DECL && peek() == '?' && peek(1) == '>') {
      return scanXmlDeclEnd();
    }

    // Check if we're inside a DOCTYPE and see closing '>'
    if (state == State.DOCTYPE && peek() == '>') {
      return scanDocTypeEnd();
    }

    char ch = peek();

    // Handle whitespace
    if (isWhitespace(ch)) {
      return scanWhitespace();
    }

    // Handle '<' based tokens
    if (ch == '<') {
      if (peek(1) == '!') {
        if (peek(2) == '-' && peek(3) == '-') {
          return scanCommentStart();
        } else if (peek(2) == '[' && peekString(3, 6).equals("CDATA[")) {
          return scanCData();
        } else if (peekString(2, 7).equals("DOCTYPE")) {
          return scanDocTypeStart();
        }
      } else if (ch == '<' && peek(1) == '?') {
        if (peekString(2, 3).equals("xml") && isWhitespaceOrEnd(5)) {
          return scanXmlDeclStart();
        } else {
          return scanPI();
        }
      } else if (ch == '<' && peek(1) == '/') {
        return scanElementClose();
      } else {
        return scanElementStart();
      }
    }

    // Handle '>' and '/>'
    if (ch == '>') {
      return scanElementEnd();
    }

    if (ch == '/' && peek(1) == '>') {
      return scanEmptyElementEnd();
    }

    // Handle '=' for attributes
    if (ch == '=') {
      return scanEquals();
    }

    // Handle quoted strings (attribute values)
    if (ch == '"' || ch == '\'') {
      return scanAttrValueOpen();
    }

    // Handle '&' references
    if (ch == '&') {
      return scanReference();
    }

    // Handle '?' and '?>' for PI/XML decl end
    if (ch == '?' && peek(1) == '>') {
      int startPos = pos;
      int startLine = line;
      int startCol = column;
      advance();
      advance();
      return setCurrentToken(
          TokenType.PI_END, new char[] {'?', '>'}, startLine, startCol, startPos);
    }

    // Handle names (element names, attribute names)
    if (isNameStartChar(ch)) {
      return scanName();
    }

    // Handle character data
    return scanCharData();
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
    return setCurrentToken(
        TokenType.ELEMENT_START, new char[] {'<'}, startLine, startCol, startPos);
  }

  private TokenType scanElementClose() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '<'
    advance(); // consume '/'
    return setCurrentToken(
        TokenType.ELEMENT_CLOSE, new char[] {'<', '/'}, startLine, startCol, startPos);
  }

  private TokenType scanElementEnd() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '>'
    return setCurrentToken(TokenType.ELEMENT_END, new char[] {'>'}, startLine, startCol, startPos);
  }

  private TokenType scanEmptyElementEnd() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // consume '/'
    advance(); // consume '>'
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
        // Found closing quote
        break;
      } else if (ch == '<') {
        return setCurrentToken(
            TokenType.ERROR,
            "< not allowed in attribute value".toCharArray(),
            startLine,
            startCol,
            startPos);
      } else if (ch == '&') {
        // References are allowed in attribute values but we'll let them be separate tokens
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

  private TokenType scanCommentStart() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    // Consume '<!--'
    advance();
    advance();
    advance();
    advance();

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

    // Consume '-->'
    advance();
    advance();
    advance();

    state = State.ROOT;
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

  private TokenType scanPI() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    advance(); // '<'
    advance(); // '?'

    return setCurrentToken(
        TokenType.PI_START, new char[] {'<', '?'}, startLine, startCol, startPos);
  }

  private TokenType scanXmlDeclStart() {
    int startPos = pos;
    int startLine = line;
    int startCol = column;

    // Consume '<?xml'
    advance();
    advance();
    advance();
    advance();
    advance();

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

    // Consume '?>'
    advance();
    advance();

    state = State.ROOT;
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

    state = State.ROOT;
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
      // Entity reference
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
