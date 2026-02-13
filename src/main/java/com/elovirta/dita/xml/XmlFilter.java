package com.elovirta.dita.xml;

import static com.elovirta.dita.xml.XmlLexer.TokenType.ATTR_VALUE;

import com.elovirta.dita.Utils;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlFilter extends AbstractXmlFilter {

  private static Logger logger = LoggerFactory.getLogger(XmlFilter.class);

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
              pushPeekToBufferAs(ATTR_VALUE);
              return;
              //              switch (peek()) {
              //                case ATTR_QUOTE -> {
              //                  pushPeekToBuffer();
              //                }
              //                default -> {
              //                  pushToBuffer(TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -2);
              //                  pushPeekToBuffer();
              //                }
              //              }
              //              return;
            }
            case ELEMENT_END, EMPTY_ELEMENT_END -> {
              pushToBuffer(TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -3);
              pushToBuffer(TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -4);
              pushPeekToBuffer();
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
            pushPeekToBuffer();
          }
        }
      }
      case ELEMENT_NAME_END -> {
        var stackName = elementStack.peek();
        if (!Arrays.equals(getText(), stackName) && Utils.startsWith(stackName, getText())) {
          logger.debug(
              "Correct end tag name from {} to {}",
              String.valueOf(getText()),
              String.valueOf(stackName));
          setText(stackName);
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
              return;
            }
          }
        }
      }
    }
  }
}
