//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcStreamingCall](index.md)

# GrpcStreamingCall

[common]\
interface [GrpcStreamingCall](index.md)&lt;[S](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [R](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;

A single streaming call to a remote server. This class handles three streaming call types:

<ul><li>Single request, streaming response. The send channel or message sink accept exactly one     message. The receive channel or message source produce zero or more messages. The outbound     request message is sent before any inbound response messages.</li><li>Streaming request, single response. The send channel or message sink accept zero or more     messages. The receive channel or message source produce exactly one message. All outbound     request messages are sent before the inbound response message.</li><li>Streaming request, streaming response. The send channel or message sink accept zero or more     messages, and the receive channel or message source produce any number of messages. Unlike     the above two types, you are free to interleave request and response messages.</li></ul>

A gRPC call cannot be executed twice.

gRPC calls can be [suspending](execute-in.md) or [blocking](execute-blocking.md). Use whichever mechanism works at your call site: the bytes transmitted on the network are the same.

## Functions

| Name | Summary |
|---|---|
| [cancel](cancel.md) | [common]<br>abstract fun [cancel](cancel.md)()<br>Attempts to cancel the call. This function is safe to call concurrently with execution. When canceled, execution fails with an immediate IOException rather than waiting to complete normally. |
| [clone](clone.md) | [common]<br>abstract fun [clone](clone.md)(): [GrpcStreamingCall](index.md)&lt;[S](index.md), [R](index.md)&gt;<br>Create a new, identical gRPC call to this one which can be enqueued or executed even if this call has already been. |
| [executeBlocking](execute-blocking.md) | [common]<br>abstract fun [executeBlocking](execute-blocking.md)(): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;MessageSink&lt;[S](index.md)&gt;, MessageSource&lt;[R](index.md)&gt;&gt;<br>Enqueues this call for execution and returns streams to send and receive the call's messages. Reads and writes on the returned streams are blocking. |
| [executeIn](execute-in.md) | [common]<br>abstract fun [executeIn](execute-in.md)(scope: CoroutineScope): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;SendChannel&lt;[S](index.md)&gt;, ReceiveChannel&lt;[R](index.md)&gt;&gt;<br>Enqueues this call for execution and returns channels to send and receive the call's messages. This uses the Dispatchers.IO to transmit outbound messages. |
| [isCanceled](is-canceled.md) | [common]<br>abstract fun [isCanceled](is-canceled.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>True if [cancel](cancel.md) was called. |
| [isExecuted](is-executed.md) | [common]<br>abstract fun [isExecuted](is-executed.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if [executeIn](execute-in.md) or [executeBlocking](execute-blocking.md) was called. It is an error to execute a call more than once. |

## Properties

| Name | Summary |
|---|---|
| [method](method.md) | [common]<br>abstract val [method](method.md): [GrpcMethod](../-grpc-method/index.md)&lt;[S](index.md), [R](index.md)&gt;<br>The method invoked by this call. |
| [requestMetadata](request-metadata.md) | [common]<br>abstract var [requestMetadata](request-metadata.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>A map containing request metadata. This is initially empty; it can be assigned to any other map of metadata before the call is executed. It is an error to set this value after the call is executed. |
| [responseMetadata](response-metadata.md) | [common]<br>abstract val [responseMetadata](response-metadata.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;?<br>A map containing response metadata. This is null until the call has executed, at which point it will be non-null if the call completed successfully. It may also be non-null in failure cases if the failure was not a problem of connectivity. For example, if the gRPC call fails with an HTTP 503 error, response metadata will be present. |
| [timeout](timeout.md) | [common]<br>abstract val [timeout](timeout.md): Timeout<br>Configures how long the call can take to complete before it is automatically canceled. The timeout applies to the full set of messages transmitted. For long-running streams you must configure a sufficiently long timeout. |
