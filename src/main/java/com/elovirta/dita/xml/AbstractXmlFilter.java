package com.elovirta.dita.xml;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class AbstractXmlFilter implements XmlLexer {

  private final XmlLexer parent;

  private final Deque<XmlLexerImpl.TokenType> typeBuffer = new ArrayDeque<>();
  private final Deque<char[]> textBuffer = new ArrayDeque<>();
  private final Deque<Integer> lineBuffer = new ArrayDeque<>();
  private final Deque<Integer> columnBuffer = new ArrayDeque<>();
  private final Deque<Integer> offsetBuffer = new ArrayDeque<>();

  private XmlLexerImpl.TokenType currentType;
  private char[] currentText;
  private int currentLine;
  private int currentColumn;
  private int currentOffset;

  private XmlLexerImpl.TokenType peekType;
  private char[] peekText;
  private int peekLine;
  private int peekColumn;
  private int peekOffset;

  public AbstractXmlFilter(XmlLexer parent) {
    this.parent = parent;
  }

  @Override
  public void setInput(char[] input) {
    parent.setInput(input);
  }

  @Override
  public boolean hasNext() {
    if (!typeBuffer.isEmpty()) {
      return true;
    }
    return parent.hasNext();
  }

  @Override
  public XmlLexerImpl.TokenType next() {
    if (!typeBuffer.isEmpty()) {
      setCurrentToken(
          typeBuffer.removeFirst(),
          textBuffer.removeFirst(),
          lineBuffer.removeFirst(),
          columnBuffer.removeFirst(),
          offsetBuffer.removeFirst());
      return currentType;
    }

    parent.next();
    setCurrentToken(
        parent.getType(),
        parent.getText(),
        parent.getLine(),
        parent.getColumn(),
        parent.getOffset());

    filter();

    return currentType;
  }

  @Override
  public XmlLexerImpl.TokenType getType() {
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

  private void setCurrentToken(
      XmlLexerImpl.TokenType type, char[] text, int line, int column, int offset) {
    this.currentType = type;
    this.currentText = text;
    this.currentLine = line;
    this.currentColumn = column;
    this.currentOffset = offset;
  }

  abstract void filter();

  void pushToBuffer(XmlLexerImpl.TokenType type, char[] text, int line, int column, int offset) {
    typeBuffer.addLast(type);
    textBuffer.addLast(text);
    lineBuffer.addLast(line);
    columnBuffer.addLast(column);
    offsetBuffer.addLast(offset);
  }

  void pushPeekToBuffer() {
    typeBuffer.addLast(peekType);
    textBuffer.addLast(peekText);
    lineBuffer.addLast(peekLine);
    columnBuffer.addLast(peekColumn);
    offsetBuffer.addLast(peekOffset);
    clearPeek();
  }

  void clearPeek() {
    peekType = null;
    peekText = null;
    peekLine = -1;
    peekColumn = -1;
    peekOffset = -1;
  }

  XmlLexerImpl.TokenType peek() {
    parent.next();
    var type = parent.getType();

    peekType = type;
    peekText = parent.getText();
    peekLine = parent.getLine();
    peekColumn = parent.getColumn();
    peekOffset = parent.getOffset();

    return type;
  }

  XmlLexerImpl.TokenType popLast() {
    var type = typeBuffer.pop();
    textBuffer.removeFirst();
    lineBuffer.removeFirst();
    columnBuffer.removeFirst();
    offsetBuffer.removeFirst();
    return type;
  }
}
