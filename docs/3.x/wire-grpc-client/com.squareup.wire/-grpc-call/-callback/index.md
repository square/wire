//[wire-grpc-client](../../../../index.md)/[com.squareup.wire](../../index.md)/[GrpcCall](../index.md)/[Callback](index.md)

# Callback

[common]\
interface [Callback](index.md)&lt;[S](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [R](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;

## Functions

| Name | Summary |
|---|---|
| [onFailure](on-failure.md) | [common]<br>abstract fun [onFailure](on-failure.md)(call: [GrpcCall](../index.md)&lt;[S](index.md), [R](index.md)&gt;, exception: IOException) |
| [onSuccess](on-success.md) | [common]<br>abstract fun [onSuccess](on-success.md)(call: [GrpcCall](../index.md)&lt;[S](index.md), [R](index.md)&gt;, response: [R](index.md)) |
