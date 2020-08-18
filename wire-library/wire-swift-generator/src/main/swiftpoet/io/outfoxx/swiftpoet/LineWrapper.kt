/*
 * Copyright 2018 Outfox, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.outfoxx.swiftpoet

/**
 * Implements soft line wrapping on an appendable. To use, append characters using
 * [LineWrapper.append] or soft-wrapping spaces using [LineWrapper.wrappingSpace].
 */
internal class LineWrapper(
  private val out: Appendable,
  private val indent: String,
  private val columnLimit: Int
) {
  private var closed = false

  /** Characters written since the last wrapping space that haven't yet been flushed.  */
  private val buffer = StringBuilder()

  /** The number of characters since the most recent newline. Includes both out and the buffer.  */
  private var column = 0

  /** -1 if we have no buffering; otherwise the number of spaces to write after wrapping.  */
  private var indentLevel = -1

  /** Emit `s`. This may be buffered to permit line wraps to be inserted.  */
  fun append(s: String) {
    check(!closed) { "closed" }

    if (indentLevel != -1) {
      val nextNewline = s.indexOf('\n')

      // If s doesn't cause the current line to cross the limit, buffer it and return. We'll decide
      // whether or not we have to wrap it later.
      if (nextNewline == -1 && column + s.length <= columnLimit) {
        buffer.append(s)
        column += s.length
        return
      }

      // Wrap if appending s would overflow the current line.
      val wrap = nextNewline == -1 || column + nextNewline > columnLimit
      flush(wrap)
    }

    out.append(s)
    val lastNewline = s.lastIndexOf('\n')
    column = if (lastNewline != -1)
      s.length - lastNewline - 1 else
      column + s.length
  }

  /** Emit either a space or a newline character.  */
  fun wrappingSpace(indentLevel: Int) {
    check(!closed) { "closed" }

    if (this.indentLevel != -1) flush(false)
    this.column++
    this.indentLevel = indentLevel
  }

  /** Flush any outstanding text and forbid future writes to this line wrapper.  */
  fun close() {
    if (indentLevel != -1) flush(false)
    closed = true
  }

  /** Write the space followed by any buffered text that follows it.  */
  private fun flush(wrap: Boolean) {
    if (wrap) {
      out.append('\n')
      for (i in 0 until indentLevel) {
        out.append(indent)
      }
      column = indentLevel * indent.length
      column += buffer.length
    } else {
      out.append(' ')
    }
    out.append(buffer)
    buffer.delete(0, buffer.length)
    indentLevel = -1
  }
}
