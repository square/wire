//[wire-grpc-server](../../../index.md)/[com.squareup.wire.kotlin.grpcserver](../index.md)/[FlowAdapter](index.md)/[clientStream](client-stream.md)

# clientStream

[jvm]\
suspend fun &lt;[I](client-stream.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [O](client-stream.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [clientStream](client-stream.md)(context: [CoroutineContext](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html), request: Flow&lt;[I](client-stream.md)&gt;, f: suspend (ReceiveChannel&lt;[I](client-stream.md)&gt;) -&gt; [O](client-stream.md)): [O](client-stream.md)
