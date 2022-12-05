//[wire-grpc-server-generator](../../../index.md)/[com.squareup.wire.kotlin.grpcserver](../index.md)/[KotlinGrpcGenerator](index.md)

# KotlinGrpcGenerator

[jvm]\
class [KotlinGrpcGenerator](index.md)(typeToKotlinName: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;ProtoType, TypeName&gt;, singleMethodServices: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), suspendingCalls: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html))

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [generateGrpcServer](generate-grpc-server.md) | [jvm]<br>fun [generateGrpcServer](generate-grpc-server.md)(service: Service, protoFile: ProtoFile?, schema: Schema): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;ClassName, TypeSpec&gt; |
