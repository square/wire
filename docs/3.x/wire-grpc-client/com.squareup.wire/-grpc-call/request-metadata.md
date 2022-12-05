//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcCall](index.md)/[requestMetadata](request-metadata.md)

# requestMetadata

[common]\
abstract var [requestMetadata](request-metadata.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;

A map containing request metadata. This is initially empty; it can be assigned to any other map of metadata before the call is executed. It is an error to set this value after the call is executed.
