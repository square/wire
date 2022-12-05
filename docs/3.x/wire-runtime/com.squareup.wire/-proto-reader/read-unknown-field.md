//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoReader](index.md)/[readUnknownField](read-unknown-field.md)

# readUnknownField

[common]\
fun [readUnknownField](read-unknown-field.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))

Read an unknown field and store temporarily. Once the entire message is read, call [endMessageAndGetUnknownFields](end-message-and-get-unknown-fields.md) to retrieve unknown fields.
