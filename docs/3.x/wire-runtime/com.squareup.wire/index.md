//[wire-runtime](../../index.md)/[com.squareup.wire](index.md)

# Package com.squareup.wire

## Types

| Name | Summary |
|---|---|
| [AndroidMessage](-android-message/index.md) | [jvm]<br>abstract class [AndroidMessage](-android-message/index.md)&lt;[M](-android-message/index.md) : [Message](-message/index.md)&lt;[M](-android-message/index.md), [B](-android-message/index.md)&gt;, [B](-android-message/index.md) : [Message.Builder](-message/-builder/index.md)&lt;[M](-android-message/index.md), [B](-android-message/index.md)&gt;&gt; : [Message](-message/index.md)&lt;[M](-android-message/index.md), [B](-android-message/index.md)&gt; , Parcelable<br>An Android-specific [Message](-message/index.md) which adds support for Parcelable. |
| [AnyMessage](-any-message/index.md) | [common]<br>class [AnyMessage](-any-message/index.md)(typeUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), value: ByteString) : [Message](-message/index.md)&lt;[AnyMessage](-any-message/index.md), [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)&gt; <br>Wire implementation of the google.protobuf.Any type. The Any type wraps an arbitrary protobuf message. Field of Any generated by Wire will be typed [AnyMessage](-any-message/index.md). [typeUrl](-any-message/type-url.md) is used to identify the type of the message, defined as 'type.googleapis.com/full.type.name'. [value](-any-message/value.md) is the serialized representation of the wrapped message. The [pack](-any-message/-companion/pack.md) and [unpack](-any-message/unpack.md) methods are available to wrap and unwrap an Any message. |
| [Duration](-duration/index.md) | [common]<br>class [Duration](-duration/index.md)<br>A measurement of time. Durations may be positive, zero, or negative.<br>[js, native, jvm]<br>[js, native]<br>class [Duration](-duration/index.md)<br>[jvm]<br>typealias [Duration](-duration/index.md) = [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html) |
| [EnumAdapter](-enum-adapter/index.md) | [common, js, jvm, native]<br>[common]<br>abstract class [EnumAdapter](-enum-adapter/index.md)&lt;[E](-enum-adapter/index.md) : [WireEnum](-wire-enum/index.md)&gt; : [ProtoAdapter](-proto-adapter/index.md)&lt;[E](-enum-adapter/index.md)&gt; <br>[js, jvm, native]<br>abstract class [EnumAdapter](-enum-adapter/index.md)&lt;[E](-enum-adapter/index.md) : WireEnum&gt; : [ProtoAdapter](-proto-adapter/index.md)&lt;[E](-enum-adapter/index.md)&gt; <br>An abstract [ProtoAdapter](-proto-adapter/index.md) that converts values of an enum to and from integers. |
| [FieldEncoding](-field-encoding/index.md) | [common]<br>enum [FieldEncoding](-field-encoding/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[FieldEncoding](-field-encoding/index.md)&gt; |
| [Instant](-instant/index.md) | [common]<br>class [Instant](-instant/index.md)<br>This represents a timestamp, though we use the name 'instant' in the runtime because that's what the JVM libraries use, and 'Timestamp' means something else on that platform.<br>[js, native, jvm]<br>[js, native]<br>class [Instant](-instant/index.md)<br>[jvm]<br>typealias [Instant](-instant/index.md) = [Instant](https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html) |
| [Message](-message/index.md) | [common, js, jvm, native]<br>[common, js, native]<br>abstract class [Message](-message/index.md)&lt;[M](-message/index.md) : [Message](-message/index.md)&lt;[M](-message/index.md), [B](-message/index.md)&gt;, [B](-message/index.md) : [Message.Builder](-message/-builder/index.md)&lt;[M](-message/index.md), [B](-message/index.md)&gt;&gt;<br>[jvm]<br>abstract class [Message](-message/index.md)&lt;[M](-message/index.md) : [Message](-message/index.md)&lt;[M](-message/index.md), [B](-message/index.md)&gt;, [B](-message/index.md) : [Message.Builder](-message/-builder/index.md)&lt;[M](-message/index.md), [B](-message/index.md)&gt;&gt; : [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)<br>A protocol buffer message. |
| [MessageSink](-message-sink/index.md) | [common]<br>interface [MessageSink](-message-sink/index.md)&lt;in [T](-message-sink/index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;<br>A writable stream of messages.<br>[js, jvm, native]<br>[js, native]<br>interface [MessageSink](-message-sink/index.md)&lt;in [T](-message-sink/index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;<br>[jvm]<br>interface [MessageSink](-message-sink/index.md)&lt;in [T](-message-sink/index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; : [Closeable](https://docs.oracle.com/javase/8/docs/api/java/io/Closeable.html) |
| [MessageSource](-message-source/index.md) | [common]<br>interface [MessageSource](-message-source/index.md)&lt;out [T](-message-source/index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;<br>A readable stream of messages.<br>[js, jvm, native]<br>[js, native]<br>interface [MessageSource](-message-source/index.md)&lt;out [T](-message-source/index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;<br>[jvm]<br>interface [MessageSource](-message-source/index.md)&lt;out [T](-message-source/index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; : [Closeable](https://docs.oracle.com/javase/8/docs/api/java/io/Closeable.html) |
| [OneOf](-one-of/index.md) | [common]<br>data class [OneOf](-one-of/index.md)&lt;out [K](-one-of/index.md) : [OneOf.Key](-one-of/-key/index.md)&lt;[T](-one-of/index.md)&gt;, [T](-one-of/index.md)&gt;(key: [K](-one-of/index.md), value: [T](-one-of/index.md))<br>A oneof's value and the key that identifies which field holds it. |
| [ProtoAdapter](-proto-adapter/index.md) | [common, js, jvm, native]<br>[common]<br>abstract class [ProtoAdapter](-proto-adapter/index.md)&lt;[E](-proto-adapter/index.md)&gt;(fieldEncoding: [FieldEncoding](-field-encoding/index.md), type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;?, typeUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, syntax: [Syntax](-syntax/index.md), identity: [E](-proto-adapter/index.md)?, sourceFile: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?)<br>[js, jvm, native]<br>abstract class [ProtoAdapter](-proto-adapter/index.md)&lt;[E](-proto-adapter/index.md)&gt;(fieldEncoding: FieldEncoding, type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;?, typeUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, syntax: Syntax, identity: [E](-proto-adapter/index.md)?, sourceFile: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?) |
| [ProtoReader](-proto-reader/index.md) | [common]<br>class [ProtoReader](-proto-reader/index.md)(source: BufferedSource)<br>Reads and decodes protocol message fields. |
| [ProtoWriter](-proto-writer/index.md) | [common]<br>class [ProtoWriter](-proto-writer/index.md)(sink: BufferedSink)<br>Utilities for encoding and writing protocol message fields. |
| [ReverseProtoWriter](-reverse-proto-writer/index.md) | [common]<br>class [ReverseProtoWriter](-reverse-proto-writer/index.md)<br>Encodes protocol buffer message fields from back-to-front for efficiency. Callers should write data in the opposite order that the data will be read. |
| [RuntimeEnumAdapter](-runtime-enum-adapter/index.md) | [jvm]<br>class [RuntimeEnumAdapter](-runtime-enum-adapter/index.md)&lt;[E](-runtime-enum-adapter/index.md) : WireEnum&gt; : [EnumAdapter](-enum-adapter/index.md)&lt;[E](-runtime-enum-adapter/index.md)&gt; <br>Converts values of an enum to and from integers using reflection. |
| [Service](-service/index.md) | [common]<br>interface [Service](-service/index.md) |
| [Syntax](-syntax/index.md) | [common]<br>enum [Syntax](-syntax/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[Syntax](-syntax/index.md)&gt; <br>Syntax version. |
| [WireEnum](-wire-enum/index.md) | [common]<br>interface [WireEnum](-wire-enum/index.md)<br>Interface for generated [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html) values to help serialization and deserialization. |
| [WireEnumConstant](-wire-enum-constant/index.md) | [common]<br>@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.FIELD](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-f-i-e-l-d/index.html)])<br>annotation class [WireEnumConstant](-wire-enum-constant/index.md)(declaredName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Annotates generated [WireEnum](-wire-enum/index.md) fields with metadata for serialization and deserialization. |
| [WireField](-wire-field/index.md) | [common]<br>@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.FIELD](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-f-i-e-l-d/index.html)])<br>annotation class [WireField](-wire-field/index.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), keyAdapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), adapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), label: [WireField.Label](-wire-field/-label/index.md), redacted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), declaredName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), jsonName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), oneofName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Annotates generated [Message](-message/index.md) fields with metadata for serialization and deserialization. |
| [WireRpc](-wire-rpc/index.md) | [common]<br>@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.FUNCTION](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-f-u-n-c-t-i-o-n/index.html)])<br>annotation class [WireRpc](-wire-rpc/index.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestAdapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), responseAdapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), sourceFile: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>For gRPC actions the path is formatted as /&lt;service name&gt;/&lt;method name&gt;. The path of the proto service below is /squareup.helloworld.Greeter/SayHello. |

