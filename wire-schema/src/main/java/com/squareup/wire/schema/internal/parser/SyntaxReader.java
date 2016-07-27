/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema.internal.parser;

import com.squareup.wire.schema.Location;

/** A general purpose reader for formats like {@code .proto}. */
public final class SyntaxReader {
  private final Location location;

  private final char[] data;
  /** Our cursor within the document. {@code data[pos]} is the next character to be read. */
  private int pos;
  /** The number of newline characters encountered thus far. */
  private int line;
  /** The index of the most recent newline character. */
  private int lineStart;

  public SyntaxReader(char[] data, Location location) {
    this.data = data;
    this.location = location;
  }

  public boolean exhausted() {
    return pos == data.length;
  }

  /** Reads a non-whitespace character and returns it. */
  public char readChar() {
    char result = peekChar();
    pos++;
    return result;
  }

  /** Reads a non-whitespace character 'c', or throws an exception. */
  public void require(char c) {
    if (readChar() != c) throw unexpected("expected '" + c + "'");
  }

  /**
   * Peeks a non-whitespace character and returns it. The only difference
   * between this and {@code readChar} is that this doesn't consume the char.
   */
  public char peekChar() {
    skipWhitespace(true);
    if (pos == data.length) throw unexpected("unexpected end of file");
    return data[pos];
  }

  public boolean peekChar(char c) {
    if (peekChar() == c) {
      pos++;
      return true;
    } else {
      return false;
    }
  }

  /** Push back the most recently read character. */
  public void pushBack(char c) {
    if (data[pos - 1] != c) throw new IllegalArgumentException();
    pos--;
  }

  /** Reads a quoted or unquoted string and returns it. */
  public String readString() {
    skipWhitespace(true);
    char c = peekChar();
    return c == '"' || c == '\'' ? readQuotedString() : readWord();
  }

  public String readQuotedString() {
    char startQuote = readChar();
    if (startQuote != '"' && startQuote != '\'') throw new AssertionError();
    StringBuilder result = new StringBuilder();
    while (pos < data.length) {
      char c = data[pos++];
      if (c == startQuote) {
        if (peekChar() == '"' || peekChar() == '\'') {
          // Adjacent strings are concatenated. Consume new quote and continue reading.
          startQuote = readChar();
          continue;
        }
        return result.toString();
      }

      if (c == '\\') {
        if (pos == data.length) throw unexpected("unexpected end of file");
        c = data[pos++];
        switch (c) {
          case 'a': c = 0x7; break;
          case 'b': c = '\b'; break;
          case 'f': c = '\f'; break;
          case 'n': c = '\n'; break;
          case 'r': c = '\r'; break;
          case 't': c = '\t'; break;
          case 'v': c = 0xb; break;
          case 'x':case 'X':
            c = readNumericEscape(16, 2);
            break;
          case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':
            --pos;
            c = readNumericEscape(8, 3);
            break;
          default:
            // use char as-is
            break;
        }
      }

      result.append(c);
      if (c == '\n') newline();
    }
    throw unexpected("unterminated string");
  }

  private char readNumericEscape(int radix, int len) {
    int value = -1;
    for (int endPos = Math.min(pos + len, data.length); pos < endPos; pos++) {
      int digit = hexDigit(data[pos]);
      if (digit == -1 || digit >= radix) break;
      if (value < 0) {
        value = digit;
      } else {
        value = value * radix + digit;
      }
    }
    if (value < 0) throw unexpected("expected a digit after \\x or \\X");
    return (char) value;
  }

  private int hexDigit(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    else if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    else if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    else return -1;
  }

  /** Reads a (paren-wrapped), [square-wrapped] or naked symbol name. */
  public String readName() {
    String optionName;
    char c = peekChar();
    if (c == '(') {
      pos++;
      optionName = readWord();
      if (readChar() != ')') throw unexpected("expected ')'");
    } else if (c == '[') {
      pos++;
      optionName = readWord();
      if (readChar() != ']') throw unexpected("expected ']'");
    } else {
      optionName = readWord();
    }
    return optionName;
  }

  /** Reads a scalar, map, or type name. */
  public String readDataType() {
    String name = readWord();
    return readDataType(name);
  }

  /** Reads a scalar, map, or type name with {@code name} as a prefix word. */
  public String readDataType(String name) {
    if (name.equals("map")) {
      if (readChar() != '<') throw unexpected("expected '<'");
      String keyType = readDataType();
      if (readChar() != ',') throw unexpected("expected ','");
      String valueType = readDataType();
      if (readChar() != '>') throw unexpected("expected '>'");
      return String.format("map<%s, %s>", keyType, valueType);
    } else {
      return name;
    }
  }

  /** Reads a non-empty word and returns it. */
  public String readWord() {
    skipWhitespace(true);
    int start = pos;
    while (pos < data.length) {
      char c = data[pos];
      if ((c >= 'a' && c <= 'z')
          || (c >= 'A' && c <= 'Z')
          || (c >= '0' && c <= '9')
          || (c == '_')
          || (c == '-')
          || (c == '.')) {
        pos++;
      } else {
        break;
      }
    }
    if (start == pos) {
      throw unexpected("expected a word");
    }
    return new String(data, start, pos - start);
  }

