package com.elovirta.dita.xml;

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
        switch (peek()) {
          case WHITESPACE -> {
            clearPeek();
          }
          case ATTR_NAME -> {
            pushToBuffer(TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -1);
            pushPeekToBuffer();
            switch (peek()) {
              case ATTR_QUOTE -> {
                pushPeekToBuffer();
              }
              default -> {
                pushToBuffer(TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -1);
                pushPeekToBuffer();
              }
            }
          }
          default -> {
            pushPeekToBuffer();
          }
        }
      }
      case ATTR_VALUE -> {
        switch (peek()) {
          case ATTR_QUOTE -> {
            pushPeekToBuffer();
          }
          default -> {
            pushToBuffer(TokenType.ATTR_QUOTE, new char[] {'"'}, -1, -1, -1);
            pushPeekToBuffer();
          }
        }
      }
      case ELEMENT_CLOSE -> {
        switch (peek()) {
          case ELEMENT_NAME_END -> {
            var stackName = elementStack.peek();
            if (!Arrays.equals(getPeekText(), stackName)
                && Utils.startsWith(stackName, getPeekText())) {
              logger.debug(
                  "Correct end tag name from {} to {}",
                  String.valueOf(getPeekText()),
                  String.valueOf(stackName));
              setPeekText(stackName);
            }
            pushPeekToBuffer();
            switch (peek()) {
              case ELEMENT_END -> {
                pushPeekToBuffer();
              }
              case WHITESPACE -> {
                pushPeekToBuffer();
                switch (peek()) {
                  case ELEMENT_END -> {
                    pushPeekToBuffer();
                  }
                  default -> {
                    pushToBuffer(TokenType.ELEMENT_END, new char[] {'>'}, -1, -1, -1);
                    pushPeekToBuffer();
                  }
                }
              }
              default -> {
                pushToBuffer(TokenType.ELEMENT_END, new char[] {'>'}, -1, -1, -1);
                pushPeekToBuffer();
              }
            }
          }
          default -> {
            pushPeekToBuffer();
          }
        }
      }
    }
  }
}