## Functions

| Name | Summary |
|---|---|
| [decode](decode.md) | [darwin]<br>fun &lt;[E](decode.md)&gt; [ProtoAdapter](-proto-adapter/index.md#-1395866391%2FExtensions%2F2045859631)&lt;[E](decode.md)&gt;.[decode](decode.md)(data: &lt;ERROR CLASS&gt;): [E](decode.md)<br>Read an encoded message from data. |
| [durationOfSeconds](duration-of-seconds.md) | [common, js, jvm, native]<br>[common, js, native]<br>fun [durationOfSeconds](duration-of-seconds.md)(seconds: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), nano: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)): [Duration](-duration/index.md)<br>[jvm]<br>inline fun [durationOfSeconds](duration-of-seconds.md)(seconds: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), nano: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)): [Duration](-duration/index.md) |
| [get](get.md) | [jvm]<br>fun &lt;[T](get.md)&gt; [get](get.md)(value: [T](get.md)?, defaultValue: [T](get.md)): [T](get.md)<br>Returns value if it is not null; defaultValue otherwise. This is used to conveniently return a default value when a value is null. For example, |
| [ofEpochSecond](of-epoch-second.md) | [common, js, jvm, native]<br>[common, js, native]<br>fun [ofEpochSecond](of-epoch-second.md)(epochSecond: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), nano: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)): [Instant](-instant/index.md)<br>[jvm]<br>inline fun [ofEpochSecond](of-epoch-second.md)(epochSecond: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), nano: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)): [Instant](-instant/index.md) |