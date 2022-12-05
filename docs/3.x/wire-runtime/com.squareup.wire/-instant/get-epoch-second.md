//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[Instant](index.md)/[getEpochSecond](get-epoch-second.md)

# getEpochSecond

[common]\
fun [getEpochSecond](get-epoch-second.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)

Returns the number of seconds since the UNIX epoch (1970-01-01T00:00:00Z) if this value is positive, or until the UNIX epoch if this value is negative.

For example, this value will be -1 for the instant 1969-12-31T23:59:59Z, and 1 for the instant 1970-01-01T00:00:01Z.

[js, native]\
[js, native]\
fun [getEpochSecond](get-epoch-second.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)
