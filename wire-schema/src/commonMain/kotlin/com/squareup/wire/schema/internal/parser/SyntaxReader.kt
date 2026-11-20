/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.Location

private val HEAD_SURROGATES = 0xd800L..0xdbffL
private val TRAIL_SURROGATES = 0xdc00L..0xdfffL

/** A general purpose reader for formats like `.proto`. */
class SyntaxReader(
  private val data: CharArray,
  private val location: Location,
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
    val readChar = readChar()
    expect(readChar == c) { "expected '$c' but was '$readChar'" }
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

  /** Note that although the name suggests otherwise, this does consume the char if it finds it. */
  fun peekChar(c: Char): Boolean = when (peekChar()) {
    c -> {
      pos++
      true
    }
    else -> false
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

  /**
   * Reads a quoted string and decodes its escapes.
   *
   * Wire keeps the literal of a `bytes` field as a string with one char per byte. With [bytes] true,
   * a char that is not an escape and a `\u` or `\U` escape become the UTF-8 bytes of their code
   * point, as chars from U+0080 to U+00FF. Protoc does the same. Octal and hex escapes stay one
   * byte. With [bytes] false, the result holds the decoded text.
   */
  fun readQuotedString(bytes: Boolean = false): String {
    var startQuote = readChar()
    expect(startQuote == '"' || startQuote == '\'') { "expected quoted string" }
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
      var isByte = false
      if (c == '\\') {
        expect(pos < data.size) { "unexpected end of file" }
        c = data[pos++]
        when (c) {
          'a' -> c = '\u0007' // Alert.
          'b' -> c = '\b' // Backspace.
          'f' -> c = '\u000c' // Form feed.
          'n' -> c = '\n' // Newline.
          'r' -> c = '\r' // Carriage return.
          't' -> c = '\t' // Horizontal tab.
          'v' -> c = '\u000b' // Vertical tab.
          'x', 'X' -> {
            c = readNumericEscape(16, 2)
            isByte = true
          }
          '0', '1', '2', '3', '4', '5', '6', '7' -> {
            --pos
            c = readNumericEscape(8, 3)
            isByte = true
          }
          'u', 'U' -> {
            appendUnicodeEscape(result, c, bytes)
            continue
          }
        }
      }
      if (bytes && c.code >= 0x80 && !isByte) {
        appendCodePoint(result, readRawCodePoint(c), bytes = true)
      } else {
        result.append(c)
      }
      if (c == '\n') newline()
    }
    throw unexpected("unterminated string")
  }

  /** Returns the code point that starts with [c], which [pos] just moved past. */
  private fun readRawCodePoint(c: Char): Int {
    if (c.isHighSurrogate() && pos < data.size && data[pos].isLowSurrogate()) {
      return 0x10000 + ((c.code - 0xd800) shl 10) + (data[pos++].code - 0xdc00)
    }
    return c.code
  }

  /**
   * Appends [codePoint]. With [bytes] true, this appends its UTF-8 bytes as chars. A surrogate
   * that has no partner gets three bytes, like in protoc.
   */
  private fun appendCodePoint(result: StringBuilder, codePoint: Int, bytes: Boolean) {
    when {
      bytes -> when {
        codePoint < 0x80 -> result.append(codePoint.toChar())
        codePoint < 0x800 -> {
          result.append((0xc0 or (codePoint shr 6)).toChar())
          result.append((0x80 or (codePoint and 0x3f)).toChar())
        }
        codePoint < 0x10000 -> {
          result.append((0xe0 or (codePoint shr 12)).toChar())
          result.append((0x80 or ((codePoint shr 6) and 0x3f)).toChar())
          result.append((0x80 or (codePoint and 0x3f)).toChar())
        }
        else -> {
          result.append((0xf0 or (codePoint shr 18)).toChar())
          result.append((0x80 or ((codePoint shr 12) and 0x3f)).toChar())
          result.append((0x80 or ((codePoint shr 6) and 0x3f)).toChar())
          result.append((0x80 or (codePoint and 0x3f)).toChar())
        }
      }

      codePoint >= 0x10000 -> {
        val offset = codePoint - 0x10000
        result.append((0xd800 + (offset shr 10)).toChar())
        result.append((0xdc00 + (offset and 0x3ff)).toChar())
      }

      else -> result.append(codePoint.toChar())
    }
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

  /**
   * Reads the digits of a `\u` (4 digits) or `\U` (8 digits) escape and appends the character it
   * names. [escape] is the `u` or `U` that [pos] just moved past. This follows protoc:
   *
   *  * A head surrogate followed by a `\u` trail surrogate makes one code point. The head may come
   *    from a `\u` or a `\U` escape. A trail from a `\U` escape does not combine.
   *  * A surrogate that has no partner stays a lone surrogate.
   *  * A `\U` value above 0x10FFFF and up to 0x1FFFFF is kept as the text `\U` plus eight digits.
   *  * A `\U` value above 0x1FFFFF, and too few hex digits, are errors.
   */
  private fun appendUnicodeEscape(result: StringBuilder, escape: Char, bytes: Boolean) {
    val length = if (escape == 'u') 4 else 8
    var codePoint = readHexDigits(pos, length)
    expect(codePoint != -1L) {
      when (escape) {
        'u' -> "expected four hex digits for \\u escape sequence"
        else -> "expected eight hex digits up to 10ffff for \\U escape sequence"
      }
    }
    expect(codePoint <= 0x1fffffL) {
      "expected eight hex digits up to 10ffff for \\U escape sequence"
    }
    pos += length

    if (codePoint in HEAD_SURROGATES && pos + 1 < data.size && data[pos] == '\\' && data[pos + 1] == 'u') {
      val trail = readHexDigits(pos + 2, 4)
      if (trail in TRAIL_SURROGATES) {
        codePoint = 0x10000L + ((codePoint - HEAD_SURROGATES.first) shl 10) + (trail - TRAIL_SURROGATES.first)
        pos += 6
      }
    }

    if (codePoint > 0x10ffffL) {
      result.append("\\U")
      result.append(codePoint.toString(16).padStart(8, '0'))
    } else {
      appendCodePoint(result, codePoint.toInt(), bytes)
    }
  }

  /** Returns the value of exactly [length] hex digits at [start], or -1 if there are not enough. */
  private fun readHexDigits(start: Int, length: Int): Long {
    if (start + length > data.size) return -1L
    var value = 0L
    for (i in start until start + length) {
      val digit = hexDigit(data[i])
      if (digit == -1) return -1L
      value = value * 16 + digit
    }
    return value
  }

  private fun hexDigit(c: Char): Int = when (c) {
    in '0'..'9' -> c - '0'
    in 'a'..'f' -> c - 'a' + 10
    in 'A'..'F' -> c - 'A' + 10
    else -> -1
  }

  /**
   * Reads a (paren-wrapped), [square-wrapped] or naked symbol name.
   * If {@code retainWrap} is true and the symbol was wrapped in parens
   * or square brackets, the returned string retains the wrapping
   * punctuation. Otherwise, just the symbol is returned.
   *
   * @param allowDots whether a naked name may span '.' characters. Wrapped names always may; they
   *     are delimited by their wrapping punctuation rather than by dots.
   */
  fun readName(
    allowLeadingDigit: Boolean = true,
    retainWrap: Boolean = false,
    allowDots: Boolean = true,
  ): String = when (peekChar()) {
    '(' -> {
      pos++
      val word = readWord(allowLeadingDigit).also {
        expect(readChar() == ')') { "expected ')'" }
      }
      if (retainWrap) "($word)" else word
    }

    '[' -> {
      pos++
      val word = readWord(allowLeadingDigit).also {
        expect(readChar() == ']') { "expected ']'" }
      }
      if (retainWrap) "[$word]" else word
    }

    else -> readWord(allowLeadingDigit, allowDots)
  }

  /** Reads a scalar, map, or type name. */
  fun readDataType(): String {
    val name = readWord()
    return readDataType(name)
  }

  /** Reads a scalar, map, or type name with `name` as a prefix word. */
  fun readDataType(name: String): String = when (name) {
    "map" -> {
      expect(readChar() == '<') { "expected '<'" }
      val keyType = readDataType()

      expect(readChar() == ',') { "expected ','" }
      val valueType = readDataType()

      expect(readChar() == '>') { "expected '>'" }
      "map<$keyType, $valueType>"
    }

    else -> {
      buildString {
        append(name)
        while (peekChar() == '.') {
          pos++ // We skip the dot so we can read the next word.
          append(".${readWord()}")
        }
      }
    }
  }

  /**
   * Reads a non-empty word and returns it.
   *
   * @param allowDots true for the word to span '.' characters, as in the qualified name `foo.Bar`.
   *     When false the word ends at the first '.', leaving it for the caller to consume as a
   *     separator between the components of a path.
   */
  fun readWord(allowLeadingDigit: Boolean = true, allowDots: Boolean = true): String {
    skipWhitespace(skipComments = true)
    val start = pos
    loop@ while (pos < data.size) {
      when (data[pos]) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', '_', '-' -> pos++
        '.' -> if (allowDots) pos++ else break@loop
        else -> break@loop
      }
    }
    expect(start < pos) { "expected a word" }
    if (!allowLeadingDigit) {
      expect(!data[start].isDigit(), location().copy(column = start - lineStart)) {
        "field and constant names cannot start with a digit"
      }
    }
    return data.concatToString(start, pos)
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
    when (if (pos < data.size) data[pos++].code else -1) {
      '*'.code -> {
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

      '/'.code -> {
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
        return data.concatToString(start, pos - 1)
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
          expect(c == ' ' || c == '\t' || c == '\r') { "no syntax may follow trailing comment" }
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
    while (end > start && (data[end] == ' ' || data[end] == '\t' || data[end] == '\r')) {
      end--
    }

    if (end == start) return documentation

    val trailingDocumentation = data.concatToString(start, end + 1)
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
    location: Location? = location(),
  ): RuntimeException = throw IllegalStateException("Syntax error in $location: $message")
}
