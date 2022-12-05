//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcStreamingCall](index.md)/[executeBlocking](execute-blocking.md)

# executeBlocking

[common]\
abstract fun [executeBlocking](execute-blocking.md)(): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;MessageSink&lt;[S](index.md)&gt;, MessageSource&lt;[R](index.md)&gt;&gt;

Enqueues this call for execution and returns streams to send and receive the call's messages. Reads and writes on the returned streams are blocking.
