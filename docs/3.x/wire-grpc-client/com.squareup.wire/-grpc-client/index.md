//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcClient](index.md)

# GrpcClient

[common, js, jvm, native]\
class [GrpcClient](index.md)

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [jvm]<br>class [Builder](-builder/index.md) |

## Functions

| Name | Summary |
|---|---|
| [create](create.md) | [jvm]<br>inline fun &lt;[T](create.md) : Service&gt; [create](create.md)(): [T](create.md)<br>Returns a [T](create.md) that makes gRPC calls using this client.<br>[jvm]<br>fun &lt;[T](create.md) : Service&gt; [create](create.md)(service: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[T](create.md)&gt;): [T](create.md)<br>Returns a [service](create.md) that makes gRPC calls using this client. |
| [newBuilder](new-builder.md) | [jvm]<br>fun [newBuilder](new-builder.md)(): [GrpcClient.Builder](-builder/index.md) |
| [newCall](new-call.md) | [common, js, jvm, native]<br>[common]<br>fun &lt;[S](new-call.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [R](new-call.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [newCall](new-call.md)(method: [GrpcMethod](../-grpc-method/index.md)&lt;[S](new-call.md), [R](new-call.md)&gt;): [GrpcCall](../-grpc-call/index.md)&lt;[S](new-call.md), [R](new-call.md)&gt;<br>[js, jvm, native]<br>fun &lt;[S](new-call.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [R](new-call.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [newCall](new-call.md)(method: GrpcMethod&lt;[S](new-call.md), [R](new-call.md)&gt;): GrpcCall&lt;[S](new-call.md), [R](new-call.md)&gt; |
| [newStreamingCall](new-streaming-call.md) | [common, js, jvm, native]<br>[common]<br>fun &lt;[S](new-streaming-call.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [R](new-streaming-call.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [newStreamingCall](new-streaming-call.md)(method: [GrpcMethod](../-grpc-method/index.md)&lt;[S](new-streaming-call.md), [R](new-streaming-call.md)&gt;): [GrpcStreamingCall](../-grpc-streaming-call/index.md)&lt;[S](new-streaming-call.md), [R](new-streaming-call.md)&gt;<br>[js, jvm, native]<br>fun &lt;[S](new-streaming-call.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [R](new-streaming-call.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [newStreamingCall](new-streaming-call.md)(method: GrpcMethod&lt;[S](new-streaming-call.md), [R](new-streaming-call.md)&gt;): GrpcStreamingCall&lt;[S](new-streaming-call.md), [R](new-streaming-call.md)&gt; |
