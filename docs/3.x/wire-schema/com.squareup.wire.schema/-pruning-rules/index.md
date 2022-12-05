//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[PruningRules](index.md)

# PruningRules

[common]\
class [PruningRules](index.md)

A set of rules that describes which types and members to retain and which to remove.

Members may be pruned using either their identifier (package, type name, member name) or their version (since and until options).

Despite the builder, instances of this class are not safe for concurrent use.

###  Identifier Matching

If a member is a root in the set, its type is implicitly also considered a root. A type that is a root without a specific member implicitly set all of that type's members as roots, but not its nested types.

Identifiers in this set may be in the following forms:

<ul><li>Package names, followed by .*, like squareup.protos.person.*. This matches types and     services defined in the package and its descendant packages.</li><li>Fully qualified type and service names, like squareup.protos.person.Person.</li><li>Fully qualified member names, which are type names followed by a '#', followed by a member     name, like squareup.protos.person.Person#address. Members may be fields, enum constants or     RPCs.</li></ul>

An identifier set populated with Movie and Actor#name contains all members of Movie (such as Movie#name and Movie#release_date). It contains the type Actor and one member Actor#name, but not Actor#birth_date or Actor#oscar_count.

This set has *root identifiers* and *prune identifiers*, with the most precise identifier taking precedence over the other ones. For instance, if there is one root identifier a.Movie along a pruning identifier a.*, the type a.Movie is considered a root.

If the roots set is empty, that implies that all elements are considered roots. Use this to prune unwanted types and members without also marking everything else as roots.

###  Version Matching

Members may be declared with wire.since and wire.until options. For example, these options declare a field age that was replaced with birth_date in version "5.0":

optional int32 age = 3 [(wire.until) = "5.0"];\
optional Date birth_date = 4 [(wire.since) = "5.0"];

Client code should typically target a single version. In this example, versions <= "4.0" will have the age field only and versions >= "5.0" will have the birth_date field only. One can target a single version using [only](only.md).

Service code that supports many clients should support the union of versions of all supported clients. Such code will have both the age and birth_date fields.

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [common]<br>class [Builder](-builder/index.md) |
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [isEnumConstantRetainedVersion](is-enum-constant-retained-version.md) | [common]<br>fun [isEnumConstantRetainedVersion](is-enum-constant-retained-version.md)(options: [Options](../-options/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true unless [options](is-enum-constant-retained-version.md) specifies a version that is outside of the configured range. |
| [isFieldRetainedVersion](is-field-retained-version.md) | [common]<br>fun [isFieldRetainedVersion](is-field-retained-version.md)(options: [Options](../-options/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true unless [options](is-field-retained-version.md) specifies a version that is outside of the configured range. |
| [isRoot](is-root.md) | [common]<br>fun [isRoot](is-root.md)(protoMember: [ProtoMember](../-proto-member/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if [protoMember](is-root.md) is a root.<br>[common]<br>fun [isRoot](is-root.md)(type: [ProtoType](../-proto-type/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if [type](is-root.md) is a root. |
| [prunes](prunes.md) | [common]<br>fun [prunes](prunes.md)(protoMember: [ProtoMember](../-proto-member/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if [protoMember](prunes.md) should be pruned.<br>[common]<br>fun [prunes](prunes.md)(type: [ProtoType](../-proto-type/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if [type](prunes.md) should be pruned, even if it is a transitive dependency of a root. In that case, the referring member is also pruned. |
| [unusedPrunes](unused-prunes.md) | [common]<br>fun [unusedPrunes](unused-prunes.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [unusedRoots](unused-roots.md) | [common]<br>fun [unusedRoots](unused-roots.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |

## Properties

| Name | Summary |
|---|---|
| [isEmpty](is-empty.md) | [common]<br>val [isEmpty](is-empty.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [only](only.md) | [common]<br>val [only](only.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [prunes](prunes.md) | [common]<br>val [prunes](prunes.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [roots](roots.md) | [common]<br>val [roots](roots.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [since](since.md) | [common]<br>val [since](since.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [until](until.md) | [common]<br>val [until](until.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
