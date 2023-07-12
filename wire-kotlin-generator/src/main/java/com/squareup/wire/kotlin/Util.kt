/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.kotlin

import java.util.Collections

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

internal fun <T> T.isOneOf(t1: T, t2: T, t3: T? = null, t4: T? = null, t5: T? = null, t6: T? = null) =
  this == t1 || this == t2 || this == t3 || this == t4 || this == t5 || this == t6

internal fun <T> Collection<T>.containsAnyOf(vararg t: T) = t.any(this::contains)

// see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
internal fun characterLiteralWithoutSingleQuotes(c: Char) = when {
  c == '\b' -> "\\b" // \u0008: backspace (BS)
  c == '\t' -> "\\t" // \u0009: horizontal tab (HT)
  c == '\n' -> "\\n" // \u000a: linefeed (LF)
  c == '\r' -> "\\r" // \u000d: carriage return (CR)
  c == '\"' -> "\"" // \u0022: double quote (")
  c == '\'' -> "\\'" // \u0027: single quote (')
  c == '\\' -> "\\\\" // \u005c: backslash (\)
  c.isIsoControl -> String.format("\\u%04x", c.code)
  else -> c.toString()
}

internal fun escapeCharacterLiterals(s: String) = buildString {
  for (c in s) append(characterLiteralWithoutSingleQuotes(c))
}

private val Char.isIsoControl: Boolean
  get() {
    return this in '\u0000'..'\u001F' || this in '\u007F'..'\u009F'
  }

/** Returns the string literal representing `value`, including wrapping double quotes.  */
internal fun stringLiteralWithQuotes(
  value: String,
  isInsideRawString: Boolean = false,
  isConstantContext: Boolean = false,
): String {
  if (!isConstantContext && '\n' in value) {
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
      } else if (c == '$' && !isInsideRawString) {
        // Escape '$' symbols with ${'$'}.
        result.append("\${\'\$\'}")
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
    // using pre-formatted strings allows us to get away with not escaping symbols that would
    // normally require escaping, e.g. "foo ${"bar"} baz"
    if (isInsideRawString) result.append("\"\"\"") else result.append('"')
    for (c in value) {
      // Trivial case: single quote must not be escaped.
      if (c == '\'') {
        result.append("'")
        continue
      }
      // Trivial case: double quotes must be escaped.
      if (c == '\"' && !isInsideRawString) {
        result.append("\\\"")
        continue
      }
      // Trivial case: $ signs must be escaped.
      if (c == '$' && !isInsideRawString) {
        result.append("\${\'\$\'}")
        continue
      }
      // Default case: just let character literal do its work.
      result.append(if (isInsideRawString) c else characterLiteralWithoutSingleQuotes(c))
      // Need to append indent after linefeed?
    }
    if (isInsideRawString) result.append("\"\"\"") else result.append('"')
    return result.toString()
  }
}

private val IDENTIFIER_REGEX =
  (
    "((\\p{gc=Lu}+|\\p{gc=Ll}+|\\p{gc=Lt}+|\\p{gc=Lm}+|\\p{gc=Lo}+|\\p{gc=Nl}+)+" +
      "\\d*" +
      "\\p{gc=Lu}*\\p{gc=Ll}*\\p{gc=Lt}*\\p{gc=Lm}*\\p{gc=Lo}*\\p{gc=Nl}*)" +
      "|" +
      "(`[^\n\r`]+`)"
    )
    .toRegex()

internal val String.isIdentifier get() = IDENTIFIER_REGEX.matches(this)

// https://kotlinlang.org/docs/reference/keyword-reference.html
private val KEYWORDS = setOf(
  // Hard keywords
  "as",
  "break",
  "class",
  "continue",
  "do",
  "else",
  "false",
  "for",
  "fun",
  "if",
  "in",
  "interface",
  "is",
  "null",
  "object",
  "package",
  "return",
  "super",
  "this",
  "throw",
  "true",
  "try",
  "typealias",
  "typeof",
  "val",
  "var",
  "when",
  "while",

  // Soft keywords
  "by",
  "catch",
  "constructor",
  "delegate",
  "dynamic",
  "field",
  "file",
  "finally",
  "get",
  "import",
  "init",
  "param",
  "property",
  "receiver",
  "set",
  "setparam",
  "where",

  // Modifier keywords
  "actual",
  "abstract",
  "annotation",
  "companion",
  "const",
  "crossinline",
  "data",
  "enum",
  "expect",
  "external",
  "final",
  "infix",
  "inline",
  "inner",
  "internal",
  "lateinit",
  "noinline",
  "open",
  "operator",
  "out",
  "override",
  "private",
  "protected",
  "public",
  "reified",
  "sealed",
  "suspend",
  "tailrec",
  "value",
  "vararg",

  // These aren't keywords anymore but still break some code if unescaped. https://youtrack.jetbrains.com/issue/KT-52315
  "header",
  "impl",

  // Other reserved keywords
  "yield",
)

private const val ALLOWED_CHARACTER = '$'

private const val UNDERSCORE_CHARACTER = '_'

internal val String.isKeyword get() = this in KEYWORDS

internal val String.hasAllowedCharacters get() = this.any { it == ALLOWED_CHARACTER }

internal val String.allCharactersAreUnderscore get() = this.all { it == UNDERSCORE_CHARACTER }

// https://github.com/JetBrains/kotlin/blob/master/compiler/frontend.java/src/org/jetbrains/kotlin/resolve/jvm/checkers/JvmSimpleNameBacktickChecker.kt
private val ILLEGAL_CHARACTERS_TO_ESCAPE = setOf('.', ';', '[', ']', '/', '<', '>', ':', '\\')

private fun String.failIfEscapeInvalid() {
  require(!any { it in ILLEGAL_CHARACTERS_TO_ESCAPE }) {
    "Can't escape identifier $this because it contains illegal characters: " +
      ILLEGAL_CHARACTERS_TO_ESCAPE.intersect(this.toSet()).joinToString("")
  }
}

internal fun String.escapeIfNecessary(validate: Boolean = true): String = escapeIfNotJavaIdentifier()
  .escapeIfKeyword()
  .escapeIfHasAllowedCharacters()
  .escapeIfAllCharactersAreUnderscore()
  .apply { if (validate) failIfEscapeInvalid() }

private fun String.alreadyEscaped() = startsWith("`") && endsWith("`")

private fun String.escapeIfKeyword() = if (isKeyword && !alreadyEscaped()) "`$this`" else this

private fun String.escapeIfHasAllowedCharacters() = if (hasAllowedCharacters && !alreadyEscaped()) "`$this`" else this

private fun String.escapeIfAllCharactersAreUnderscore() = if (allCharactersAreUnderscore && !alreadyEscaped()) "`$this`" else this

private fun String.escapeIfNotJavaIdentifier(): String {
  return if ((
      !Character.isJavaIdentifierStart(first()) ||
        drop(1).any { !Character.isJavaIdentifierPart(it) }
      ) &&
    !alreadyEscaped()
  ) {
    "`$this`".replace(' ', '·')
  } else {
    this
  }
}

internal fun String.escapeSegmentsIfNecessary(delimiter: Char = '.') = split(delimiter)
  .filter { it.isNotEmpty() }
  .joinToString(delimiter.toString()) { it.escapeIfNecessary() }
