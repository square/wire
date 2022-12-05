//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[MessageSink](index.md)

# MessageSink

[common]\
interface [MessageSink](index.md)&lt;in [T](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;

A writable stream of messages.

Typical implementations will immediately encode messages and enqueue them for transmission, such as for client-to-server or server-to-client networking. But this interface is not limited to 1-1 networking use cases and implementations may persist, broadcast, validate, or take any other action with the messages.

There is no flushing mechanism. Messages are flushed one-by-one as they are written. This minimizes latency at a potential cost of throughput.

On its own this offers no guarantees that messages are delivered. For example, a message may accepted by [write](write.md) could be lost due to a network partition or crash. It is the caller's responsibility to confirm delivery and to retransmit as necessary.

It is possible for a writer to saturate the transmission channel, such as when a writer writes faster than the corresponding reader can read. In such cases calls to [write](write.md) will block until there is capacity in the outbound channel. You may use this as a basic backpressure mechanism. You should ensure that such backpressure propagates to the originator of outbound messages.

Instances of this interface are not safe for concurrent use.

[js, native]\
interface [MessageSink](index.md)&lt;in [T](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;

[jvm]\
interface [MessageSink](index.md)&lt;in [T](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; : [Closeable](https://docs.oracle.com/javase/8/docs/api/java/io/Closeable.html)

## Functions

| Name | Summary |
|---|---|
| [cancel](cancel.md) | [common]<br>abstract fun [cancel](cancel.md)()<br>Truncate this stream abnormally. This attempts to signal to readers of this data that it is incomplete. Note that unlike some cancel methods this is not safe for concurrent use.<br>[js, jvm, native]<br>[js, jvm, native]<br>abstract fun [cancel](cancel.md)() |
| close | [common]<br>abstract fun [close](close.md)()<br>Terminate the stream and release its resources. If this has not been canceled this signals a normal completion of the stream.<br>[js, native, jvm]<br>[js, native]<br>abstract fun [close](close.md)()<br>[jvm]<br>abstract override fun [close](../-message-source/index.md#358956095%2FFunctions%2F1823866683)() |
| [write](write.md) | [common]<br>abstract fun [write](write.md)(message: [T](index.md))<br>Encode [message](write.md) to bytes and enqueue the bytes for delivery, waiting if necessary until the delivery channel has capacity for the encoded message.<br>[js, jvm, native]<br>[js, jvm, native]<br>abstract fun [write](write.md)(message: [T](index.md)) |
