//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcStreamingCall](index.md)/[isExecuted](is-executed.md)

# isExecuted

[common]\
abstract fun [isExecuted](is-executed.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Returns true if [executeIn](execute-in.md) or [executeBlocking](execute-blocking.md) was called. It is an error to execute a call more than once.
