//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Schema](index.md)/[getService](get-service.md)

# getService

[common]\
fun [getService](get-service.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Service](../-service/index.md)?

Returns the service with the fully qualified name [name](get-service.md), or null if this schema defines no such service.

[common]\
fun [getService](get-service.md)(protoType: [ProtoType](../-proto-type/index.md)): [Service](../-service/index.md)?

Returns the service for [protoType](get-service.md), or null if this schema defines no such service.
