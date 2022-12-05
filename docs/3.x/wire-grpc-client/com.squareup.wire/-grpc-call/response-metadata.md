//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcCall](index.md)/[responseMetadata](response-metadata.md)

# responseMetadata

[common]\
abstract val [responseMetadata](response-metadata.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;?

A map containing response metadata. This is null until the call has executed, at which point it will be non-null if the call completed successfully. It may also be non-null in failure cases if the failure was not a problem of connectivity. For example, if the gRPC call fails with an HTTP 503 error, response metadata will be present.
