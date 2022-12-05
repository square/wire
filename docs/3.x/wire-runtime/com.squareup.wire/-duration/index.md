//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[Duration](index.md)

# Duration

[common]\
class [Duration](index.md)

A measurement of time. Durations may be positive, zero, or negative.

Positive durations are measured in seconds, with both #getSeconds and #getNano parts with non-negative signs.

Negative durations may be surprising. The #getSeconds property is negative, but the #getNano property is non-negative! To represent -1.3 seconds the seconds property is -2 and the nanos property is 700,000,000.

[js, native]\
class [Duration](index.md)

[jvm]\
typealias [Duration](index.md) = [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html)

## Functions

| Name | Summary |
|---|---|
| [getNano](get-nano.md) | [common]<br>fun [getNano](get-nano.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Returns a value in [0..1,000,000,000).<br>[js, native]<br>[js, native]<br>fun [getNano](get-nano.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [getSeconds](get-seconds.md) | [common, js, native]<br>[common, js, native]<br>fun [getSeconds](get-seconds.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |
