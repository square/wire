//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[MessageSink](index.md)/[write](write.md)

# write

[common]\
abstract fun [write](write.md)(message: [T](index.md))

Encode [message](write.md) to bytes and enqueue the bytes for delivery, waiting if necessary until the delivery channel has capacity for the encoded message.

## Throws

| | |
|---|---|
| [kotlin.IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if this stream is closed. |

[js, jvm, native]\
[js, jvm, native]\
abstract fun [write](write.md)(message: [T](index.md))

## Throws

| | |
|---|---|
| [kotlin.IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if this stream is closed. |
