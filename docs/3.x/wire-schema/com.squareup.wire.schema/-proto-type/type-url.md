//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[ProtoType](index.md)/[typeUrl](type-url.md)

# typeUrl

[common]\
val [typeUrl](type-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?

Returns a string like "type.googleapis.com/packagename.messagename" or null if this type is a scalar or a map. Note that this returns a non-null string for enums because it doesn't know if the named type is a message or an enum.
