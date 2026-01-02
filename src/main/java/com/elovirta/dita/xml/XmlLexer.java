package com.elovirta.dita.xml;

import java.util.Iterator;

public interface XmlLexer extends Iterator<XmlLexer.TokenType> {
  enum TokenType {
    // Structural tokens
    ELEMENT_START, // '<'
    ELEMENT_END, // '>'
    ELEMENT_CLOSE, // '</'
    EMPTY_ELEMENT_END, // '/>'

    // Names and values
    ELEMENT_NAME_START, // Element name
    ELEMENT_NAME_END, // Element name
    ATTR_NAME, // Attribute name
    PI_NAME, // PI name
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

  void setInput(char[] input);

  TokenType getType();

  char[] getText();

  int getLine();

  int getColumn();

  int getOffset();
}
