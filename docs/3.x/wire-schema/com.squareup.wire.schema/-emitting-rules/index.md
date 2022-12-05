//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[EmittingRules](index.md)

# EmittingRules

[common]\
class [EmittingRules](index.md)

A set of rules that describes which types to generate.

Despite the builder, instances of this class are not safe for concurrent use.

###  Identifier Matching

Identifiers in this set may be in the following forms:

<ul><li>Package names, followed by .*, like squareup.protos.person.*. This matches types and     services defined in the package and its descendant packages.</li><li>Fully qualified type and service names, like squareup.protos.person.Person.</li></ul>

Identifiers should not contain member names.

This set has *included identifiers* and *excluded identifiers*, with the most precise identifier taking precedence over the other ones. For instance, if there is one included identifier a.Movie along an excluded identifier a.*, the type a.Movie is considered included in the set.

If the includes set is empty, that implies that all elements should be included. Use this to exclude unwanted types and members without also including everything else.

## Constructors

| | |
|---|---|
| [EmittingRules](-emitting-rules.md) | [common]<br>fun [EmittingRules](-emitting-rules.md)() |

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [common]<br>class [Builder](-builder/index.md) |
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [includes](includes.md) | [common]<br>fun [includes](includes.md)(type: [ProtoType](../-proto-type/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if type should be generated. |
| [unusedExcludes](unused-excludes.md) | [common]<br>fun [unusedExcludes](unused-excludes.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [unusedIncludes](unused-includes.md) | [common]<br>fun [unusedIncludes](unused-includes.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |

## Properties

| Name | Summary |
|---|---|
| [isEmpty](is-empty.md) | [common]<br>val [isEmpty](is-empty.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
