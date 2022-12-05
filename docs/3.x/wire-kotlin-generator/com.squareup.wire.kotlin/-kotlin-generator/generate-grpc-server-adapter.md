//[wire-kotlin-generator](../../../index.md)/[com.squareup.wire.kotlin](../index.md)/[KotlinGenerator](index.md)/[generateGrpcServerAdapter](generate-grpc-server-adapter.md)

# generateGrpcServerAdapter

[jvm]\
fun [generateGrpcServerAdapter](generate-grpc-server-adapter.md)(service: Service): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;ClassName, TypeSpec&gt;

Generates TypeSpecs for gRPC adapter for the given [service](generate-grpc-server-adapter.md).

These adapters allow us to use Wire based gRPC as io.grpc.BindableService
