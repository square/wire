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
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.Location

/** A general purpose reader for formats like `.proto`. */
class SyntaxReader(
  private val data: CharArray,
  private val location: Location
) {
  /** Our cursor within the document. `data[pos]` is the next character to be read. */
  private var pos = 0

  /** The number of newline characters encountered thus far. */
  private var line = 0

  /** The index of the most recent newline character. */
  private var lineStart = 0

  fun exhausted(): Boolean = pos == data.size

  /** Reads a non-whitespace character and returns it. */
  fun readChar(): Char = peekChar().also { pos++ }

  /** Reads a non-whitespace character 'c', or throws an exception. */
  fun require(c: Char) {
    expect(readChar() == c) { "expected '$c'" }
  }

  /**
   * Peeks a non-whitespace character and returns it. The only difference between this and
   * [readChar] is that this doesn't consume the char.
   */
  fun peekChar(): Char {
    skipWhitespace(skipComments = true)
    expect(pos < data.size) { "unexpected end of file" }
    return data[pos]
  }

  fun peekChar(c: Char): Boolean {
    return when (peekChar()) {
      c -> {
        pos++
        true
      }
      else -> false
    }
  }

  /** Push back the most recently read character. */
  fun pushBack(c: Char) {
    require(data[pos - 1] == c)
    pos--
  }

  /** Reads a quoted or unquoted string and returns it. */
  fun readString(): String {
    skipWhitespace(skipComments = true)
    return when (peekChar()) {
      '"', '\'' -> readQuotedString()
      else -> readWord()
    }
  }

  fun readQuotedString(): String {
    var startQuote = readChar()
    check(startQuote == '"' || startQuote == '\'')
    val result = StringBuilder()
    while (pos < data.size) {
      var c = data[pos++]
      if (c == startQuote) {
        // Adjacent strings are concatenated. Consume new quote and continue reading.
        if (peekChar() == '"' || peekChar() == '\'') {
          startQuote = readChar()
          continue
        }
        return result.toString()
      }
      if (c == '\\') {
        expect(pos < data.size) { "unexpected end of file" }
        c = data[pos++]
        when (c) {
          'a' -> c = '\u0007' // Alert.
          'b' -> c = '\b'     // Backspace.
          'f' -> c = '\u000c' // Form feed.
          'n' -> c = '\n'     // Newline.
          'r' -> c = '\r'     // Carriage return.
          't' -> c = '\t'     // Horizontal tab.
          'v' -> c = '\u000b' // Vertical tab.
          'x', 'X' -> c = readNumericEscape(16, 2)
          '0', '1', '2', '3', '4', '5', '6', '7' -> {
            --pos
            c = readNumericEscape(8, 3)
          }
        }
      }
      result.append(c)
      if (c == '\n') newline()
    }
    throw unexpected("unterminated string")
  }

  private fun readNumericEscape(radix: Int, len: Int): Char {
    var value = -1
    val endPos = minOf(pos + len, data.size)
    while (pos < endPos) {
      val digit = hexDigit(data[pos])
      if (digit == -1 || digit >= radix) break
      value = when {
        value < 0 -> digit
        else -> value * radix + digit
      }
      pos++
    }
    expect(value >= 0) { "expected a digit after \\x or \\X" }
    return value.toChar()
  }

  private fun hexDigit(c: Char): Int {
    return when (c) {
      in '0'..'9' -> c - '0'
      in 'a'..'f' -> c - 'a' + 10
      in 'A'..'F' -> c - 'A' + 10
      else -> -1
    }
  }

  /** Reads a (paren-wrapped), [square-wrapped] or naked symbol name. */
  fun readName(): String {
    return when (peekChar()) {
      '(' -> {
        pos++
        readWord().also {
          expect(readChar() == ')') { "expected ')'" }
        }
      }

      '[' -> {
        pos++
        readWord().also {
          expect(readChar() == ']') { "expected ']'" }
        }
      }

      else -> readWord()
    }
  }

  /** Reads a scalar, map, or type name. */
  fun readDataType(): String {
    val name = readWord()
    return readDataType(name)
  }

  /** Reads a scalar, map, or type name with `name` as a prefix word. */
  fun readDataType(name: String): String {
    return when (name) {
      "map" -> {
        expect(readChar() == '<') { "expected '<'" }
        val keyType = readDataType()

        expect(readChar() == ',') { "expected ','" }
        val valueType = readDataType()

        expect(readChar() == '>') { "expected '>'" }
        "map<$keyType, $valueType>"
      }

      else -> name
    }
  }

  /** Reads a non-empty word and returns it. */
  fun readWord(): String {
    skipWhitespace(skipComments = true)
    val start = pos
    loop@ while (pos < data.size) {
      when (data[pos]) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', '_', '-', '.' -> pos++
        else -> break@loop
      }
    }
    expect(start < pos) { "expected a word" }
    return String(data, start, pos - start)
  }

  /** Reads an integer and returns it. */
  fun readInt(): Int {
    var tag = readWord()
    try {
      var radix = 10
      if (tag.startsWith("0x") || tag.startsWith("0X")) {
        tag = tag.substring("0x".length)
        radix = 16
      }
      return tag.toInt(radix)
    } catch (_: Exception) {
      throw unexpected("expected an integer but was $tag")
    }
  }

  /**
   * Like [skipWhitespace], but this returns a string containing all comment text. By convention,
   * comments before a declaration document that declaration.
   */
  fun readDocumentation(): String {
    var result: String? = null
    while (true) {
      skipWhitespace(skipComments = false)
      if (pos == data.size || data[pos] != '/') return result ?: ""
      val comment = readComment()
      result = when (result) {
        null -> comment
        else -> "$result\n$comment"
      }
    }
  }

  /** Reads a comment and returns its body. */
  private fun readComment(): String {
    check(pos != data.size && data[pos] == '/')
    pos++
    when (if (pos < data.size) data[pos++].toInt() else -1) {
      '*'.toInt() -> {
        val result = StringBuilder()
        var startOfLine = true
        while (pos + 1 < data.size) {
          val c = data[pos]
          when {
            c == '*' && data[pos + 1] == '/' -> {
              pos += 2
              return result.toString().trim()
            }

            c == '\n' -> {
              result.append('\n')
              newline()
              startOfLine = true
            }

            !startOfLine -> {
              result.append(c)
            }

            c == '*' -> {
              if (data[pos + 1] == ' ') {
                pos += 1 // Skip a single leading space, if present.
              }
              startOfLine = false
            }

            !c.isWhitespace() -> {
              result.append(c)
              startOfLine = false
            }
          }
          pos++
        }
        throw unexpected("unterminated comment")
      }

      '/'.toInt() -> {
        if (pos < data.size && data[pos] == ' ') {
          pos++ // Skip a single leading space, if present.
        }
        val start = pos
        while (pos < data.size) {
          val c = data[pos++]
          if (c == '\n') {
            newline()
            break
          }
        }
        return String(data, start, pos - 1 - start)
      }

      else -> throw unexpected("unexpected '/'")
    }
  }

  fun tryAppendTrailingDocumentation(documentation: String): String {
    // Search for a '/' character ignoring spaces and tabs.
    loop@ while (pos < data.size) {
      when (data[pos]) {
        ' ', '\t' -> pos++

        '/' -> {
          pos++
          break@loop
        }

        // Not a whitespace or comment-starting character. Return original documentation.
        else -> return documentation
      }
    }

    expect(pos < data.size && (data[pos] == '/' || data[pos] == '*')) {
      pos-- // Backtrack to start of comment.
      "expected '//' or '/*'"
    }

    val isStar = data[pos] == '*'
    pos++

    // Skip a single leading space, if present.
    if (pos < data.size && data[pos] == ' ') pos++

    val start = pos
    var end: Int
    when {
      isStar -> {
        // Consume star comment until it closes on the same line.
        while (true) {
          expect(pos < data.size) { "trailing comment must be closed" }
          if (data[pos] == '*' && pos + 1 < data.size && data[pos + 1] == '/') {
            end = pos - 1 // The character before '*'.
            pos += 2 // Skip to the character after '/'.
            break
          }
          pos++
        }

        // Ensure nothing follows a trailing star comment.
        while (pos < data.size) {
          val c = data[pos++]
          if (c == '\n') {
            newline()
            break
          }
          expect(c == ' ' || c == '\t') { "no syntax may follow trailing comment" }
        }
      }

      else -> {
        // Consume comment until newline.
        while (true) {
          if (pos == data.size) {
            end = pos - 1
            break
          }
          val c = data[pos++]
          if (c == '\n') {
            newline()
            end = pos - 2 // Account for stepping past the newline.
            break
          }
        }
      }
    }

    // Remove trailing whitespace.
    while (end > start && (data[end] == ' ' || data[end] == '\t')) {
      end--
    }

    if (end == start) return documentation

    val trailingDocumentation = String(data, start, end - start + 1)
    if (documentation.isEmpty()) return trailingDocumentation
    return "$documentation\n$trailingDocumentation"
  }

  /**
   * Skips whitespace characters and optionally comments. When this returns, either
   * `pos == data.length` or a non-whitespace character.
   */
  private fun skipWhitespace(skipComments: Boolean) {
    while (pos < data.size) {
      val c = data[pos]
      when {
        c == ' ' || c == '\t' || c == '\r' || c == '\n' -> {
          pos++
          if (c == '\n') newline()
        }

        skipComments && c == '/' -> readComment()

        else -> return
      }
    }
  }

  /** Call this every time a '\n' is encountered. */
  private fun newline() {
    line++
    lineStart = pos
  }

  fun location() = location.at(line + 1, pos - lineStart + 1)

  inline fun expect(condition: Boolean, location: Location = location(), message: () -> String) {
    if (!condition) throw unexpected(message(), location)
  }

  fun unexpected(
    message: String,
    location: Location? = location()
  ): RuntimeException = throw IllegalStateException("Syntax error in $location: $message")
}
