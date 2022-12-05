//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[WireRpc](index.md)

# WireRpc

[common]\
@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.FUNCTION](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-f-u-n-c-t-i-o-n/index.html)])

annotation class [WireRpc](index.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestAdapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), responseAdapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), sourceFile: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

For gRPC actions the path is formatted as /&lt;service name&gt;/&lt;method name&gt;. The path of the proto service below is /squareup.helloworld.Greeter/SayHello.

package squareup.helloworld;\
\
service Greeter {\
  rpc SayHello (HelloRequest) returns (HelloReply) {}\
}

## Constructors

| | |
|---|---|
| [WireRpc](-wire-rpc.md) | [common]<br>fun [WireRpc](-wire-rpc.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestAdapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), responseAdapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), sourceFile: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "") |

## Properties

| Name | Summary |
|---|---|
| [path](path.md) | [common]<br>val [path](path.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [requestAdapter](request-adapter.md) | [common]<br>val [requestAdapter](request-adapter.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [responseAdapter](response-adapter.md) | [common]<br>val [responseAdapter](response-adapter.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [sourceFile](source-file.md) | [common]<br>val [sourceFile](source-file.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
