//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcClient](index.md)/[create](create.md)

# create

[jvm]\
inline fun &lt;[T](create.md) : Service&gt; [create](create.md)(): [T](create.md)

Returns a [T](create.md) that makes gRPC calls using this client.

[jvm]\
fun &lt;[T](create.md) : Service&gt; [create](create.md)(service: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)&lt;[T](create.md)&gt;): [T](create.md)

Returns a [service](create.md) that makes gRPC calls using this client.
