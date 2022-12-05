//[wire-runtime](../../../../../index.md)/[com.squareup.wire](../../../index.md)/[ProtoAdapter](../../index.md)/[Companion](../index.md)/[UnsupportedTypeProtoAdapter](index.md)

# UnsupportedTypeProtoAdapter

[jvm]\
class [UnsupportedTypeProtoAdapter](index.md) : [ProtoAdapter](../../index.md)&lt;[Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)&gt; 

Stub [ProtoAdapter](../../index.md) for Wire types which are typeliased to java.time types on the JVM such as [Duration](../../../-duration/index.md) and [Instant](../../../-instant/index.md). This proto adapter is used when the corresponding java.time type is missing from the JVM classpath.

## Constructors

| | |
|---|---|
| [UnsupportedTypeProtoAdapter](-unsupported-type-proto-adapter.md) | [jvm]<br>fun [UnsupportedTypeProtoAdapter](-unsupported-type-proto-adapter.md)() |

## Functions

| Name | Summary |
|---|---|
| [asPacked](../../as-packed.md) | [jvm]<br>fun [asPacked](../../as-packed.md)(): [ProtoAdapter](../../index.md)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)&gt;&gt; |
| [asRepeated](../../as-repeated.md) | [jvm]<br>fun [asRepeated](../../as-repeated.md)(): [ProtoAdapter](../../index.md)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)&gt;&gt; |
| [decode](decode.md) | [jvm]<br>open override fun [decode](decode.md)(reader: ProtoReader): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)<br>fun [decode](../../decode.md)(stream: [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html)): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)<br>fun [decode](../../decode.md)(bytes: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)<br>fun [decode](../../decode.md)(source: BufferedSource): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)<br>fun [decode](../../decode.md)(bytes: ByteString): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html) |
| [encode](index.md#-451862368%2FFunctions%2F1823866683) | [jvm]<br>fun [encode](index.md#-451862368%2FFunctions%2F1823866683)(value: [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>open override fun [encode](encode.md)(writer: ProtoWriter, value: [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)<br>open override fun [encode](encode.md)(writer: ReverseProtoWriter, value: [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)<br>fun [encode](index.md#-1563320484%2FFunctions%2F1823866683)(stream: [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html), value: [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html))<br>fun [encode](index.md#1486071995%2FFunctions%2F1823866683)(sink: BufferedSink, value: [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)) |
| [encodeByteString](index.md#626486119%2FFunctions%2F1823866683) | [jvm]<br>fun [encodeByteString](index.md#626486119%2FFunctions%2F1823866683)(value: [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)): ByteString |
| [encodedSize](encoded-size.md) | [jvm]<br>open override fun [encodedSize](encoded-size.md)(value: [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html) |
| [encodedSizeWithTag](index.md#-832141005%2FFunctions%2F1823866683) | [jvm]<br>open fun [encodedSizeWithTag](index.md#-832141005%2FFunctions%2F1823866683)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)?): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [encodeWithTag](index.md#-754100352%2FFunctions%2F1823866683) | [jvm]<br>open fun [encodeWithTag](index.md#-754100352%2FFunctions%2F1823866683)(writer: ProtoWriter, tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)?)<br>open fun [encodeWithTag](index.md#-2018553612%2FFunctions%2F1823866683)(writer: ReverseProtoWriter, tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)?) |
| [redact](redact.md) | [jvm]<br>open override fun [redact](redact.md)(value: [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html) |
| [toString](index.md#-1033487030%2FFunctions%2F1823866683) | [jvm]<br>open fun [toString](index.md#-1033487030%2FFunctions%2F1823866683)(value: [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [identity](../../identity.md) | [jvm]<br>val [identity](../../identity.md): [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)? |
| [sourceFile](../../source-file.md) | [jvm]<br>val [sourceFile](../../source-file.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [syntax](../../syntax.md) | [jvm]<br>val [syntax](../../syntax.md): Syntax |
| [type](../../type.md) | [jvm]<br>val [type](../../type.md): [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;? |
| [typeUrl](../../type-url.md) | [jvm]<br>val [typeUrl](../../type-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
