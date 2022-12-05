//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcResponse](index.md)

# GrpcResponse

[common, js, native]\
class [GrpcResponse](index.md)

[jvm]\
typealias [GrpcResponse](index.md) = Response

## Functions

| Name | Summary |
|---|---|
| [close](close.md) | [common, js, native]<br>[common, js, native]<br>fun [close](close.md)() |
| [header](header.md) | [common, js, native]<br>[common]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [header](header.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), defaultValue: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>[js, native]<br>fun [header](header.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), defaultValue: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [trailers](trailers.md) | [common, js, native]<br>[common, js, native]<br>fun [trailers](trailers.md)(): [GrpcHeaders](../-grpc-headers/index.md) |

## Properties

| Name | Summary |
|---|---|
| [body](body.md) | [common]<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "body")<br>val [body](body.md): [GrpcResponseBody](../-grpc-response-body/index.md)?<br>[js, native]<br>val [body](body.md): [GrpcResponseBody](../-grpc-response-body/index.md)? |
