package com.elovirta.dita.xml;

import static com.elovirta.dita.xml.XmlLexer.TokenType.ATTR_VALUE;

import com.elovirta.dita.Utils;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlFilter extends AbstractXmlFilter {

  private static final Logger logger = LoggerFactory.getLogger(XmlFilter.class);

  public XmlFilter(XmlLexerImpl parent) {
    super(parent);
  }

  @Override
  void filter() {
    switch (getType()) {
      case EQUALS -> {
        while (true) {
          switch (peek()) {
            case WHITESPACE -> {
              clearPeek();
              continue;
            }
            case ATTR_NAME -> {
              pushToBuffer(TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -1);
              diagnostic("Open quote is expected for attribute", getPeekLine(), getPeekColumn());
              pushPeekToBufferAs(ATTR_VALUE);
              return;
            }
            case ELEMENT_END, EMPTY_ELEMENT_END -> {
              pushToBuffer(TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -3);
              pushToBuffer(TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -4);
              pushPeekToBuffer();
              diagnostic("Open quote is expected for attribute", getPeekLine(), getPeekColumn());
              return;
            }
            default -> {
              pushPeekToBuffer();
              return;
            }
          }
        }
      }
      case ATTR_NAME -> {
        while (true) {
          switch (peek()) {
            case WHITESPACE -> {
              clearPeek();
              continue;
            }
            case EQUALS -> {
              pushPeekToBuffer();
              return;
            }
            default -> {
              pushToBuffer(TokenType.EQUALS, new char[] {'='}, -1, -1, -5);
              pushPeekToBuffer();
              diagnostic(
                  "1 Attribute must be followed by '=' character", getPeekLine(), getPeekColumn());
              return;
            }
          }
        }
      }
      case ATTR_VALUE -> {
        switch (peek()) {
          case ATTR_QUOTE -> {
            pushPeekToBuffer();
          }
          default -> {
            pushToBuffer(TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -6);
            diagnostic("Close quote is expected for attribute", getPeekLine(), getPeekColumn());
            pushPeekToBuffer();
          }
        }
      }
      case ELEMENT_NAME_END -> {
        var stackHead = elementStack.peek();
        if (stackHead != null) {
          var stackName = stackHead.name();
          if (!Arrays.equals(getText(), stackName) && Utils.startsWith(stackName, getText())) {
            diagnostic(
                "End element name doesn't match start element name",
                stackHead.row() - 1,
                stackHead.startColumn() - 1,
                stackHead.endColumn() - 1);
            logger.debug(
                "Correct end tag name from {} to {}",
                String.valueOf(getText()),
                String.valueOf(stackName));
            setText(stackName);
          }
        }
        while (true) {
          switch (peek()) {
            case ELEMENT_END -> {
              pushPeekToBuffer();
              return;
            }
            case WHITESPACE -> {
              clearPeek();
              continue;
            }
            default -> {
              pushToBuffer(TokenType.ELEMENT_END, new char[] {'>'}, -1, -1, -7);
              pushPeekToBuffer();
              diagnostic(
                  "End element name must be followed by '>' character",
                  getPeekLine(),
                  getPeekColumn());
              return;
            }
          }
        }
      }
    }
  }
}
