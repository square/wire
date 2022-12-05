//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcCall](index.md)

# GrpcCall

[common]\
interface [GrpcCall](index.md)&lt;[S](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [R](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;

A single call to a remote server. This call sends a single request value and receives a single response value. A gRPC call cannot be executed twice.

gRPC calls can be [suspending](execute.md), [blocking](execute-blocking.md), or [asynchronous](enqueue.md). Use whichever mechanism works at your call site: the bytes transmitted on the network are the same.

## Types

| Name | Summary |
|---|---|
| [Callback](-callback/index.md) | [common]<br>interface [Callback](-callback/index.md)&lt;[S](-callback/index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [R](-callback/index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; |

## Functions

| Name | Summary |
|---|---|
| [cancel](cancel.md) | [common]<br>abstract fun [cancel](cancel.md)()<br>Attempts to cancel the call. This function is safe to call concurrently with execution. When canceled, execution fails with an immediate IOException rather than waiting to complete normally. |
| [clone](clone.md) | [common]<br>abstract fun [clone](clone.md)(): [GrpcCall](index.md)&lt;[S](index.md), [R](index.md)&gt;<br>Create a new, identical gRPC call to this one which can be enqueued or executed even if this call has already been. |
| [enqueue](enqueue.md) | [common]<br>abstract fun [enqueue](enqueue.md)(request: [S](index.md), callback: [GrpcCall.Callback](-callback/index.md)&lt;[S](index.md), [R](index.md)&gt;)<br>Enqueues this call for asynchronous execution. The [callback](enqueue.md) will be invoked on the client's dispatcher thread when the call completes. |
| [execute](execute.md) | [common]<br>abstract suspend fun [execute](execute.md)(request: [S](index.md)): [R](index.md)<br>Invokes the call immediately and suspends until its response is received. |
| [executeBlocking](execute-blocking.md) | [common]<br>abstract fun [executeBlocking](execute-blocking.md)(request: [S](index.md)): [R](index.md)<br>Invokes the call immediately and blocks until its response is received. |
| [isCanceled](is-canceled.md) | [common]<br>abstract fun [isCanceled](is-canceled.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>True if [cancel](cancel.md) was called. |
| [isExecuted](is-executed.md) | [common]<br>abstract fun [isExecuted](is-executed.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if [execute](execute.md), [executeBlocking](execute-blocking.md), or [enqueue](enqueue.md) was called. It is an error to execute or enqueue a call more than once. |

## Properties

| Name | Summary |
|---|---|
| [method](method.md) | [common]<br>abstract val [method](method.md): [GrpcMethod](../-grpc-method/index.md)&lt;[S](index.md), [R](index.md)&gt;<br>The method invoked by this call. |
| [requestMetadata](request-metadata.md) | [common]<br>abstract var [requestMetadata](request-metadata.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>A map containing request metadata. This is initially empty; it can be assigned to any other map of metadata before the call is executed. It is an error to set this value after the call is executed. |
| [responseMetadata](response-metadata.md) | [common]<br>abstract val [responseMetadata](response-metadata.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;?<br>A map containing response metadata. This is null until the call has executed, at which point it will be non-null if the call completed successfully. It may also be non-null in failure cases if the failure was not a problem of connectivity. For example, if the gRPC call fails with an HTTP 503 error, response metadata will be present. |
| [timeout](timeout.md) | [common]<br>abstract val [timeout](timeout.md): Timeout<br>Configures how long the call can take to complete before it is automatically canceled. |
