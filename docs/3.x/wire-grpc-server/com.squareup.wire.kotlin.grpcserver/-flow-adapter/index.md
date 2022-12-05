//[wire-grpc-server](../../../index.md)/[com.squareup.wire.kotlin.grpcserver](../index.md)/[FlowAdapter](index.md)

# FlowAdapter

[jvm]\
object [FlowAdapter](index.md)

This is an adapter class to convert Wire generated Channel based routines to flow based functions compatible with io.grpc:protoc-gen-grpc-kotlin.

## Functions

| Name | Summary |
|---|---|
| [bidiStream](bidi-stream.md) | [jvm]<br>fun &lt;[I](bidi-stream.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [O](bidi-stream.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [bidiStream](bidi-stream.md)(context: [CoroutineContext](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html), request: Flow&lt;[I](bidi-stream.md)&gt;, f: suspend (ReceiveChannel&lt;[I](bidi-stream.md)&gt;, SendChannel&lt;[O](bidi-stream.md)&gt;) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): Flow&lt;[O](bidi-stream.md)&gt; |
| [clientStream](client-stream.md) | [jvm]<br>suspend fun &lt;[I](client-stream.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [O](client-stream.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [clientStream](client-stream.md)(context: [CoroutineContext](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html), request: Flow&lt;[I](client-stream.md)&gt;, f: suspend (ReceiveChannel&lt;[I](client-stream.md)&gt;) -&gt; [O](client-stream.md)): [O](client-stream.md) |
| [serverStream](server-stream.md) | [jvm]<br>fun &lt;[I](server-stream.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [O](server-stream.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [serverStream](server-stream.md)(context: [CoroutineContext](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html), request: [I](server-stream.md), f: suspend ([I](server-stream.md), SendChannel&lt;[O](server-stream.md)&gt;) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): Flow&lt;[O](server-stream.md)&gt; |
