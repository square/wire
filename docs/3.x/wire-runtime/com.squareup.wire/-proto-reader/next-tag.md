//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoReader](index.md)/[nextTag](next-tag.md)

# nextTag

[common]\
fun [nextTag](next-tag.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)

Reads and returns the next tag of the message, or -1 if there are no further tags. Use [peekFieldEncoding](peek-field-encoding.md) after calling this method to query its encoding. This silently skips groups.
