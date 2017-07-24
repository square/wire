/*
 * Copyright (C) 2017 Square, Inc.
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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.internal.parser.OptionElement.Kind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.squareup.wire.schema.internal.parser.OptionElement.Kind.BOOLEAN;
import static com.squareup.wire.schema.internal.parser.OptionElement.Kind.ENUM;
import static com.squareup.wire.schema.internal.parser.OptionElement.Kind.LIST;
import static com.squareup.wire.schema.internal.parser.OptionElement.Kind.MAP;
import static com.squareup.wire.schema.internal.parser.OptionElement.Kind.NUMBER;
import static com.squareup.wire.schema.internal.parser.OptionElement.Kind.STRING;

public final class OptionReader {
  final SyntaxReader reader;

  public OptionReader(SyntaxReader reader) {
    this.reader = reader;
  }

  /**
   * Reads options enclosed in '[' and ']' if they are present and returns them. Returns an empty
   * list if no options are present.
   */
  public ImmutableList<OptionElement> readOptions() {
    if (!reader.peekChar('[')) return ImmutableList.of();

    ImmutableList.Builder<OptionElement> result = ImmutableList.builder();
    while (true) {
      result.add(readOption('='));

      // Check for closing ']'
      if (reader.peekChar(']')) break;

      // Discard optional ','.
      if (!reader.peekChar(',')) throw reader.unexpected("Expected ',' or ']");
    }
    return result.build();
  }

  /** Reads a option containing a name, an '=' or ':', and a value. */
  public OptionElement readOption(char keyValueSeparator) {
    boolean isExtension = (reader.peekChar() == '[');
    boolean isParenthesized = (reader.peekChar() == '(');
    String name = reader.readName(); // Option name.
    if (isExtension) {
      name = "[" + name + "]";
    }
    String subName = null;
    char c = reader.readChar();
    if (c == '.') {
      // Read nested field name. For example "baz" in "(foo.bar).baz = 12".
      subName = reader.readName();
      c = reader.readChar();
    }
    if (keyValueSeparator == ':' && c == '{') {
      // In text format, values which are maps can omit a separator. Backtrack so it can be re-read.
      reader.pushBack('{');
    } else if (c != keyValueSeparator) {
      throw reader.unexpected("expected '" + keyValueSeparator + "' in option");
    }
    KindAndValue kindAndValue = readKindAndValue();
    Kind kind = kindAndValue.kind();
    Object value = kindAndValue.value();
    if (subName != null) {
      value = OptionElement.create(subName, kind, value);
      kind = Kind.OPTION;
    }
    return OptionElement.create(name, kind, value, isParenthesized);
  }

  /** Reads a value that can be a map, list, string, number, boolean or enum. */
  private KindAndValue readKindAndValue() {
    char peeked = reader.peekChar();
    switch (peeked) {
      case '{':
        return KindAndValue.of(MAP, readMap('{', '}', ':'));
      case '[':
        return KindAndValue.of(LIST, readList());
      case '"':
      case '\'':
        return KindAndValue.of(STRING, reader.readString());
      default:
        if (Character.isDigit(peeked) || peeked == '-') {
          return KindAndValue.of(NUMBER, reader.readWord());
        }
        String word = reader.readWord();
        switch (word) {
          case "true":
            return KindAndValue.of(BOOLEAN, "true");
          case "false":
            return KindAndValue.of(BOOLEAN, "false");
          default:
            return KindAndValue.of(ENUM, word);
        }
    }
  }

  /**
   * Returns a map of string keys and values. This is similar to a JSON object, with '{' and '}'
   * surrounding the map, ':' separating keys from values, and ',' separating entries.
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> readMap(char openBrace, char closeBrace, char keyValueSeparator) {
    if (reader.readChar() != openBrace) throw new AssertionError();
    Map<String, Object> result = new LinkedHashMap<>();
    while (true) {
      if (reader.peekChar(closeBrace)) {
        // If we see the close brace, finish immediately. This handles {}/[] and ,}/,] cases.
        return result;
      }

      OptionElement option = readOption(keyValueSeparator);
      String name = option.name();
      Object value = option.value();
      if (value instanceof OptionElement) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) result.get(name);
        if (nested == null) {
          nested = new LinkedHashMap<>();
          result.put(name, nested);
        }
        OptionElement valueOption = (OptionElement) value;
        nested.put(valueOption.name(), valueOption.value());
      } else {
        // Add the value(s) to any previous values with the same key
        Object previous = result.get(name);
        if (previous == null) {
          result.put(name, value);
        } else if (previous instanceof List) {
          // Add to previous List
          addToList((List<Object>) previous, value);
        } else {
          List<Object> newList = new ArrayList<>();
          newList.add(previous);
          addToList(newList, value);
          result.put(name, newList);
        }
      }

      // Discard optional ',' separator.
      reader.peekChar(',');
    }
  }

  /** Adds an object or objects to a List. */
  private void addToList(List<Object> list, Object value) {
    if (value instanceof List) {
      list.addAll((List) value);
    } else {
      list.add(value);
    }
  }

  /**
   * Returns a list of values. This is similar to JSON with '[' and ']' surrounding the list and ','
   * separating values.
   */
  private List<Object> readList() {
    reader.require('[');
    List<Object> result = new ArrayList<>();
    while (true) {
      // If we see the close brace, finish immediately. This handles [] and ,] cases.
      if (reader.peekChar(']')) return result;

      result.add(readKindAndValue().value());

      if (reader.peekChar(',')) continue;
      if (reader.peekChar() != ']') throw reader.unexpected("expected ',' or ']'");
    }
  }

  @AutoValue
  abstract static class KindAndValue {
    static KindAndValue of(Kind kind, Object value) {
      return new AutoValue_OptionReader_KindAndValue(kind, value);
    }

    abstract Kind kind();
    abstract Object value();
  }
}
