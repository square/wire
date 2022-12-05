//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoAdapter](index.md)/[encodeWithTag](encode-with-tag.md)

# encodeWithTag

[common, js, native]\
[common]\
open fun [encodeWithTag](encode-with-tag.md)(writer: [ProtoWriter](../-proto-writer/index.md), tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [E](index.md)?)

[js, native]\
open fun [encodeWithTag](encode-with-tag.md)(writer: ProtoWriter, tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [E](index.md)?)

[common]\
open fun [encodeWithTag](encode-with-tag.md)(writer: [ReverseProtoWriter](../-reverse-proto-writer/index.md), tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [E](index.md)?)

[js, native]\
open fun [encodeWithTag](encode-with-tag.md)(writer: ReverseProtoWriter, tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [E](index.md)?)

Write tag and value to writer. If value is null this does nothing.

[jvm]\
open fun [encodeWithTag](encode-with-tag.md)(writer: ProtoWriter, tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [E](index.md)?)

open fun [encodeWithTag](encode-with-tag.md)(writer: ReverseProtoWriter, tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [E](index.md)?)
