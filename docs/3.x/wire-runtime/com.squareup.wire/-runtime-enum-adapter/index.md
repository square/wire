//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[RuntimeEnumAdapter](index.md)

# RuntimeEnumAdapter

[jvm]\
class [RuntimeEnumAdapter](index.md)&lt;[E](index.md) : WireEnum&gt; : [EnumAdapter](../-enum-adapter/index.md)&lt;[E](index.md)&gt; 

Converts values of an enum to and from integers using reflection.

## Constructors

| | |
|---|---|
| [RuntimeEnumAdapter](-runtime-enum-adapter.md) | [jvm]<br>fun &lt;[E](index.md) : WireEnum&gt; [RuntimeEnumAdapter](-runtime-enum-adapter.md)(javaType: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[E](index.md)&gt;) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [asPacked](../-proto-adapter/as-packed.md) | [jvm]<br>fun [asPacked](../-proto-adapter/as-packed.md)(): [ProtoAdapter](../-proto-adapter/index.md)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)&gt;&gt; |
| [asRepeated](../-proto-adapter/as-repeated.md) | [jvm]<br>fun [asRepeated](../-proto-adapter/as-repeated.md)(): [ProtoAdapter](../-proto-adapter/index.md)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)&gt;&gt; |
| [decode](../-enum-adapter/decode.md) | [jvm]<br>open override fun [decode](../-enum-adapter/decode.md)(reader: ProtoReader): [E](index.md)<br>fun [decode](../-proto-adapter/decode.md)(stream: [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html)): [E](index.md)<br>fun [decode](../-proto-adapter/decode.md)(bytes: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [E](index.md)<br>fun [decode](../-proto-adapter/decode.md)(source: BufferedSource): [E](index.md)<br>fun [decode](../-proto-adapter/decode.md)(bytes: ByteString): [E](index.md) |
| [encode](../-enum-adapter/index.md#362516774%2FFunctions%2F1823866683) | [jvm]<br>fun [encode](../-enum-adapter/index.md#362516774%2FFunctions%2F1823866683)(value: [E](index.md)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>open override fun [encode](../-enum-adapter/encode.md)(writer: ProtoWriter, value: [E](index.md))<br>open override fun [encode](../-enum-adapter/encode.md)(writer: ReverseProtoWriter, value: [E](index.md))<br>fun [encode](../-enum-adapter/index.md#1817321442%2FFunctions%2F1823866683)(stream: [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html), value: [E](index.md))<br>fun [encode](../-enum-adapter/index.md#-490803903%2FFunctions%2F1823866683)(sink: BufferedSink, value: [E](index.md)) |
| [encodeByteString](../-enum-adapter/index.md#-320012307%2FFunctions%2F1823866683) | [jvm]<br>fun [encodeByteString](../-enum-adapter/index.md#-320012307%2FFunctions%2F1823866683)(value: [E](index.md)): ByteString |
| [encodedSize](../-enum-adapter/encoded-size.md) | [jvm]<br>open override fun [encodedSize](../-enum-adapter/encoded-size.md)(value: [E](index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [encodedSizeWithTag](../-enum-adapter/index.md#804415853%2FFunctions%2F1823866683) | [jvm]<br>open fun [encodedSizeWithTag](../-enum-adapter/index.md#804415853%2FFunctions%2F1823866683)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [E](index.md)?): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [encodeWithTag](../-enum-adapter/index.md#-1882930246%2FFunctions%2F1823866683) | [jvm]<br>open fun [encodeWithTag](../-enum-adapter/index.md#-1882930246%2FFunctions%2F1823866683)(writer: ProtoWriter, tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [E](index.md)?)<br>open fun [encodeWithTag](../-enum-adapter/index.md#1543761198%2FFunctions%2F1823866683)(writer: ReverseProtoWriter, tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), value: [E](index.md)?) |
| [equals](equals.md) | [jvm]<br>open operator override fun [equals](equals.md)(other: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [hashCode](hash-code.md) | [jvm]<br>open override fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [redact](../-enum-adapter/redact.md) | [jvm]<br>open override fun [redact](../-enum-adapter/redact.md)(value: [E](index.md)): [E](index.md) |
| [toString](../-enum-adapter/index.md#-2000452144%2FFunctions%2F1823866683) | [jvm]<br>open fun [toString](../-enum-adapter/index.md#-2000452144%2FFunctions%2F1823866683)(value: [E](index.md)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [identity](../-proto-adapter/identity.md) | [jvm]<br>val [identity](../-proto-adapter/identity.md): [E](index.md)? |
| [sourceFile](../-proto-adapter/source-file.md) | [jvm]<br>val [sourceFile](../-proto-adapter/source-file.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [syntax](../-proto-adapter/syntax.md) | [jvm]<br>val [syntax](../-proto-adapter/syntax.md): Syntax |
| [type](../-proto-adapter/type.md) | [jvm]<br>val [type](../-proto-adapter/type.md): [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;? |
| [typeUrl](../-proto-adapter/type-url.md) | [jvm]<br>val [typeUrl](../-proto-adapter/type-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
