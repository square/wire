//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcCall](index.md)/[enqueue](enqueue.md)

# enqueue

[common]\
abstract fun [enqueue](enqueue.md)(request: [S](index.md), callback: [GrpcCall.Callback](-callback/index.md)&lt;[S](index.md), [R](index.md)&gt;)

Enqueues this call for asynchronous execution. The [callback](enqueue.md) will be invoked on the client's dispatcher thread when the call completes.
