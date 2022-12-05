//[wire-schema](../../../index.md)/[com.squareup.wire](../index.md)/[WireLogger](index.md)/[unusedPrunes](unused-prunes.md)

# unusedPrunes

[common]\
abstract fun [unusedPrunes](unused-prunes.md)(unusedPrunes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;)

This is called if some prune values have not been used when Wire pruned the schema model. Note that prune should contain package names (suffixed with .*), type names, and member names only. It should not contain file paths. Unused prunes can happen if the referenced type or service isn't part of any .proto files defined in either com.squareup.wire.schema.WireRun.sourcePath or com.squareup.wire.schema.WireRun.protoPath, or if a broader prune value is already defined.
