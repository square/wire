//[wire-grpc-server](../../index.md)/[com.squareup.wire.kotlin.grpcserver](index.md)

# Package com.squareup.wire.kotlin.grpcserver

## Types

| Name | Summary |
|---|---|
| [FlowAdapter](-flow-adapter/index.md) | [jvm]<br>object [FlowAdapter](-flow-adapter/index.md)<br>This is an adapter class to convert Wire generated Channel based routines to flow based functions compatible with io.grpc:protoc-gen-grpc-kotlin. |
| [MessageSinkAdapter](-message-sink-adapter/index.md) | [jvm]<br>class [MessageSinkAdapter](-message-sink-adapter/index.md)&lt;[T](-message-sink-adapter/index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;(responseObserver: StreamObserver&lt;[T](-message-sink-adapter/index.md)&gt;) : MessageSink&lt;[T](-message-sink-adapter/index.md)&gt; |
| [MessageSourceAdapter](-message-source-adapter/index.md) | [jvm]<br>class [MessageSourceAdapter](-message-source-adapter/index.md)&lt;[T](-message-source-adapter/index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; : MessageSource&lt;[T](-message-source-adapter/index.md)&gt; , StreamObserver&lt;[T](-message-source-adapter/index.md)&gt; |
| [WireBindableService](-wire-bindable-service/index.md) | [jvm]<br>interface [WireBindableService](-wire-bindable-service/index.md) : BindableService<br>Wire specific interface for BindableService. |
| [WireMethodMarshaller](-wire-method-marshaller/index.md) | [jvm]<br>interface [WireMethodMarshaller](-wire-method-marshaller/index.md)&lt;[T](-wire-method-marshaller/index.md)&gt; : MethodDescriptor.Marshaller&lt;[T](-wire-method-marshaller/index.md)&gt; <br>Interface for generated method marshallers used in Google style grpc stubs. |
