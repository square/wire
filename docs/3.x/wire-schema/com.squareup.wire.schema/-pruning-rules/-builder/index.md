//[wire-schema](../../../../index.md)/[com.squareup.wire.schema](../../index.md)/[PruningRules](../index.md)/[Builder](index.md)

# Builder

[common]\
class [Builder](index.md)

## Functions

| Name | Summary |
|---|---|
| [addRoot](add-root.md) | [common]<br>fun [addRoot](add-root.md)(identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [PruningRules.Builder](index.md)<br>fun [addRoot](add-root.md)(identifiers: [Iterable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;): [PruningRules.Builder](index.md) |
| [build](build.md) | [common]<br>fun [build](build.md)(): [PruningRules](../index.md) |
| [only](only.md) | [common]<br>fun [only](only.md)(only: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?): [PruningRules.Builder](index.md)<br>The only version of the version range. Fields with until values greater than this, as well as fields with since values less than or equal to this, are retained. |
| [prune](prune.md) | [common]<br>fun [prune](prune.md)(identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [PruningRules.Builder](index.md)<br>fun [prune](prune.md)(identifiers: [Iterable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;): [PruningRules.Builder](index.md) |
| [since](since.md) | [common]<br>fun [since](since.md)(since: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?): [PruningRules.Builder](index.md)<br>The exclusive lower bound of the version range. Fields with until values greater than this are retained. |
| [until](until.md) | [common]<br>fun [until](until.md)(until: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?): [PruningRules.Builder](index.md)<br>The inclusive upper bound of the version range. Fields with since values less than or equal to this are retained. |
