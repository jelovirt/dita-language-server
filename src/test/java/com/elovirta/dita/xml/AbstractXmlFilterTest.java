package com.elovirta.dita.xml;

import static org.junit.jupiter.api.Assertions.*;

import com.elovirta.dita.xml.XmlLexer.TokenType;
import java.util.List;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

class AbstractXmlFilterTest {

  @Test
  void filterNothing() {
    var parent =
        new FixtureXmlLexer(
            TokenType.ELEMENT_START, TokenType.ELEMENT_NAME_START, TokenType.EMPTY_ELEMENT_END);
    var filter =
        new AbstractXmlFilter(parent) {
          @Override
          void filter() {
            // NOOP
          }
        };

    assertEquals(TokenType.ELEMENT_START, filter.next());
    assertEquals(TokenType.ELEMENT_NAME_START, filter.next());
    assertEquals(TokenType.EMPTY_ELEMENT_END, filter.next());
  }

  @Test
  void ignorePeekedToken() {
    var parent =
        new FixtureXmlLexer(
            TokenType.ELEMENT_START, TokenType.ELEMENT_NAME_START,
            TokenType.WHITESPACE, TokenType.EMPTY_ELEMENT_END);
    var filter =
        new AbstractXmlFilter(parent) {
          @Override
          void filter() {
            if (getType() == XmlLexer.TokenType.ELEMENT_NAME_START) {
              peek();
              clearPeek();
            }
          }
        };

    assertEquals(TokenType.ELEMENT_START, filter.next());
    assertEquals(TokenType.ELEMENT_NAME_START, filter.next());
    assertEquals(TokenType.EMPTY_ELEMENT_END, filter.next());
  }

  @Test
  void ignorePushToken() {
    var parent =
        new FixtureXmlLexer(
            TokenType.ELEMENT_START, TokenType.ELEMENT_NAME_START, TokenType.EMPTY_ELEMENT_END);
    var filter =
        new AbstractXmlFilter(parent) {
          @Override
          void filter() {
            if (getType() == XmlLexer.TokenType.ELEMENT_NAME_START) {
              pushToBuffer(TokenType.WHITESPACE, new char[] {' '}, -1, -1, -1);
            }
          }
        };

    assertEquals(TokenType.ELEMENT_START, filter.next());
    assertEquals(TokenType.ELEMENT_NAME_START, filter.next());
    assertEquals(TokenType.WHITESPACE, filter.next());
    assertEquals(TokenType.EMPTY_ELEMENT_END, filter.next());
  }

  @Test
  void ignoreFilterPushedToken() {
    var parent =
        new FixtureXmlLexer(
            TokenType.ELEMENT_START,
            TokenType.ELEMENT_NAME_START,
            TokenType.WHITESPACE,
            TokenType.ATTR_NAME,
            TokenType.EMPTY_ELEMENT_END);
    var filter =
        new AbstractXmlFilter(parent) {
          @Override
          void filter() {
            if (getType() == TokenType.ATTR_NAME) {
              pushToBuffer(TokenType.EQUALS, new char[] {'='}, -1, -1, -1);
            } else if (getType() == TokenType.EQUALS) {
              pushToBuffer(TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -1);
              pushToBuffer(TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -1);
            }
          }
        };

    assertEquals(TokenType.ELEMENT_START, filter.next());
    assertEquals(TokenType.ELEMENT_NAME_START, filter.next());
    assertEquals(TokenType.WHITESPACE, filter.next());
    assertEquals(TokenType.ATTR_NAME, filter.next());
    assertEquals(TokenType.EQUALS, filter.next());
    assertEquals(TokenType.ATTR_QUOTE, filter.next());
    assertEquals(TokenType.ATTR_QUOTE, filter.next());
    assertEquals(TokenType.EMPTY_ELEMENT_END, filter.next());
  }

  private static class FixtureXmlLexer implements XmlLexer {

    private final TokenType[] src;
    private int index = -1;

    FixtureXmlLexer(TokenType... src) {
      this.src = src;
    }

    @Override
    public void setInput(char[] input) {
      throw new UnsupportedOperationException();
    }

    @Override
    public TokenType getType() {
      return src[index];
    }

    @Override
    public char[] getText() {
      return src[index].name().toCharArray();
    }

    @Override
    public int getLine() {
      return index;
    }

    @Override
    public int getColumn() {
      return index;
    }

    @Override
    public int getOffset() {
      return index;
    }

    @Override
    public List<Diagnostic> getDiagnostics() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNext() {
      return index < src.length;
    }

    @Override
    public TokenType next() {
      return src[++index];
    }
  }
}
