//[wire-kotlin-generator](../../../index.md)/[com.squareup.wire.kotlin](../index.md)/[KotlinGenerator](index.md)/[generateServiceTypeSpecs](generate-service-type-specs.md)

# generateServiceTypeSpecs

[jvm]\
fun [generateServiceTypeSpecs](generate-service-type-specs.md)(service: Service, onlyRpc: Rpc? = null): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;ClassName, TypeSpec&gt;

Generates all TypeSpecs for the given Service.

If [onlyRpc](generate-service-type-specs.md) isn't null, this will generate code only for this onlyRpc; otherwise, all RPCs of the [service](generate-service-type-specs.md) will be code generated.
