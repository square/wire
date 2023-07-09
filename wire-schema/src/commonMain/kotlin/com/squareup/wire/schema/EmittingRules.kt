/*
 * Copyright (C) 2020 Square, Inc.
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
package com.squareup.wire.schema

/**
 * A set of rules that describes which types to generate.
 *
 * Despite the builder, instances of this class are not safe for concurrent use.
 *
 * ### Identifier Matching
 *
 * Identifiers in this set may be in the following forms:
 *
 *  * Package names, followed by `.*`, like `squareup.protos.person.*`. This matches types and
 *    services defined in the package and its descendant packages.
 *
 *  * Fully qualified type and service names, like `squareup.protos.person.Person`.
 *
 * Identifiers should not contain member names.
 *
 * This set has *included identifiers* and *excluded identifiers*, with the most precise identifier
 * taking precedence over the other ones. For instance, if there is one included identifier
 * `a.Movie` along an excluded identifier `a.*`, the type `a.Movie` is considered included in the
 * set.
 *
 * If the includes set is empty, that implies that all elements should be included. Use this to
 * exclude unwanted types and members without also including everything else.
 */
class EmittingRules private constructor(builder: Builder) {
  constructor() : this(Builder())

  private val includes = builder.includes.toSet()
  private val excludes = builder.excludes.toSet()
  private val usedIncludes = mutableSetOf<String>()
  private val usedExcludes = mutableSetOf<String>()

  val isEmpty: Boolean
    get() = includes.isEmpty() && excludes.isEmpty()

  /** Returns true if `type` should be generated. */
  fun includes(type: ProtoType) = includes(type.toString())

  private fun includes(identifier: String): Boolean {
    if (includes.isEmpty()) return !exclude(identifier)

    var includeMatch: String? = null
    var excludeMatch: String? = null
    var rule: String? = identifier
    while (rule != null) {
      if (excludeMatch == null && excludes.contains(rule)) {
        excludeMatch = rule
      }
      if (includeMatch == null && includes.contains(rule)) {
        includeMatch = rule
      }
      rule = enclosing(rule)
    }

    val isIncluded = when {
      excludeMatch != null && includeMatch != null -> excludeMatch.length < includeMatch.length
      excludeMatch != null -> false
      includeMatch != null -> true
      else -> false
    }
    if (isIncluded) {
      usedIncludes.add(includeMatch!!)
    } else if (excludeMatch != null) {
      usedExcludes.add(excludeMatch)
    }
    return isIncluded
  }

  private fun exclude(identifier: String): Boolean {
    var includeMatch: String? = null
    var excludeMatch: String? = null
    var rule: String? = identifier
    while (rule != null) {
      if (excludeMatch == null && excludes.contains(rule)) {
        excludeMatch = rule
      }
      if (includeMatch == null && includes.contains(rule)) {
        includeMatch = rule
      }
      rule = enclosing(rule)
    }

    val excluded = when {
      excludeMatch != null && includeMatch != null -> excludeMatch.length >= includeMatch.length
      excludeMatch != null -> true
      else -> false
    }
    if (excluded) {
      usedExcludes.add(excludeMatch!!)
    }
    return excluded
  }

  fun unusedIncludes() = includes - usedIncludes

  fun unusedExcludes() = excludes - usedExcludes

  class Builder {
    internal val includes = mutableSetOf<String>()
    internal val excludes = mutableSetOf<String>()

    fun include(identifier: String) = apply {
      includes.add(identifier)
    }

    fun include(identifiers: Iterable<String>) = apply {
      includes.addAll(identifiers)
    }

    fun exclude(identifiers: Iterable<String>) = apply {
      excludes.addAll(identifiers)
    }

    fun exclude(identifier: String) = apply {
      excludes.add(identifier)
    }

    fun build(): EmittingRules {
      val conflictingRules = includes.intersect(excludes)
      check(conflictingRules.isEmpty()) {
        "same rule(s) defined in both includes and excludes: ${conflictingRules.joinToString()}"
      }
      return EmittingRules(this)
    }
  }

  companion object {
    /**
     * Returns the identifier or wildcard that encloses `identifier`, or null if it is not enclosed.
     *
     *  * If it is a type it returns the enclosing package with a wildcard, like
     *    `squareup.dinosaurs.*`.
     *
     *  * If it is a package with a wildcard, it returns the parent package with a wildcard, like
     *    `squareup.*`. The root wildcard is a lone asterisk, `*`.
     *
     */
    internal fun enclosing(identifier: String): String? {
      val from = if (identifier.endsWith(".*")) identifier.length - 3 else identifier.length - 1
      val dot = identifier.lastIndexOf('.', from)
      if (dot != -1) return identifier.substring(0, dot) + ".*"

      if (identifier != "*") return "*"
      return null
    }
  }
}
