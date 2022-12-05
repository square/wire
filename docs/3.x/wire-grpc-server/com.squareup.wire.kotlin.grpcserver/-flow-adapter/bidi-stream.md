//[wire-grpc-server](../../../index.md)/[com.squareup.wire.kotlin.grpcserver](../index.md)/[FlowAdapter](index.md)/[bidiStream](bidi-stream.md)

# bidiStream

[jvm]\
fun &lt;[I](bidi-stream.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [O](bidi-stream.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [bidiStream](bidi-stream.md)(context: [CoroutineContext](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html), request: Flow&lt;[I](bidi-stream.md)&gt;, f: suspend (ReceiveChannel&lt;[I](bidi-stream.md)&gt;, SendChannel&lt;[O](bidi-stream.md)&gt;) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): Flow&lt;[O](bidi-stream.md)&gt;
