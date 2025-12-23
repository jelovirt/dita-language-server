package com.elovirta.dita.validator;

import java.util.*;

public class PseudoXPathParser {

  sealed interface Step permits ElementStep, TextStep, AttributeStep {}

  record ElementStep(String namespace, String localName, int position) implements Step {}

  record TextStep(int position) implements Step {}

  record AttributeStep(String namespace, String localName) implements Step {}

  private enum State {
    START,
    AFTER_SLASH,
    AFTER_AT,
    IN_Q,
    IN_NAMESPACE,
    IN_LOCAL_NAME,
    AFTER_LOCAL_NAME,
    IN_TEXT,
    IN_POSITION
  }

  public static List<Step> parse(String xpath) {
    List<Step> steps = new ArrayList<>();
    State state = State.START;

    StringBuilder namespace = new StringBuilder();
    StringBuilder localName = new StringBuilder();
    StringBuilder position = new StringBuilder();
    boolean isAttribute = false;
    boolean isElement = false;

    var chars = xpath.toCharArray();
    int i = 0;
    while (i < chars.length) {
      char ch = chars[i];

      switch (state) {
        case START:
          if (ch == '/') {
            state = State.AFTER_SLASH;
            i++;
          } else {
            throw new IllegalArgumentException("XPath must start with /");
          }
          break;

        case AFTER_SLASH:
          if (ch == '@') {
            isAttribute = true;
            isElement = false;
            state = State.AFTER_AT;
            i++;
          } else if (ch == 'Q') {
            isAttribute = false;
            isElement = true;
            state = State.IN_Q;
            i++;
          } else if (ch == 't') {
            isAttribute = false;
            isElement = false;
            state = State.IN_TEXT;
            i++;
          } else {
            throw new IllegalArgumentException("Expected Q or @ at position " + i);
          }
          break;
        case AFTER_AT:
          if (ch == 'Q') {
            state = State.IN_Q;
            i++;
          } else {
            // Local name without namespace
            namespace.setLength(0);
            localName.setLength(0);
            state = State.IN_LOCAL_NAME;
          }
          break;

        case IN_Q:
          if (ch == '{') {
            namespace.setLength(0);
            state = State.IN_NAMESPACE;
            i++;
          } else {
            throw new IllegalArgumentException("Expected { after Q at position " + i);
          }
          break;
        case IN_TEXT:
          if (ch == '[') {
            position.setLength(0);
            state = State.IN_POSITION;
            i++;
          } else {
            i++;
          }
          break;
        case IN_NAMESPACE:
          if (ch == '}') {
            localName.setLength(0);
            state = State.IN_LOCAL_NAME;
            i++;
          } else {
            namespace.append(ch);
            i++;
          }
          break;

        case IN_LOCAL_NAME:
          if (ch == '[' || ch == '/' || i == chars.length - 1) {
            // Handle last character
            if (i == chars.length - 1 && ch != '[' && ch != '/') {
              localName.append(ch);
              i++;
            }

            if (isAttribute) {
              steps.add(new AttributeStep(namespace.toString(), localName.toString()));
              if (i < chars.length) {
                throw new IllegalArgumentException("Attribute must be last step");
              }
            } else {
              state = State.AFTER_LOCAL_NAME;
            }
          } else {
            localName.append(ch);
            i++;
          }
          break;

        case AFTER_LOCAL_NAME:
          if (ch == '[') {
            position.setLength(0);
            state = State.IN_POSITION;
            i++;
          } else {
            throw new IllegalArgumentException("Expected [ at position " + i);
          }
          break;

        case IN_POSITION:
          if (ch == ']') {
            int pos = Integer.parseInt(position.toString());
            if (isElement) {
              steps.add(new ElementStep(namespace.toString(), localName.toString(), pos));
            } else {
              steps.add(new TextStep(pos));
            }
            state = State.AFTER_SLASH;
            i++;

            // Check if we're at the end or if there's another slash
            if (i < chars.length && chars[i] == '/') {
              i++; // consume the slash
            } else if (i < chars.length) {
              throw new IllegalArgumentException("Expected / or end at position " + i);
            }
          } else if (ch >= '0' && ch <= '9') {
            position.append(ch);
            i++;
          } else {
            throw new IllegalArgumentException("Expected digit or ] at position " + i);
          }
          break;
      }
    }

    return steps;
  }
}
