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
 * If a member is a root in the set, its type is implicitly also considered a root. A type that is
 * a root without a specific member implicitly set all of that type's members as roots, but not its
 * nested types.
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
 * This set has *root identifiers* and *prune identifiers*, with the most precise identifier
 * taking precedence over the other ones. For instance, if there is one root identifier
 * `a.Movie` along a pruning identifier `a.*`, the type `a.Movie` is considered a root.
 *
 * If the roots set is empty, that implies that all elements are considered roots. Use this to
 * prune unwanted types and members without also marking everything else as roots.
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
  private val roots = builder.roots.toSet()
  private val prunes = builder.prunes.toSet()
  private val since = builder.since
  private val until = builder.until
  private val usedRoots = mutableSetOf<String>()
  private val usedPrunes = mutableSetOf<String>()

  val isEmpty: Boolean
    get() = roots.isEmpty() && prunes.isEmpty() && since == null && until == null

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
    if (until != null) {
      val sinceOption = options.get(sinceMember)
      val since = (sinceOption as? String)?.toLowerCaseSemVer()
      if (since != null && since >= until) return false
    }

    if (since != null) {
      val untilOption = options.get(untilMember)
      val until = (untilOption as? String)?.toLowerCaseSemVer()
      if (until != null && until <= since) return false
    }

    return true
  }

  /** Returns true if [type] is a root. */
  fun isRoot(type: ProtoType) = isRoot(type.toString())

  /** Returns true if [protoMember] is a root. */
  fun isRoot(protoMember: ProtoMember) = isRoot(protoMember.toString())

  /**
   * Returns true if [identifier] or any of its enclosing identifiers is a root. If any enclosing
   * identifier is pruned, that takes precedence and this returns false unless the root identifier
   * is more precise.
   */
  private fun isRoot(identifier: String): Boolean {
    if (roots.isEmpty()) return !prunes(identifier)

    var rootMatch: String? = null
    var pruneMatch: String? = null
    var rule: String? = identifier
    while (rule != null) {
      if (pruneMatch == null && prunes.contains(rule)) {
        pruneMatch = rule
      }
      if (rootMatch == null && roots.contains(rule)) {
        rootMatch = rule
      }
      rule = enclosing(rule)
    }

    val isRoot = when {
      pruneMatch != null && rootMatch != null -> pruneMatch.length < rootMatch.length
      pruneMatch != null -> false
      rootMatch != null -> true
      else -> false
    }
    if (isRoot) {
      usedRoots.add(rootMatch!!)
    } else if (pruneMatch != null) {
      usedPrunes.add(pruneMatch)
    }
    return isRoot
  }

  /**
   * Returns true if [type] should be pruned, even if it is a transitive dependency of a root. In
   * that case, the referring member is also pruned.
   */
  fun prunes(type: ProtoType) = prunes(type.toString())

  /** Returns true if [protoMember] should be pruned. */
  fun prunes(protoMember: ProtoMember) = prunes(protoMember.toString())

  /** Returns true if [identifier] or any of its enclosing identifiers is pruned.  */
  private fun prunes(identifier: String): Boolean {
    var rootMatch: String? = null
    var pruneMatch: String? = null
    var rule: String? = identifier
    while (rule != null) {
      if (pruneMatch == null && prunes.contains(rule)) {
        pruneMatch = rule
      }
      if (rootMatch == null && roots.contains(rule)) {
        rootMatch = rule
      }
      rule = enclosing(rule)
    }

    val pruned = when {
      pruneMatch != null && rootMatch != null -> pruneMatch.length >= rootMatch.length
      pruneMatch != null -> true
      else -> false
    }
    if (pruned) {
      usedPrunes.add(pruneMatch!!)
    }
    return pruned
  }

  fun unusedRoots() = roots - usedRoots

  fun unusedPrunes() = prunes - usedPrunes

  class Builder {
    internal val roots = mutableSetOf<String>()
    internal val prunes = mutableSetOf<String>()
    internal var since: SemVer? = null
    internal var until: SemVer? = null

    fun addRoot(identifier: String) = apply {
      roots.add(identifier)
    }

    fun addRoot(identifiers: Iterable<String>) = apply {
      roots.addAll(identifiers)
    }

    fun prune(identifier: String) = apply {
      prunes.add(identifier)
    }

    fun prune(identifiers: Iterable<String>) = apply {
      prunes.addAll(identifiers)
    }

    /**
     * The exclusive lower bound of the version range. Fields with `until` values greater than this
     * are retained.
     */
    fun since(since: String?) = apply {
      this.since = since?.toLowerCaseSemVer()
    }

    /**
     * The inclusive upper bound of the version range. Fields with `since` values less than or equal
     * to this are retained.
     */
    fun until(until: String?) = apply {
      this.until = until?.toLowerCaseSemVer()
    }

    fun build(): PruningRules {
      check(since == null || until == null || since!! < until!!) {
        "expected since $since < until $until"
      }
      val conflictingRules = roots.intersect(prunes)
      check(conflictingRules.isEmpty()) {
        "same rule(s) defined in both roots and prunes: ${conflictingRules.joinToString()}"
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
     * Returns the identifier or wildcard that encloses [identifier], or null if it is not enclosed.
     *
     *  * If [identifier] is a member this returns the enclosing type.
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
