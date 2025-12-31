package com.elovirta.dita.xml;

import static javax.xml.XMLConstants.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.*;

public class XmlSerializer {

  public static final String LOC_NAMESPACE = "loc:";
  public static final String LOC_PREFIX = "loc";
  public static final String LOC_ATTR_PREFIX = "attr-";

  private final XmlLexer lexer;
  private Writer writer;
  private boolean isFirstElement = true;

  private static class AttributeLocation {
    String prefix;
    String localName;
    int startLine;
    int startColumn;
    int endLine;
    int endColumn;
  }

  public XmlSerializer() {
    this.lexer = new XmlFilter(new XmlLexerImpl(true));
  }

  public void serialize(char[] input, Writer writer) throws IOException {
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
        case ELEMENT_NAME:
        case ATTR_NAME:
        case PI_NAME:
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
    while (lexer.hasNext()) {
      XmlLexerImpl.TokenType type = lexer.next();
      if (type == XmlLexerImpl.TokenType.XML_DECL_END) {
        break;
      }
    }
  }

  private void skipComment() {
    while (lexer.hasNext()) {
      XmlLexerImpl.TokenType type = lexer.next();
      if (type == XmlLexerImpl.TokenType.COMMENT_END) {
        break;
      }
    }
  }

  private void writeDocType() throws IOException {
    writer.write(lexer.getText());

    while (lexer.hasNext()) {
      XmlLexerImpl.TokenType type = lexer.next();
      writer.write(lexer.getText());

      if (type == XmlLexerImpl.TokenType.DOCTYPE_END) {
        break;
      }
    }
  }

  private void writeStartElement() throws IOException {
    writer.write(lexer.getText());

    if (!lexer.hasNext()) {
      return;
    }

    XmlLexerImpl.TokenType type = lexer.next();
    if (type != XmlLexerImpl.TokenType.ELEMENT_NAME) {
      writer.write(lexer.getText());
      return;
    }

    writer.write(lexer.getText());

    // Add loc namespace declaration to root element
    if (isFirstElement) {
      writer.write(" ");
      writer.write(XMLNS_ATTRIBUTE);
      writer.write(":");
      writer.write(LOC_PREFIX);
      writer.write("=\"");
      writer.write(LOC_NAMESPACE);
      writer.write("\"");
      isFirstElement = false;

      writer.write(" ");
      writer.write(XMLNS_ATTRIBUTE);
      writer.write(":");
      writer.write(LOC_PREFIX);
      writer.write("-");
      writer.write(XML_NS_PREFIX);
      writer.write("=\"");
      writer.write(LOC_NAMESPACE);
      writer.write(XML_NS_URI);
      writer.write("\"");

      writer.write(" ");
      writer.write(XMLNS_ATTRIBUTE);
      writer.write(":");
      writer.write(LOC_PREFIX);
      writer.write("-");
      writer.write(XMLNS_ATTRIBUTE);
      writer.write("=\"");
      writer.write(LOC_NAMESPACE);
      writer.write(XMLNS_ATTRIBUTE_NS_URI);
      writer.write("\"");
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
    Map<String, String> namespaceDeclarations = new HashMap<>();
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
          if (loc.prefix != null) {
            writer.write("-");
            writer.write(loc.prefix);
          }
          writer.write(":attr-");
          writer.write(loc.localName);
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
        namespaceDeclarations.forEach(
            (key, value) -> {
              try {
                writer.write(" ");
                writer.write(XMLNS_ATTRIBUTE);
                writer.write(":");
                writer.write(LOC_PREFIX);
                writer.write("-");
                writer.write(key);
                writer.write("=\"");
                writer.write(value);
                writer.write("\"");
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });

        // Write closing '>' or '/>'
        writer.write(lexer.getText());
        break;
      } else if (type == XmlLexerImpl.TokenType.ATTR_NAME) {
        String attrName = new String(lexer.getText());
        bufferedContent.append(lexer.getText());

        String namespacePrefix =
            attrName.startsWith(XMLNS_ATTRIBUTE + ":") ? attrName.substring(6) : null;
        StringBuilder namespaceUri = namespacePrefix != null ? new StringBuilder() : null;

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
                  valueEndLine = lexer.getLine();
                  valueEndColumn = lexer.getColumn() + lexer.getText().length - 1;
                  bufferedContent.append(lexer.getText());
                  if (namespacePrefix != null) {
                    namespaceUri.append(lexer.getText());
                  }
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
              if (namespacePrefix != null) {
                namespaceDeclarations.put(namespacePrefix, namespaceUri.toString());
              }
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
        var index = attrName.indexOf(':');
        if (index != -1) {
          loc.prefix = attrName.substring(0, index);
          loc.localName = attrName.substring(index + 1);
        } else {
          loc.prefix = null;
          loc.localName = attrName;
        }
        if (!Objects.equals(loc.prefix, XMLNS_ATTRIBUTE)) {
          loc.startLine = valueStartLine;
          loc.startColumn = valueStartColumn;
          loc.endLine = valueEndLine;
          loc.endColumn = valueEndColumn;
          attrLocations.add(loc);
        }
      } else {
        bufferedContent.append(lexer.getText());
      }
    }
  }
}
