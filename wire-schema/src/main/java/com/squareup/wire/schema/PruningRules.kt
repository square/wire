/*
 * Copyright (C) 2015 Square, Inc.
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

import com.squareup.wire.schema.SemVer.Companion.toLowerCaseSemVer

/**
 * A set of rules that describes which types and members to retain and which to remove.
 *
 * Members may be pruned using either their identifier (package, type name, member name) or their
 * version (since and until options).
 *
 * Despite the builder, instances of this class are not safe for concurrent use.
 *
 * ### Identifier Matching
 *
 * If a member is included in the set, its type is implicitly also included. A type that is included
 * without a specific member implicitly includes all of that type's members, but not its nested
 * types.
 *
 * Identifiers in this set may be in the following forms:
 *
 *  * Package names, followed by `.*`, like `squareup.protos.person.*`. This matches types and
 *    services defined in the package and its descendant packages.
 *
 *  * Fully qualified type and service names, like `squareup.protos.person.Person`.
 *
 *  * Fully qualified member names, which are type names followed by a '#', followed by a member
 *    name, like `squareup.protos.person.Person#address`. Members may be fields, enum constants or
 *    RPCs.
 *
 * An identifier set populated with `Movie` and `Actor#name` contains all members of `Movie` (such
 * as `Movie#name` and `Movie#release_date`). It contains the type `Actor` and one member
 * `Actor#name`, but not `Actor#birth_date` or `Actor#oscar_count`.
 *
 * This set has *included identifiers* and *excluded identifiers*, with excludes taking precedence
 * over includes. That is, if a type `Movie` is in both the includes and the excludes, it is not
 * contained in the set.
 *
 * If the includes set is empty, that implies that all elements should be included. Use this to
 * exclude unwanted types and members without also including everything else.
 *
 * ### Version Matching
 *
 * Members may be declared with `wire.since` and `wire.until` options. For example, these options
 * declare a field `age` that was replaced with `birth_date` in version "5.0":
 *
 * ```
 *   optional int32 age = 3 [(wire.until) = "5.0"];
 *   optional Date birth_date = 4 [(wire.since) = "5.0"];
 * ```
 *
 * Client code should typically target a single version. In this example, versions <= "4.0" will
 * have the `age` field only and versions >= "5.0" will have the `birth_date` field only.
 *
 * Service code that supports many clients should support the union of versions of all supported
 * clients. Such code will have both the `age` and `birth_date` fields.
 */
class PruningRules private constructor(builder: Builder) {
  private val includes = builder.includes.toSet()
  private val excludes = builder.excludes.toSet()
  private val oldest = builder.oldest
  private val newest = builder.newest
  private val usedIncludes = mutableSetOf<String>()
  private val usedExcludes = mutableSetOf<String>()

  val isEmpty: Boolean
    get() = includes.isEmpty() && excludes.isEmpty() && oldest == null && newest == null

  /** Returns true unless [options] specifies a version that is outside of the configured range. */
  fun isFieldRetainedVersion(options: Options) =
      isRetainedVersion(options, FIELD_SINCE, FIELD_UNTIL)

  /** Returns true unless [options] specifies a version that is outside of the configured range. */
  fun isEnumConstantRetainedVersion(options: Options) =
      isRetainedVersion(options, ENUM_CONSTANT_SINCE, ENUM_CONSTANT_UNTIL)

  private fun isRetainedVersion(
    options: Options,
    sinceMember: ProtoMember,
    untilMember: ProtoMember
  ): Boolean {
    if (newest != null) {
      val sinceOption = options[sinceMember]
      val since = (sinceOption as? String)?.toLowerCaseSemVer()
      if (since != null && since > newest) return false
    }

    if (oldest != null) {
      val untilOption = options[untilMember]
      val until = (untilOption as? String)?.toLowerCaseSemVer()
      if (until != null && until <= oldest) return false
    }

    return true
  }

  /** Returns true if `type` is a root. */
  fun includes(type: ProtoType) = includes(type.toString())

  /** Returns true if `protoMember` is a root. */
  fun includes(protoMember: ProtoMember) = includes(protoMember.toString())

