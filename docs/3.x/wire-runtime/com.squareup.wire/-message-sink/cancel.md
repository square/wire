//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[MessageSink](index.md)/[cancel](cancel.md)

# cancel

[common]\
abstract fun [cancel](cancel.md)()

Truncate this stream abnormally. This attempts to signal to readers of this data that it is incomplete. Note that unlike some cancel methods this is not safe for concurrent use.

A stream must be closed after it is canceled.

## Throws

| | |
|---|---|
| [kotlin.IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if this stream is closed. |

[js, jvm, native]\
[js, jvm, native]\
abstract fun [cancel](cancel.md)()

## Throws

| | |
|---|---|
| [kotlin.IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if this stream is closed. |
