//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[Instant](index.md)

# Instant

[common]\
class [Instant](index.md)

This represents a timestamp, though we use the name 'instant' in the runtime because that's what the JVM libraries use, and 'Timestamp' means something else on that platform.

[js, native]\
class [Instant](index.md)

[jvm]\
typealias [Instant](index.md) = [Instant](https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html)

## Functions

| Name | Summary |
|---|---|
| [getEpochSecond](get-epoch-second.md) | [common]<br>fun [getEpochSecond](get-epoch-second.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)<br>Returns the number of seconds since the UNIX epoch (1970-01-01T00:00:00Z) if this value is positive, or until the UNIX epoch if this value is negative.<br>[js, native]<br>[js, native]<br>fun [getEpochSecond](get-epoch-second.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |
| [getNano](get-nano.md) | [common]<br>fun [getNano](get-nano.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Returns a value in the range [0..1,000,000,000) indicating the fraction of a second that is added to [getEpochSecond](get-epoch-second.md).<br>[js, native]<br>[js, native]<br>fun [getNano](get-nano.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
