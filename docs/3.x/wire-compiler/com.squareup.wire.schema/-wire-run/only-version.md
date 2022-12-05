//[wire-compiler](../../../index.md)/[com.squareup.wire.schema](../index.md)/[WireRun](index.md)/[onlyVersion](only-version.md)

# onlyVersion

[jvm]\
val [onlyVersion](only-version.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null

The only version of the version range. Fields with until values greater than this, as well as fields with since values less than or equal to this, are retained. This field is mutually exclusive with sinceVersion and untilVersion.
