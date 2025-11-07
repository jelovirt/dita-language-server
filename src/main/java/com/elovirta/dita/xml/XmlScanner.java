package com.elovirta.dita.xml;

import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class XmlScanner {

  private final XmlLexer lexer;
  private ContentHandler contentHandler;

  public XmlScanner() {
    this.lexer = new XmlLexer();
  }

  public void setContentHandler(ContentHandler contentHandler) {
    this.contentHandler = contentHandler;
  }

  public void parse(String input) throws SAXException {
    lexer.setInput(input);

    if (contentHandler != null) {
      contentHandler.setDocumentLocator(new LexerLocator());
      contentHandler.startDocument();
    }

    try {
      while (lexer.hasNext()) {
        XmlLexer.TokenType type = lexer.next();

        switch (type) {
          case ELEMENT_START:
            parseStartElement();
            break;
          case ELEMENT_CLOSE:
            parseEndElement();
            break;
          case CHAR_DATA:
            parseCharData();
            break;
          case COMMENT_START:
            skipComment();
            break;
          case PI_START:
            parseProcessingInstruction();
            break;
          case XML_DECL_START:
            skipXmlDecl();
            break;
          case DOCTYPE_START:
            skipDocType();
            break;
          case CDATA_START:
            parseCData();
            break;
          case EOF:
            break;
          case ERROR:
            throw new SAXException(
                "Lexer error at "
                    + lexer.getLine()
                    + ":"
                    + lexer.getColumn()
                    + " - "
                    + new String(lexer.getText()));
          default:
            // Ignore other tokens at document level
            break;
        }
      }

      if (contentHandler != null) {
        contentHandler.endDocument();
      }
    } catch (Exception e) {
      if (e instanceof SAXException) {
        throw (SAXException) e;
      }
      throw new SAXException("Parse error", e);
    }
  }

  private void parseStartElement() throws SAXException {
    // After ELEMENT_START '<', next should be NAME
    if (!lexer.hasNext()) {
      throw new SAXException("Expected element name");
    }

    XmlLexer.TokenType type = lexer.next();
    if (type != XmlLexer.TokenType.NAME) {
      throw new SAXException("Expected element name, got " + type);
    }

    String elementName = new String(lexer.getText());
    AttributesImpl attributes = new AttributesImpl();

    // Parse attributes until we hit '>' or '/>'
    boolean isEmpty = false;
    while (lexer.hasNext()) {
      type = lexer.next();

      if (type == XmlLexer.TokenType.ELEMENT_END) {
        // End of start tag
        break;
      } else if (type == XmlLexer.TokenType.EMPTY_ELEMENT_END) {
        // Empty element
        isEmpty = true;
        break;
      } else if (type == XmlLexer.TokenType.WHITESPACE) {
        // Skip whitespace
        continue;
      } else if (type == XmlLexer.TokenType.NAME) {
        // Attribute name
        String attrName = new String(lexer.getText());

        // Expect '='
        skipWhitespace();
        if (!lexer.hasNext() || lexer.next() != XmlLexer.TokenType.EQUALS) {
          throw new SAXException("Expected '=' after attribute name");
        }

        // Expect opening quote
        skipWhitespace();
        if (!lexer.hasNext() || lexer.next() != XmlLexer.TokenType.ATTR_QUOTE) {
          throw new SAXException("Expected quote for attribute value");
        }

        // Get attribute value
        StringBuilder value = new StringBuilder();
        while (lexer.hasNext()) {
          type = lexer.next();
          if (type == XmlLexer.TokenType.ATTR_QUOTE) {
            // Closing quote
            break;
          } else if (type == XmlLexer.TokenType.ATTR_VALUE) {
            value.append(lexer.getText());
          } else if (type == XmlLexer.TokenType.ENTITY_REF || type == XmlLexer.TokenType.CHAR_REF) {
            // Expand references in attribute values
            value.append(expandReference(new String(lexer.getText())));
          }
        }

        attributes.addAttribute("", attrName, attrName, "CDATA", value.toString());
      }
    }

    if (contentHandler != null) {
      contentHandler.startElement("", elementName, elementName, attributes);

      if (isEmpty) {
        contentHandler.endElement("", elementName, elementName);
      }
    }
  }

  private void parseEndElement() throws SAXException {
    // After ELEMENT_CLOSE '</', next should be NAME
    if (!lexer.hasNext()) {
      throw new SAXException("Expected element name");
    }

    XmlLexer.TokenType type = lexer.next();
    if (type != XmlLexer.TokenType.NAME) {
      throw new SAXException("Expected element name, got " + type);
    }

    String elementName = new String(lexer.getText());

    // Skip to '>'
    while (lexer.hasNext()) {
      type = lexer.next();
      if (type == XmlLexer.TokenType.ELEMENT_END) {
        break;
      }
    }

    if (contentHandler != null) {
      contentHandler.endElement("", elementName, elementName);
    }
  }

  private void parseCharData() throws SAXException {
    char[] text = lexer.getText();

    if (contentHandler != null && text.length > 0) {
      contentHandler.characters(text, 0, text.length);
    }
  }

  private void parseCData() throws SAXException {
    // Skip CDATA_START, look for CDATA_CONTENT
    // For now, we'll scan until we find CDATA_END
    StringBuilder content = new StringBuilder();

    while (lexer.hasNext()) {
      XmlLexer.TokenType type = lexer.next();
      if (type == XmlLexer.TokenType.CDATA_END) {
        break;
      }
      // Everything between CDATA_START and CDATA_END is content
      content.append(lexer.getText());
    }

    if (contentHandler != null && content.length() > 0) {
      char[] chars = content.toString().toCharArray();
      contentHandler.characters(chars, 0, chars.length);
    }
  }

  private void parseProcessingInstruction() throws SAXException {
    // After PI_START '<?', next should be PI target
    if (!lexer.hasNext()) {
      throw new SAXException("Expected PI target");
    }

    XmlLexer.TokenType type = lexer.next();
    if (type != XmlLexer.TokenType.NAME) {
      throw new SAXException("Expected PI target");
    }

    String target = new String(lexer.getText());
    StringBuilder data = new StringBuilder();

    // Collect everything until PI_END
    while (lexer.hasNext()) {
      type = lexer.next();
      if (type == XmlLexer.TokenType.PI_END) {
        break;
      }
      data.append(lexer.getText());
    }

    if (contentHandler != null) {
      contentHandler.processingInstruction(target, data.toString().trim());
    }
  }

  private void skipComment() {
    // Skip until COMMENT_END
    while (lexer.hasNext()) {
      XmlLexer.TokenType type = lexer.next();
      if (type == XmlLexer.TokenType.COMMENT_END) {
        break;
      }
    }
  }

  private void skipXmlDecl() {
    // Skip until XML_DECL_END
    while (lexer.hasNext()) {
      XmlLexer.TokenType type = lexer.next();
      if (type == XmlLexer.TokenType.XML_DECL_END) {
        break;
      }
    }
  }

  private void skipDocType() {
    // Skip until DOCTYPE_END
    while (lexer.hasNext()) {
      XmlLexer.TokenType type = lexer.next();
      if (type == XmlLexer.TokenType.DOCTYPE_END) {
        break;
      }
    }
  }

  private void skipWhitespace() {
    while (lexer.hasNext()) {
      int currentPos = lexer.getOffset();
      XmlLexer.TokenType type = lexer.next();
      if (type != XmlLexer.TokenType.WHITESPACE) {
        // We went too far, but can't go back
        // This is a limitation - we need to handle this token
        throw new RuntimeException("Expected whitespace but got " + type);
      }
      break;
    }
  }

  private String expandReference(String ref) {
    // Basic entity expansion
    if (ref.equals("&lt;")) return "<";
    if (ref.equals("&gt;")) return ">";
    if (ref.equals("&amp;")) return "&";
    if (ref.equals("&quot;")) return "\"";
    if (ref.equals("&apos;")) return "'";

    // Character references
    if (ref.startsWith("&#")) {
      try {
        if (ref.charAt(2) == 'x') {
          // Hex reference
          int codepoint = Integer.parseInt(ref.substring(3, ref.length() - 1), 16);
          return String.valueOf((char) codepoint);
        } else {
          // Decimal reference
          int codepoint = Integer.parseInt(ref.substring(2, ref.length() - 1));
          return String.valueOf((char) codepoint);
        }
      } catch (Exception e) {
        return ref; // Return as-is if parsing fails
      }
    }

    return ref; // Unknown entity, return as-is
  }

  private class LexerLocator implements Locator {
    @Override
    public String getPublicId() {
      return null;
    }

    @Override
    public String getSystemId() {
      return null;
    }

    @Override
    public int getLineNumber() {
      return lexer.getLine();
    }

    @Override
    public int getColumnNumber() {
      return lexer.getColumn();
    }
  }
}
