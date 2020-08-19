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

import java.util.Collections
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet

internal object NullAppendable : Appendable {
  override fun append(charSequence: CharSequence) = this
  override fun append(charSequence: CharSequence, start: Int, end: Int) = this
  override fun append(c: Char) = this
}

internal fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> =
    Collections.unmodifiableMap(LinkedHashMap(this))

internal fun <T> Collection<T>.toImmutableList(): List<T> =
    Collections.unmodifiableList(ArrayList(this))

internal fun <T> Collection<T>.toImmutableSet(): Set<T> =
    Collections.unmodifiableSet(LinkedHashSet(this))

internal inline fun <reified T : Enum<T>> Collection<T>.toEnumSet(): Set<T> =
    enumValues<T>().filterTo(mutableSetOf(), this::contains)

internal fun requireExactlyOneOf(modifiers: Set<Modifier>, vararg mutuallyExclusive: Modifier) {
  val count = mutuallyExclusive.count(modifiers::contains)
  require(count == 1) {
    "modifiers $modifiers must contain one of ${mutuallyExclusive.contentToString()}"
  }
}

internal fun requireNoneOrOneOf(modifiers: Set<Modifier>, vararg mutuallyExclusive: Modifier) {
  val count = mutuallyExclusive.count(modifiers::contains)
  require(count <= 1) {
    "modifiers $modifiers must contain none or only one of ${mutuallyExclusive.contentToString()}"
  }
}

internal fun requireNoneOf(modifiers: Set<Modifier>, vararg forbidden: Modifier) {
  require(forbidden.none(modifiers::contains)) {
    "modifiers $modifiers must contain none of ${forbidden.contentToString()}"
  }
}

internal fun <T> T.isOneOf(t1: T, t2: T, t3: T? = null, t4: T? = null, t5: T? = null, t6: T? = null) =
    this == t1 || this == t2 || this == t3 || this == t4 || this == t5 || this == t6

internal fun <T> Collection<T>.containsAnyOf(vararg t: T) = t.any(this::contains)

// see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
internal fun characterLiteralWithoutSingleQuotes(c: Char) = when {
  c == '\b' -> "\\b"   // \u0008: backspace (BS)
  c == '\t' -> "\\t"   // \u0009: horizontal tab (HT)
  c == '\n' -> "\\n"   // \u000a: linefeed (LF)
  c == '\r' -> "\\r"   // \u000d: carriage return (CR)
  c == '\"' -> "\""    // \u0022: double quote (")
  c == '\'' -> "\\'"   // \u0027: single quote (')
  c == '\\' -> "\\\\"  // \u005c: backslash (\)
  c.isIsoControl -> String.format("\\u%04x", c.toInt())
  else -> Character.toString(c)
}

private val Char.isIsoControl: Boolean
  get() {
    return this in '\u0000'..'\u001F' || this in '\u007F'..'\u009F'
  }

/** Returns the string literal representing `value`, including wrapping double quotes.  */
internal fun stringLiteralWithQuotes(value: String): String {
  if (value.contains("\n")) {
    val result = StringBuilder(value.length + 32)
    result.append("\"\"\"\n|")
    var i = 0
    while (i < value.length) {
      val c = value[i]
      if (value.regionMatches(i, "\"\"\"", 0, 3)) {
        // Don't inadvertently end the raw string too early
        result.append("\"\"\${'\"'}")
        i += 2
      } else if (c == '\n') {
        // Add a '|' after newlines. This pipe will be removed by trimMargin().
        result.append("\n|")
      } else {
        result.append(c)
      }
      i++
    }
    // If the last-emitted character wasn't a margin '|', add a blank line. This will get removed
    // by trimMargin().
    if (!value.endsWith("\n")) result.append("\n")
    result.append("\"\"\".trimMargin()")
    return result.toString()
  } else {
    val result = StringBuilder(value.length + 32)
    result.append('"')
    for (i in 0 until value.length) {
      val c = value[i]
      // Trivial case: single quote must not be escaped.
      if (c == '\'') {
        result.append("'")
        continue
      }
      // Trivial case: double quotes must be escaped.
      if (c == '\"') {
        result.append("\\\"")
        continue
      }
      // Default case: just let character literal do its work.
      result.append(characterLiteralWithoutSingleQuotes(c))
      // Need to append indent after linefeed?
    }
    result.append('"')
    return result.toString()
  }
}

internal fun escapeKeywords(canonicalName: String)
    = canonicalName.split('.').joinToString(".") { escapeIfKeyword(it) }

internal fun escapeIfKeyword(value: String) = if (value.isKeyword) "`$value`" else value

internal fun escapeIfNotJavaIdentifier(value: String) = if (!Character.isJavaIdentifierStart(value.first()) || value.drop(1).any { !Character.isJavaIdentifierPart(it) }) "`$value`" else value

internal fun escapeIfNecessary(value: String) = escapeIfKeyword(escapeIfNotJavaIdentifier(value))

internal val String.isIdentifier get() = IDENTIFIER_REGEX.matches(this)

internal val String.isKeyword get() = KEYWORDS.contains(this)

internal val String.isName get() = split("\\.").none { it.isKeyword }

private val IDENTIFIER_REGEX
    = ("((\\p{gc=Lu}+|\\p{gc=Ll}+|\\p{gc=Lt}+|\\p{gc=Lm}+|\\p{gc=Lo}+|\\p{gc=Nl}+)+" +
    "\\d*" +
    "\\p{gc=Lu}*\\p{gc=Ll}*\\p{gc=Lt}*\\p{gc=Lm}*\\p{gc=Lo}*\\p{gc=Nl}*)" +
    "|" +
    "(`[^\n\r`]+`)")
    .toRegex()

// https://github.com/JetBrains/kotlin/blob/master/core/descriptors/src/org/jetbrains/kotlin/renderer/KeywordStringsGenerated.java
private val KEYWORDS = setOf(
   "associatedtype",
   "class",
   "deinit",
   "enum",
   "extension",
   "fileprivate",
   "func",
   "import",
   "init",
   "inout",
   "internal",
   "let",
   "open",
   "operator",
   "private",
   "protocol",
   "public",
   "static",
   "struct",
   "subscript",
   "typealias",
   "var",

   "break",
   "case",
   "continue",
   "default",
   "defer",
   "do",
   "else",
   "fallthrough",
   "for",
   "guard",
   "if",
   "in",
   "repeat",
   "return",
   "switch",
   "where",
   "while",

   "as",
   "catch",
   "false",
   "is",
   "nil",
   "rethrows",
   "super",
   "self",
   "Self",
   "throw",
   "throws",
   "true",
   "try",

   "Type",
   "Self"
)
