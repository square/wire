//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[MessageSource](index.md)

# MessageSource

[common]\
interface [MessageSource](index.md)&lt;out [T](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;

A readable stream of messages.

Typical implementations will receive messages recently transmitted from a peer, such as for server-to-client or client-to-server networking. But this implementation is not limited to such networking use cases and implementations may load messages from local storage or generate messages on demand.

Calls to [read](read.md) will block until a message becomes available. There is no mechanism to limit how long a specific [read](read.md) will wait, though implementations may be configured to fail if they consider a source to be unhealthy.

Readers should take care to keep up with the stream of messages. A reader that takes an excessive amount of time to process a message may cause their writer to back up and suffer queueing.

Instances of this interface are not safe for concurrent use.

[js, native]\
interface [MessageSource](index.md)&lt;out [T](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;

[jvm]\
interface [MessageSource](index.md)&lt;out [T](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; : [Closeable](https://docs.oracle.com/javase/8/docs/api/java/io/Closeable.html)

## Functions

| Name | Summary |
|---|---|
| close | [common, js, native, jvm]<br>[common, js, native]<br>abstract fun [close](close.md)()<br>[jvm]<br>abstract override fun [close](index.md#358956095%2FFunctions%2F1823866683)() |
| [read](read.md) | [common]<br>abstract fun [read](read.md)(): [T](index.md)?<br>Read the next length-prefixed message on the stream and return it. Returns null if there are no further messages on this stream.<br>[js, jvm, native]<br>[js, jvm, native]<br>abstract fun [read](read.md)(): [T](index.md)? |
