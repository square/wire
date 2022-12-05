//[wire-grpc-client](../../../../index.md)/[com.squareup.wire](../../index.md)/[GrpcClient](../index.md)/[Builder](index.md)/[minMessageToCompress](min-message-to-compress.md)

# minMessageToCompress

[jvm]\
fun [minMessageToCompress](min-message-to-compress.md)(bytes: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)): [GrpcClient.Builder](index.md)

Sets the minimum outbound message size (in bytes) that will be compressed.

Set this to 0 to enable compression for all outbound messages. Set to [Long.MAX_VALUE](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/-m-a-x_-v-a-l-u-e.html) to disable compression.

This is 0 by default.
