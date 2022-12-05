//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoReader](index.md)/[addUnknownField](add-unknown-field.md)

# addUnknownField

[common]\
fun [addUnknownField](add-unknown-field.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), fieldEncoding: [FieldEncoding](../-field-encoding/index.md), value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?)

Store an already read field temporarily. Once the entire message is read, call [endMessageAndGetUnknownFields](end-message-and-get-unknown-fields.md) to retrieve unknown fields.
