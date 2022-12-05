//[wire-grpc-server](../../../index.md)/[com.squareup.wire.kotlin.grpcserver](../index.md)/[WireMethodMarshaller](index.md)

# WireMethodMarshaller

[jvm]\
interface [WireMethodMarshaller](index.md)&lt;[T](index.md)&gt; : MethodDescriptor.Marshaller&lt;[T](index.md)&gt; 

Interface for generated method marshallers used in Google style grpc stubs.

Adds a method for fetching the underlying class at run time to support generating other marshallers (like JSON encoding) from an instance of this class.

## Functions

| Name | Summary |
|---|---|
| [marshalledClass](marshalled-class.md) | [jvm]<br>abstract fun [marshalledClass](marshalled-class.md)(): [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[T](index.md)&gt; |
| [parse](index.md#-2087896245%2FFunctions%2F1317461078) | [jvm]<br>abstract fun [parse](index.md#-2087896245%2FFunctions%2F1317461078)(p0: [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html)): [T](index.md) |
| [stream](index.md#1520829312%2FFunctions%2F1317461078) | [jvm]<br>abstract fun [stream](index.md#1520829312%2FFunctions%2F1317461078)(p0: [T](index.md)): [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html) |
