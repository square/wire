//[wire-grpc-client](../../../../index.md)/[com.squareup.wire](../../index.md)/[GrpcClient](../index.md)/[Builder](index.md)

# Builder

[jvm]\
class [Builder](index.md)

## Functions

| Name | Summary |
|---|---|
| [baseUrl](base-url.md) | [jvm]<br>fun [baseUrl](base-url.md)(baseUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [GrpcClient.Builder](index.md)<br>fun [baseUrl](base-url.md)(url: [GrpcHttpUrl](../../-grpc-http-url/index.md)): [GrpcClient.Builder](index.md) |
| [build](build.md) | [jvm]<br>fun [build](build.md)(): [GrpcClient](../index.md) |
| [callFactory](call-factory.md) | [jvm]<br>fun [callFactory](call-factory.md)(client: Call.Factory): [GrpcClient.Builder](index.md) |
| [client](client.md) | [jvm]<br>fun [client](client.md)(client: OkHttpClient): [GrpcClient.Builder](index.md) |
| [minMessageToCompress](min-message-to-compress.md) | [jvm]<br>fun [minMessageToCompress](min-message-to-compress.md)(bytes: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)): [GrpcClient.Builder](index.md)<br>Sets the minimum outbound message size (in bytes) that will be compressed. |
