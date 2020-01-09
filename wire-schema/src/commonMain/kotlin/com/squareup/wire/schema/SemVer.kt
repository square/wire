/*
 * Copyright (C) 2019 Square, Inc.
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
package com.squareup.wire.schema

import com.squareup.wire.schema.internal.isDigit
import com.squareup.wire.schema.internal.toEnglishLowerCase

/**
 * A version string as specified by [semver.org][semver_org]. This is used to order versions for
 * sorting since and until.
 *
 * If there is a `+` in the version string it delimits the build metadata. The plus character and
 * the section that follows are unused for version comparisons.
 *
 * The rest of the string is split into two sections:
 *
 *  * Release (preceding the first dash). Typically like "1.0.0".
 *
 *  * Pre-release (optional, following the first dash). Typically like "alpha", "snapshot", or
 *    "rc1".
 *
 * Each section has one or more segments separated by dots and dashes. Within a segment comparisons
 * are either numeric (if both segments are strictly digits), lexicographic (if both contain
 * non-digit characters) or numbers-first (if only one segment is strictly digits).
 *
 * This class requires lowercase versions to defend against potentially surprising behavior in the
 * semver.org spec ("1.0-alpha" comes after "1.0-BETA" due to case-sensitive sorting).
 *
 * [semver_org]: https://semver.org/
 */
internal data class SemVer(val version: String) : Comparable<SemVer> {

  init {
    require(version.toEnglishLowerCase() == version) { "version must be lowercase: $version" }
  }

  override fun toString() = version

  override fun compareTo(other: SemVer): Int {
    val a = version
    val b = other.version
    var aPos = 0
    var bPos = 0

    for (terminators in listOf(RELEASE_TERMINATORS, PRERELEASE_TERMINATORS)) {
      val aSize = a.find(terminators, startIndex = aPos)
      val bSize = b.find(terminators, startIndex = bPos)

      // A version that lacks a pre-release section comes after a version that has one.
      if (terminators === PRERELEASE_TERMINATORS) {
        val aNoPrerelease = aPos == aSize
        val bNoPrerelease = bPos == bSize
        if (aNoPrerelease && !bNoPrerelease) return 1
        if (bNoPrerelease && !aNoPrerelease) return -1
      }

      while (aPos < aSize && bPos < bSize) {
        val aLimit = a.find(SEPARATORS, startIndex = aPos, endIndex = aSize)
        val bLimit = b.find(SEPARATORS, startIndex = bPos, endIndex = bSize)

        val result = compareSegment(
            a.substring(aPos, aLimit),
            b.substring(bPos, bLimit)
        )

        if (result != 0) return result

        // The next position starts just past the delimiter.
        aPos = if (aLimit < aSize) aLimit + 1 else aSize
        bPos = if (bLimit < bSize) bLimit + 1 else bSize
      }

      // If there are more segments in this section, it's later.
      if (aPos < aSize) return 1
      if (bPos < bSize) return -1
    }

    return 0
  }

  private fun compareSegment(a: String, b: String): Int {
    val aAllDigits = a.all(Char::isDigit)
    val bAllDigits = b.all(Char::isDigit)
    return when {
      aAllDigits && bAllDigits -> compareNumber(a, b)
      aAllDigits -> -1
      bAllDigits -> 1
      else -> a.compareTo(b)
    }
  }

  /** Compare two strings as non-negative integral values without decoding. */
  private fun compareNumber(a: String, b: String): Int {
    val aStart = a.indexOfFirst { it != '0' }
    val bStart = b.indexOfFirst { it != '0' }
    return when {
      aStart == -1 && bStart == -1 -> 0 // a and b are 0
      aStart == -1 -> -1 // a is 0, b is not
      bStart == -1 -> 1 // b is 0, a is not
      else -> {
        val aLength = a.length - aStart
        val bLength = b.length - bStart
        when {
          aLength < bLength -> -1 // a is shorter than b, thus smaller
          aLength > bLength -> 1 // b is shorter than a, thus smaller
          else -> a.substring(aStart).compareTo(b.substring(bStart)) // compare lexicographically
        }
      }
    }
  }

  /**
   * Returns the index of a char in [chars] in `[startIndex..endIndex)`. Returns [endIndex] if the
   * value is not found.
   */
  private fun String.find(
    chars: CharArray,
    startIndex: Int = 0,
    endIndex: Int = length
  ): Int {
    val result = indexOfAny(chars, startIndex = startIndex)
    if (result != -1 && result < endIndex) return result
    return endIndex
  }

  companion object {
    /** Characters that signal the end of the release section. */
    private val RELEASE_TERMINATORS = charArrayOf('+', '-')
    /** Characters that signal the end of the pre-release section. */
    private val PRERELEASE_TERMINATORS = charArrayOf('+')
    /** Characters that separate segments within a section. */
    private val SEPARATORS = charArrayOf('.', '-')

    fun String.toLowerCaseSemVer() = SemVer(toEnglishLowerCase())
  }
}
