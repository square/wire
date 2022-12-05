//[wire-grpc-server](../../../index.md)/[com.squareup.wire.kotlin.grpcserver](../index.md)/[FlowAdapter](index.md)/[serverStream](server-stream.md)

# serverStream

[jvm]\
fun &lt;[I](server-stream.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [O](server-stream.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [serverStream](server-stream.md)(context: [CoroutineContext](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html), request: [I](server-stream.md), f: suspend ([I](server-stream.md), SendChannel&lt;[O](server-stream.md)&gt;) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): Flow&lt;[O](server-stream.md)&gt;
