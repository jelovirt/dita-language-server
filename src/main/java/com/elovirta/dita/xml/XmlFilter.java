package com.elovirta.dita.xml;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class XmlFilter implements Iterator<XmlLexer.TokenType> {

  private final XmlLexer parent;

  private final Deque<XmlLexer.TokenType> typeBuffer = new ArrayDeque<>();
  private final Deque<char[]> textBuffer = new ArrayDeque<>();
  private final Deque<Integer> lineBuffer = new ArrayDeque<>();
  private final Deque<Integer> columnBuffer = new ArrayDeque<>();
  private final Deque<Integer> offsetBuffer = new ArrayDeque<>();

  private XmlLexer.TokenType currentType;
  private char[] currentText;
  private int currentLine;
  private int currentColumn;
  private int currentOffset;

  public XmlFilter(XmlLexer parent) {
    this.parent = parent;
  }

  public void setInput(String input) {
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
  public XmlLexer.TokenType next() {
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

    //    if (!typeBuffer.isEmpty()) {
    //      setCurrentToken(
    //          typeBuffer.removeFirst(),
    //          textBuffer.removeFirst(),
    //          lineBuffer.removeFirst(),
    //          columnBuffer.removeFirst(),
    //          offsetBuffer.removeFirst());
    //      return currentType;
    //    }

    return currentType;
  }

  public XmlLexer.TokenType getType() {
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

  private void setCurrentToken(
      XmlLexer.TokenType type, char[] text, int line, int column, int offset) {
    this.currentType = type;
    this.currentText = text;
    this.currentLine = line;
    this.currentColumn = column;
    this.currentOffset = offset;
  }

  private void filter() {
    if (currentType == XmlLexer.TokenType.EQUALS) {
      //      push();
      if (peek() == XmlLexer.TokenType.WHITESPACE) {
        popLast();
      }
    }
  }

  //  private void push() {
  //    typeBuffer.addLast(currentType);
  //    textBuffer.addLast(currentText);
  //    lineBuffer.addLast(currentLine);
  //    columnBuffer.addLast(currentColumn);
  //    offsetBuffer.addLast(currentOffset);
  //  }

  private XmlLexer.TokenType peek() {
    parent.next();
    var type = parent.getType();
    typeBuffer.addLast(type);
    textBuffer.addLast(parent.getText());
    lineBuffer.addLast(parent.getLine());
    columnBuffer.addLast(parent.getColumn());
    offsetBuffer.addLast(parent.getOffset());
    return type;
  }

  private XmlLexer.TokenType popLast() {
    var type = typeBuffer.pop();
    textBuffer.removeFirst();
    lineBuffer.removeFirst();
    columnBuffer.removeFirst();
    offsetBuffer.removeFirst();
    return type;
  }
}
