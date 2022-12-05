//[wire-runtime](../../../../index.md)/[com.squareup.wire](../../index.md)/[ProtoAdapter](../index.md)/[EnumConstantNotFoundException](index.md)

# EnumConstantNotFoundException

[common, js]\
class [EnumConstantNotFoundException](index.md)(value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;?) : [IllegalArgumentException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-argument-exception/index.html)

[jvm]\
class [EnumConstantNotFoundException](index.md)(value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;?) : [IllegalArgumentException](https://docs.oracle.com/javase/8/docs/api/java/lang/IllegalArgumentException.html)

[native]\
class [EnumConstantNotFoundException](index.md)(value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;?)

## Constructors

| | |
|---|---|
| [EnumConstantNotFoundException](-enum-constant-not-found-exception.md) | [jvm]<br>fun [EnumConstantNotFoundException](-enum-constant-not-found-exception.md)(value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), type: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;*&gt;) |

## Functions

| Name | Summary |
|---|---|
| [addSuppressed](index.md#282858770%2FFunctions%2F1823866683) | [jvm]<br>fun [addSuppressed](index.md#282858770%2FFunctions%2F1823866683)(p0: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) |
| [fillInStackTrace](index.md#-1102069925%2FFunctions%2F1823866683) | [jvm]<br>open fun [fillInStackTrace](index.md#-1102069925%2FFunctions%2F1823866683)(): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html) |
| [getLocalizedMessage](index.md#1043865560%2FFunctions%2F1823866683) | [jvm]<br>open fun [getLocalizedMessage](index.md#1043865560%2FFunctions%2F1823866683)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [getStackTrace](index.md#2050903719%2FFunctions%2F1823866683) | [jvm]<br>open fun [getStackTrace](index.md#2050903719%2FFunctions%2F1823866683)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[StackTraceElement](https://docs.oracle.com/javase/8/docs/api/java/lang/StackTraceElement.html)&gt; |
| [getSuppressed](index.md#672492560%2FFunctions%2F1823866683) | [jvm]<br>fun [getSuppressed](index.md#672492560%2FFunctions%2F1823866683)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)&gt; |
| [initCause](index.md#-418225042%2FFunctions%2F1823866683) | [jvm]<br>open fun [initCause](index.md#-418225042%2FFunctions%2F1823866683)(p0: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html) |
| [printStackTrace](index.md#-1769529168%2FFunctions%2F1823866683) | [jvm]<br>open fun [printStackTrace](index.md#-1769529168%2FFunctions%2F1823866683)()<br>open fun [printStackTrace](index.md#1841853697%2FFunctions%2F1823866683)(p0: [PrintStream](https://docs.oracle.com/javase/8/docs/api/java/io/PrintStream.html))<br>open fun [printStackTrace](index.md#1175535278%2FFunctions%2F1823866683)(p0: [PrintWriter](https://docs.oracle.com/javase/8/docs/api/java/io/PrintWriter.html)) |
| [setStackTrace](index.md#2135801318%2FFunctions%2F1823866683) | [jvm]<br>open fun [setStackTrace](index.md#2135801318%2FFunctions%2F1823866683)(p0: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[StackTraceElement](https://docs.oracle.com/javase/8/docs/api/java/lang/StackTraceElement.html)&gt;) |

## Properties

| Name | Summary |
|---|---|
| [cause](index.md#-654012527%2FProperties%2F-1082500773) | [common]<br>open val [cause](index.md#-654012527%2FProperties%2F-1082500773): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? |
| [cause](index.md#-654012527%2FProperties%2F-1733684867) | [js]<br>open val [cause](index.md#-654012527%2FProperties%2F-1733684867): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? |
| [cause](index.md#-654012527%2FProperties%2F1823866683) | [jvm]<br>open val [cause](index.md#-654012527%2FProperties%2F1823866683): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? |
| [message](index.md#1824300659%2FProperties%2F-1082500773) | [common]<br>open val [message](index.md#1824300659%2FProperties%2F-1082500773): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [message](index.md#1824300659%2FProperties%2F-1733684867) | [js]<br>open val [message](index.md#1824300659%2FProperties%2F-1733684867): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [message](index.md#1824300659%2FProperties%2F1823866683) | [jvm]<br>open val [message](index.md#1824300659%2FProperties%2F1823866683): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [value](value.md) | [common, jvm]<br>@[JvmField](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/index.html)<br>val [value](value.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>[js, native]<br>val [value](value.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
