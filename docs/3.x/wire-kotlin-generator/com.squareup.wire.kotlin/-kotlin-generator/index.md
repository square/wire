//[wire-kotlin-generator](../../../index.md)/[com.squareup.wire.kotlin](../index.md)/[KotlinGenerator](index.md)

# KotlinGenerator

[jvm]\
class [KotlinGenerator](index.md)

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [generatedServiceName](generated-service-name.md) | [jvm]<br>fun [generatedServiceName](generated-service-name.md)(service: Service, rpc: Rpc? = null, isImplementation: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false): ClassName<br>Returns the full name of the class generated for [service](generated-service-name.md)#[rpc](generated-service-name.md). This returns a name like RouteGuideClient or RouteGuideGetFeatureBlockingServer. |
| [generatedTypeName](generated-type-name.md) | [jvm]<br>fun [generatedTypeName](generated-type-name.md)(member: ProtoMember): ClassName<br>Returns the full name of the class generated for [member](generated-type-name.md).<br>[jvm]<br>fun [generatedTypeName](generated-type-name.md)(type: Type): ClassName<br>Returns the full name of the class generated for [type](generated-type-name.md). |
| [generateGrpcServerAdapter](generate-grpc-server-adapter.md) | [jvm]<br>fun [generateGrpcServerAdapter](generate-grpc-server-adapter.md)(service: Service): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;ClassName, TypeSpec&gt;<br>Generates TypeSpecs for gRPC adapter for the given [service](generate-grpc-server-adapter.md). |
| [generateOptionType](generate-option-type.md) | [jvm]<br>fun [generateOptionType](generate-option-type.md)(extend: Extend, field: Field): TypeSpec?<br>Example |
| [generateServiceTypeSpecs](generate-service-type-specs.md) | [jvm]<br>fun [generateServiceTypeSpecs](generate-service-type-specs.md)(service: Service, onlyRpc: Rpc? = null): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;ClassName, TypeSpec&gt;<br>Generates all TypeSpecs for the given Service. |
| [generateType](generate-type.md) | [jvm]<br>fun [generateType](generate-type.md)(type: Type): TypeSpec |

## Properties

| Name | Summary |
|---|---|
| [schema](schema.md) | [jvm]<br>val [schema](schema.md): Schema |
