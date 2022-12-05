//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Linker](index.md)/[link](link.md)

# link

[common]\
fun [link](link.md)(sourceProtoFiles: [Iterable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)&lt;[ProtoFile](../-proto-file/index.md)&gt;): [Schema](../-schema/index.md)

Link all features of all files in [sourceProtoFiles](link.md) to create a schema. This will also partially link any imported files necessary.
