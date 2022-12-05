//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcCall](index.md)/[isExecuted](is-executed.md)

# isExecuted

[common]\
abstract fun [isExecuted](is-executed.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Returns true if [execute](execute.md), [executeBlocking](execute-blocking.md), or [enqueue](enqueue.md) was called. It is an error to execute or enqueue a call more than once.
