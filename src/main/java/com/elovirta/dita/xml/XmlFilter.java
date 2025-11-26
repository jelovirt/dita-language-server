package com.elovirta.dita.xml;

public class XmlFilter extends AbstractXmlFilter {

  public XmlFilter(XmlLexer parent) {
    super(parent);
  }

  @Override
  void filter() {
    if (getType() == XmlLexer.TokenType.EQUALS) {
      if (peek() == XmlLexer.TokenType.WHITESPACE) {
        popLast();
      }
    }
  }
}