  /** Reads an integer and returns it. */
  public int readInt() {
    String tag = readWord();
    try {
      int radix = 10;
      if (tag.startsWith("0x") || tag.startsWith("0X")) {
        tag = tag.substring("0x".length());
        radix = 16;
      }
      return Integer.valueOf(tag, radix);
    } catch (Exception e) {
      throw unexpected("expected an integer but was " + tag);
    }
  }

  /**
   * Like {@link #skipWhitespace}, but this returns a string containing all
   * comment text. By convention, comments before a declaration document that
   * declaration.
   */
  public String readDocumentation() {
    String result = null;
    while (true) {
      skipWhitespace(false);
      if (pos == data.length || data[pos] != '/') {
        return result != null ? result : "";
      }
      String comment = readComment();
      result = (result == null) ? comment : (result + "\n" + comment);
    }
  }

  /** Reads a comment and returns its body. */
  private String readComment() {
    if (pos == data.length || data[pos] != '/') throw new AssertionError();
    pos++;
    int commentType = pos < data.length ? data[pos++] : -1;
    if (commentType == '*') {
      StringBuilder result = new StringBuilder();
      boolean startOfLine = true;

      for (; pos + 1 < data.length; pos++) {
        char c = data[pos];
        if (c == '*' && data[pos + 1] == '/') {
          pos += 2;
          return result.toString().trim();
        }
        if (c == '\n') {
          result.append('\n');
          newline();
          startOfLine = true;
        } else if (!startOfLine) {
          result.append(c);
        } else if (c == '*') {
          if (data[pos + 1] == ' ') {
            pos += 1; // Skip a single leading space, if present.
          }
          startOfLine = false;
        } else if (!Character.isWhitespace(c)) {
          result.append(c);
          startOfLine = false;
        }
      }
      throw unexpected("unterminated comment");
    } else if (commentType == '/') {
      if (pos < data.length && data[pos] == ' ') {
        pos += 1; // Skip a single leading space, if present.
      }
      int start = pos;
      while (pos < data.length) {
        char c = data[pos++];
        if (c == '\n') {
          newline();
          break;
        }
      }
      return new String(data, start, pos - 1 - start);
    } else {
      throw unexpected("unexpected '/'");
    }
  }

  public String tryAppendTrailingDocumentation(String documentation) {
    // Search for a '/' character ignoring spaces and tabs.
    while (pos < data.length) {
      char c = data[pos];
      if (c == ' ' || c == '\t') {
        pos++;
      } else if (c == '/') {
        pos++;
        break;
      } else {
        // Not a whitespace or comment-starting character. Return original documentation.
        return documentation;
      }
    }

    if (pos == data.length || (data[pos] != '/' && data[pos] != '*')) {
      pos--; // Backtrack to start of comment.
      throw unexpected("expected '//' or '/*'");
    }
    boolean isStar = data[pos] == '*';
    pos++;

    if (pos < data.length && data[pos] == ' ') {
      pos++; // Skip a single leading space, if present.
    }

    int start = pos;
    int end;

    if (isStar) {
      // Consume star comment until it closes on the same line.
      while (true) {
        if (pos == data.length || data[pos] == '\n') {
          throw unexpected("trailing comment must be closed on the same line");
        }
        if (data[pos] == '*' && pos + 1 < data.length && data[pos + 1] == '/') {
          end = pos - 1; // The character before '*'.
          pos += 2; // Skip to the character after '/'.
          break;
        }
        pos++;
      }
      // Ensure nothing follows a trailing star comment.
      while (pos < data.length) {
        char c = data[pos++];
        if (c == '\n') {
          newline();
          break;
        }
        if (c != ' ' && c != '\t') {
          throw unexpected("no syntax may follow trailing comment");
        }
      }
    } else {
      // Consume comment until newline.
      while (true) {
        if (pos == data.length) {
          end = pos - 1;
          break;
        }
        char c = data[pos++];
        if (c == '\n') {
          newline();
          end = pos - 2; // Account for stepping past the newline.
          break;
        }
      }
    }

    // Remove trailing whitespace.
    while (end > start && (data[end] == ' ' || data[end] == '\t')) {
      end--;
    }

    if (end == start) {
      return documentation;
    }

    String trailingDocumentation = new String(data, start, end - start + 1);

    return documentation.isEmpty()
        ? trailingDocumentation
        : documentation + '\n' + trailingDocumentation;
  }

  /**
   * Skips whitespace characters and optionally comments. When this returns,
   * either {@code pos == data.length} or a non-whitespace character.
   */
  private void skipWhitespace(boolean skipComments) {
    while (pos < data.length) {
      char c = data[pos];
      if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
        pos++;
        if (c == '\n') newline();
      } else if (skipComments && c == '/') {
        readComment();
      } else {
        break;
      }
    }
  }

  /** Call this every time a '\n' is encountered. */
  private void newline() {
    line++;
    lineStart = pos;
  }

  public Location location() {
    return location.at(line + 1, pos - lineStart + 1);
  }

  public RuntimeException unexpected(String message) {
    return unexpected(location(), message);
  }

  public RuntimeException unexpected(Location location, String message) {
    throw new IllegalStateException(String.format("Syntax error in %s: %s", location, message));
  }
}
