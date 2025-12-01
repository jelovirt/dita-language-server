package com.elovirta.dita.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class XmlSerializer {

  public static final String LOC_NAMESPACE = "http://www.elovirta.com/dita/location";
  public static final String LOC_PREFIX = "loc";

  private final XmlLexer lexer;
  private Writer writer;
  private boolean isFirstElement = true;

  private static class AttributeLocation {
    String name;
    int startLine;
    int startColumn;
    int endLine;
    int endColumn;
  }

  public XmlSerializer() {
    this.lexer = new XmlFilter(new XmlLexerImpl(true));
  }

  public void serialize(String input, Writer writer) throws IOException {
    this.writer = writer;
    this.isFirstElement = true;
    lexer.setInput(input);

    while (lexer.hasNext()) {
      XmlLexerImpl.TokenType type = lexer.next();

      switch (type) {
        case XML_DECL_START:
          skipXmlDecl();
          break;
        case COMMENT_START:
          skipComment();
          break;
        case DOCTYPE_START:
          writeDocType();
          break;
        case ELEMENT_START:
          writeStartElement();
          break;
        case ELEMENT_CLOSE:
        case ELEMENT_END:
        case EMPTY_ELEMENT_END:
        case NAME:
        case EQUALS:
        case ATTR_QUOTE:
        case ATTR_VALUE:
        case CHAR_DATA:
        case WHITESPACE:
        case ENTITY_REF:
        case CHAR_REF:
        case PI_START:
        case PI_END:
        case CDATA_START:
        case CDATA_END:
          writeToken();
          break;
        case EOF:
          break;
        case ERROR:
          throw new IOException(
              "Lexer error at "
                  + lexer.getLine()
                  + ":"
                  + lexer.getColumn()
                  + " - "
                  + new String(lexer.getText()));
        default:
          // Write unknown tokens as-is
          writeToken();
          break;
      }
    }

    writer.flush();
  }

  private void writeToken() throws IOException {
    writer.write(lexer.getText());
  }

  private void skipXmlDecl() {
    // Skip until XML_DECL_END
    while (lexer.hasNext()) {
      XmlLexerImpl.TokenType type = lexer.next();
      if (type == XmlLexerImpl.TokenType.XML_DECL_END) {
        break;
      }
    }
  }

  private void skipComment() {
    // Skip until COMMENT_END
    while (lexer.hasNext()) {
      XmlLexerImpl.TokenType type = lexer.next();
      if (type == XmlLexerImpl.TokenType.COMMENT_END) {
        break;
      }
    }
  }

  private void writeDocType() throws IOException {
    // Write DOCTYPE_START
    writer.write(lexer.getText());

    // Write everything until DOCTYPE_END
    while (lexer.hasNext()) {
      XmlLexerImpl.TokenType type = lexer.next();
      writer.write(lexer.getText());

      if (type == XmlLexerImpl.TokenType.DOCTYPE_END) {
        break;
      }
    }
  }

  private void writeStartElement() throws IOException {
    // Write ELEMENT_START '<'
    writer.write(lexer.getText());

    // Next should be element name
    if (!lexer.hasNext()) {
      return;
    }

    XmlLexerImpl.TokenType type = lexer.next();
    if (type != XmlLexerImpl.TokenType.NAME) {
      writer.write(lexer.getText());
      return;
    }

    // Write element name
    writer.write(lexer.getText());

    // Add loc namespace declaration to root element
    if (isFirstElement) {
      writer.write(" xmlns:");
      writer.write(LOC_PREFIX);
      writer.write("=\"");
      writer.write(LOC_NAMESPACE);
      writer.write("\"");
      isFirstElement = false;
    }

    writer.write(" ");
    writer.write(LOC_PREFIX);
    writer.write(":elem=\"");
    writer.write(String.valueOf(lexer.getLine()));
    writer.write(":");
    writer.write(String.valueOf(lexer.getColumn()));
    writer.write("-");
    writer.write(String.valueOf(lexer.getLine()));
    writer.write(":");
    writer.write(String.valueOf(lexer.getColumn() + lexer.getText().length));
    writer.write("\"");

    // Collect attributes and their locations
    List<AttributeLocation> attrLocations = new ArrayList<>();
    StringBuilder bufferedContent = new StringBuilder();

    // Parse attributes until we hit '>' or '/>'
    while (lexer.hasNext()) {
      type = lexer.next();

      if (type == XmlLexerImpl.TokenType.ELEMENT_END
          || type == XmlLexerImpl.TokenType.EMPTY_ELEMENT_END) {
        // Write all buffered content
        writer.write(bufferedContent.toString());

        // Write location attributes
        for (AttributeLocation loc : attrLocations) {
          writer.write(" ");
          writer.write(LOC_PREFIX);
          writer.write(":attr-");
          writer.write(loc.name);
          writer.write("=\"");
          writer.write(String.valueOf(loc.startLine));
          writer.write(":");
          writer.write(String.valueOf(loc.startColumn));
          writer.write("-");
          writer.write(String.valueOf(loc.endLine));
          writer.write(":");
          writer.write(String.valueOf(loc.endColumn));
          writer.write("\"");
        }

        // Write closing '>' or '/>'
        writer.write(lexer.getText());
        break;
      } else if (type == XmlLexerImpl.TokenType.NAME) {
        // Attribute name
        String attrName = new String(lexer.getText());
        bufferedContent.append(lexer.getText());

        // Expect '=' and quotes
        boolean foundValue = false;
        int valueStartLine = 0;
        int valueStartColumn = 0;
        int valueEndLine = 0;
        int valueEndColumn = 0;

        while (lexer.hasNext()) {
          type = lexer.next();
          bufferedContent.append(lexer.getText());

          if (type == XmlLexerImpl.TokenType.ATTR_QUOTE) {
            if (!foundValue) {
              // Opening quote - next token should be the value
              if (lexer.hasNext()) {
                type = lexer.next();
                if (type == XmlLexerImpl.TokenType.ATTR_VALUE) {
                  // Record location of attribute value
                  valueStartLine = lexer.getLine();
                  valueStartColumn = lexer.getColumn();
                  bufferedContent.append(lexer.getText());
                  valueEndLine = lexer.getLine();
                  valueEndColumn = lexer.getColumn() + lexer.getText().length - 1;
                  foundValue = true;
                } else if (type == XmlLexerImpl.TokenType.ATTR_QUOTE) {
                  // Empty attribute value
                  valueStartLine = lexer.getLine();
                  valueStartColumn = lexer.getColumn();
                  valueEndLine = lexer.getLine();
                  valueEndColumn = lexer.getColumn();
                  bufferedContent.append(lexer.getText());
                  break;
                }
              }
            } else {
              // Closing quote - we're done with this attribute
              break;
            }
          } else if (type == XmlLexerImpl.TokenType.ENTITY_REF
              || type == XmlLexerImpl.TokenType.CHAR_REF) {
            // References in attribute values - update end position
            if (foundValue) {
              valueEndLine = lexer.getLine();
              valueEndColumn = lexer.getColumn() + lexer.getText().length - 1;
            }
          } else if (type == XmlLexerImpl.TokenType.EQUALS
              || type == XmlLexerImpl.TokenType.WHITESPACE) {
            // Continue
          } else {
            // Something else, break out
            break;
          }
        }

        // Store attribute location
        AttributeLocation loc = new AttributeLocation();
        loc.name = attrName;
        loc.startLine = valueStartLine;
        loc.startColumn = valueStartColumn;
        loc.endLine = valueEndLine;
        loc.endColumn = valueEndColumn;
        attrLocations.add(loc);

      } else {
        // Whitespace or other content
        bufferedContent.append(lexer.getText());
      }
    }
  }
}
