//[wire-runtime](../../index.md)/[com.squareup.wire](index.md)/[decode](decode.md)

# decode

[darwin]\
fun &lt;[E](decode.md)&gt; [ProtoAdapter](-proto-adapter/index.md#-1395866391%2FExtensions%2F2045859631)&lt;[E](decode.md)&gt;.[decode](decode.md)(data: &lt;ERROR CLASS&gt;): [E](decode.md)

Read an encoded message from data.

Note: this method is marked with ExperimentalUnsignedTypes annotation and requires an opt-in (e.g. through @OptIn(ExperimentalUnsignedTypes::class) to be used.

## Throws

| | |
|---|---|
| IllegalArgumentException | if data.length is larger than [Int.MAX_VALUE](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/-m-a-x_-v-a-l-u-e.html). |
