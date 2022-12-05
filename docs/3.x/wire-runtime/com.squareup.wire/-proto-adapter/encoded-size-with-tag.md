//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoAdapter](index.md)/[encodedSizeWithTag](encoded-size-with-tag.md)

# encodedSizeWithTag

[common, js, native]\
[common, js, native]\
open fun [encodedSizeWithTag](encoded-size-with-tag.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [E](index.md)?): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)

The size of tag and value in the wire format. This size includes the tag, type, length-delimited prefix (should the type require one), and value. Returns 0 if value is null.

[jvm]\
open fun [encodedSizeWithTag](encoded-size-with-tag.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [E](index.md)?): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)
