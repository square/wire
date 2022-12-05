//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Linker](index.md)/[getForOptions](get-for-options.md)

# getForOptions

[common]\
fun [getForOptions](get-for-options.md)(protoType: [ProtoType](../-proto-type/index.md)): [Type](../-type/index.md)?

Returns the type or null if it doesn't exist. Before this returns it ensures members are linked so that options may dereference them.