  /**
   * Returns true if `identifier` or any of its enclosing identifiers is included. If any enclosing
   * identifier is excluded, that takes precedence and this returns false.
   */
  private fun includes(identifier: String): Boolean {
    if (includes.isEmpty()) return !exclude(identifier)

    var includeMatch: String? = null
    var excludeMatch: String? = null
    var rule: String? = identifier
    while (rule != null) {
      if (excludes.contains(rule)) {
        excludeMatch = rule
      }
      if (includes.contains(rule)) {
        includeMatch = rule
      }
      rule = enclosing(rule)
    }

    return when {
      excludeMatch != null -> {
        usedExcludes.add(excludeMatch)
        false
      }
      includeMatch != null -> {
        usedIncludes.add(includeMatch)
        true
      }
      else -> {
        false
      }
    }
  }

  /**
   * Returns true if `type` should be excluded, even if it is a transitive dependency of a root. In
   * that case, the referring member is also excluded.
   */
  fun excludes(type: ProtoType) = exclude(type.toString())

  /** Returns true if `protoMember` should be excluded. */
  fun excludes(protoMember: ProtoMember) = exclude(protoMember.toString())

  /** Returns true if `identifier` or any of its enclosing identifiers is excluded.  */
  private fun exclude(identifier: String): Boolean {
    var excludeMatch: String? = null
    var rule: String? = identifier
    while (rule != null) {
      if (excludes.contains(rule)) {
        excludeMatch = rule
      }
      rule = enclosing(rule)
    }

    return when {
      excludeMatch != null -> {
        usedExcludes.add(excludeMatch)
        true
      }
      else -> {
        false
      }
    }
  }

  fun unusedIncludes() = includes - usedIncludes

  fun unusedExcludes() = excludes - usedExcludes

  class Builder {
    internal val includes = mutableSetOf<String>()
    internal val excludes = mutableSetOf<String>()
    internal var oldest: SemVer? = null
    internal var newest: SemVer? = null

    fun include(identifier: String) = apply {
      includes.add(identifier)
    }

    fun include(identifiers: Iterable<String>) = apply {
      includes.addAll(identifiers)
    }

    fun exclude(identifier: String) = apply {
      excludes.add(identifier)
    }

    fun exclude(identifiers: Iterable<String>) = apply {
      excludes.addAll(identifiers)
    }

    /**
     * The exclusive lower bound of the version range. Fields with `until` values greater than this
     * are retained.
     */
    fun oldest(oldest: String?) = apply {
      this.oldest = oldest?.toLowerCaseSemVer()
    }

    /**
     * The inclusive upper bound of the version range. Fields with `since` values less than or equal
     * to this are retained.
     */
    fun newest(newest: String?) = apply {
      this.newest = newest?.toLowerCaseSemVer()
    }

    fun build(): PruningRules {
      check(oldest == null || newest == null || oldest!! <= newest!!) {
        "expected oldest $oldest <= newest $newest"
      }
      return PruningRules(this)
    }
  }

  companion object {
    internal val FIELD_SINCE = ProtoMember.get(Options.FIELD_OPTIONS, "wire.since")
    internal val FIELD_UNTIL = ProtoMember.get(Options.FIELD_OPTIONS, "wire.until")
    internal val ENUM_CONSTANT_SINCE = ProtoMember.get(Options.ENUM_VALUE_OPTIONS, "wire.since")
    internal val ENUM_CONSTANT_UNTIL = ProtoMember.get(Options.ENUM_VALUE_OPTIONS, "wire.until")

    /**
     * Returns the identifier or wildcard that encloses `identifier`, or null if it is not enclosed.
     *
     *  * If `identifier` is a member this returns the enclosing type.
     *
     *  * If it is a type it returns the enclosing package with a wildcard, like
     *    `squareup.dinosaurs.*`.
     *
     *  * If it is a package with a wildcard, it returns the parent package with a wildcard, like
     *    `squareup.*`. The root wildcard is a lone asterisk, `*`.
     *
     */
    internal fun enclosing(identifier: String): String? {
      val hash = identifier.lastIndexOf('#')
      if (hash != -1) return identifier.substring(0, hash)

      val from = if (identifier.endsWith(".*")) identifier.length - 3 else identifier.length - 1
      val dot = identifier.lastIndexOf('.', from)
      if (dot != -1) return identifier.substring(0, dot) + ".*"

      if (identifier != "*") return "*"
      return null
    }
  }
}
