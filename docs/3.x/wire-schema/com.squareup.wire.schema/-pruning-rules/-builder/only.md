//[wire-schema](../../../../index.md)/[com.squareup.wire.schema](../../index.md)/[PruningRules](../index.md)/[Builder](index.md)/[only](only.md)

# only

[common]\
fun [only](only.md)(only: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?): [PruningRules.Builder](index.md)

The only version of the version range. Fields with until values greater than this, as well as fields with since values less than or equal to this, are retained.
