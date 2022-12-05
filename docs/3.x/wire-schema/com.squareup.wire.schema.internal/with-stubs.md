//[wire-schema](../../index.md)/[com.squareup.wire.schema.internal](index.md)/[withStubs](with-stubs.md)

# withStubs

[common]\
fun [Schema](../com.squareup.wire.schema/-schema/index.md).[withStubs](with-stubs.md)(typesToStub: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../com.squareup.wire.schema/-proto-type/index.md)&gt;): [Schema](../com.squareup.wire.schema/-schema/index.md)

Replace types in this schema which are present in [typesToStub](with-stubs.md) with empty shells that have no outward references. This has to be done in this module so that we can access the internal constructor to avoid re-linking.
