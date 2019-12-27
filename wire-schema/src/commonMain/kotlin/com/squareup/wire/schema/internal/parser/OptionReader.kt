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
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.internal.isDigit
import com.squareup.wire.schema.internal.parser.OptionElement.Kind
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.BOOLEAN
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.ENUM
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.LIST
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.MAP
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.NUMBER
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.STRING

class OptionReader(internal val reader: SyntaxReader) {

  /**
   * Reads options enclosed in '[' and ']' if they are present and returns them. Returns an empty
   * list if no options are present.
   */
  fun readOptions(): List<OptionElement> {
    if (!reader.peekChar('[')) return emptyList()

    val result = mutableListOf<OptionElement>()
    while (true) {
      result += readOption('=')

      // Check for closing ']'
      if (reader.peekChar(']')) break

      // Discard optional ','.
      reader.expect(reader.peekChar(',')) { "Expected ',' or ']" }
    }
    return result
  }

  /** Reads a option containing a name, an '=' or ':', and a value.  */
  fun readOption(keyValueSeparator: Char): OptionElement {
    val isExtension = reader.peekChar() == '['
    val isParenthesized = reader.peekChar() == '('
    var name = reader.readName() // Option name.
    if (isExtension) name = "[$name]"

    var subName: String? = null
    var c = reader.readChar()
    if (c == '.') {
      // Read nested field name. For example "baz" in "(foo.bar).baz = 12".
      subName = reader.readName()
      c = reader.readChar()
    }
    if (keyValueSeparator == ':' && c == '{') {
      // In text format, values which are maps can omit a separator. Backtrack so it can be re-read.
      reader.pushBack('{')
    } else {
      reader.expect(c == keyValueSeparator) { "expected '$keyValueSeparator' in option" }
    }
    val kindAndValue = readKindAndValue()
    var kind = kindAndValue.kind
    var value = kindAndValue.value
    if (subName != null) {
      value = OptionElement.create(subName, kind, value)
      kind = Kind.OPTION
    }
    return OptionElement.create(name, kind, value, isParenthesized)
  }

  /** Reads a value that can be a map, list, string, number, boolean or enum.  */
  private fun readKindAndValue(): KindAndValue {
    when (val peeked = reader.peekChar()) {
      '{' -> return KindAndValue(MAP, readMap('{', '}', ':'))
      '[' -> return KindAndValue(LIST, readList())
      '"', '\'' -> return KindAndValue(STRING, reader.readString())
      else -> {
        if (peeked.isDigit() || peeked == '-') {
          return KindAndValue(NUMBER, reader.readWord())
        }
        return when (val word = reader.readWord()) {
          "true" -> KindAndValue(BOOLEAN, "true")
          "false" -> KindAndValue(BOOLEAN, "false")
          else -> KindAndValue(ENUM, word)
        }
      }
    }
  }

  /**
   * Returns a map of string keys and values. This is similar to a JSON object, with '{' and '}'
   * surrounding the map, ':' separating keys from values, and ',' separating entries.
   */
  private fun readMap(
    openBrace: Char,
    closeBrace: Char,
    keyValueSeparator: Char
  ): Map<String, Any> {
    if (reader.readChar() != openBrace) throw AssertionError()
    val result = mutableMapOf<String, Any>()
    while (true) {
      if (reader.peekChar(closeBrace)) {
        // If we see the close brace, finish immediately. This handles {}/[] and ,}/,] cases.
        return result
      }

      val option = readOption(keyValueSeparator)
      val name = option.name
      val value = option.value
      if (value is OptionElement) {
        var nested = result[name] as? MutableMap<String, Any>
        if (nested == null) {
          nested = LinkedHashMap()
          result[name] = nested
        }
        nested[value.name] = value.value
      } else {
        // Add the value(s) to any previous values with the same key
        val previous = result[name]
        when (previous) {
          null -> result[name] = value
          is List<*> -> // Add to previous List
            addToList(previous as MutableList<Any>, value)
          else -> {
            val newList = ArrayList<Any>()
            newList.add(previous)
            addToList(newList, value)
            result[name] = newList
          }
        }
      }

      // Discard optional ',' separator.
      reader.peekChar(',')
    }
  }

  /** Adds an object or objects to a List.  */
  private fun addToList(
    list: MutableList<Any>,
    value: Any
  ) {
    if (value is List<*>) {
      list.addAll(value as Collection<Any>)
    } else {
      list.add(value)
    }
  }

  /**
   * Returns a list of values. This is similar to JSON with '[' and ']' surrounding the list and ','
   * separating values.
   */
  private fun readList(): List<Any> {
    reader.require('[')
    val result = mutableListOf<Any>()
    while (true) {
      // If we see the close brace, finish immediately. This handles [] and ,] cases.
      if (reader.peekChar(']')) return result

      result.add(readKindAndValue().value)

      if (reader.peekChar(',')) continue
      reader.expect(reader.peekChar() == ']') { "expected ',' or ']'" }
    }
  }

  internal data class KindAndValue(
    internal val kind: Kind,
    internal val value: Any
  )
}
