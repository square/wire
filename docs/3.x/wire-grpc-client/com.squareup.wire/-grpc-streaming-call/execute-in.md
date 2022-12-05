//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcStreamingCall](index.md)/[executeIn](execute-in.md)

# executeIn

[common]\
abstract fun [executeIn](execute-in.md)(scope: CoroutineScope): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;SendChannel&lt;[S](index.md)&gt;, ReceiveChannel&lt;[R](index.md)&gt;&gt;

Enqueues this call for execution and returns channels to send and receive the call's messages. This uses the Dispatchers.IO to transmit outbound messages.
