//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[MessageSource](index.md)/[read](read.md)

# read

[common]\
abstract fun [read](read.md)(): [T](index.md)?

Read the next length-prefixed message on the stream and return it. Returns null if there are no further messages on this stream.

## Throws

| | |
|---|---|
| okio.IOException | if the next message cannot be read, or if the stream was abnormally     terminated by its producer. |

[js, jvm, native]\
[js, jvm, native]\
abstract fun [read](read.md)(): [T](index.md)?

## Throws

| | |
|---|---|
| okio.IOException | if the next message cannot be read, or if the stream was abnormally     terminated by its producer. |
