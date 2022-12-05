//[wire-gradle-plugin](../../../index.md)/[com.squareup.wire.gradle](../index.md)/[KotlinOutput](index.md)

# KotlinOutput

[jvm]\
open class [KotlinOutput](index.md)@Injectconstructor : [WireOutput](../-wire-output/index.md)

## Functions

| Name | Summary |
|---|---|
| [toTarget](to-target.md) | [jvm]<br>open override fun [toTarget](to-target.md)(outputDirectory: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): KotlinTarget<br>Transforms this [WireOutput](../-wire-output/index.md) into a Target for which Wire will generate code. The Target should use [outputDirectory](to-target.md) instead of [WireOutput.out](../-wire-output/--out--.md) in all cases for its output directory. |

## Properties

| Name | Summary |
|---|---|
| [android](android.md) | [jvm]<br>var [android](android.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [boxOneOfsMinSize](box-one-ofs-min-size.md) | [jvm]<br>var [boxOneOfsMinSize](box-one-ofs-min-size.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 5_000 |
| [buildersOnly](builders-only.md) | [jvm]<br>var [buildersOnly](builders-only.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [emitAppliedOptions](emit-applied-options.md) | [jvm]<br>var [emitAppliedOptions](emit-applied-options.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true |
| [emitDeclaredOptions](emit-declared-options.md) | [jvm]<br>var [emitDeclaredOptions](emit-declared-options.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true |
| [excludes](excludes.md) | [jvm]<br>var [excludes](excludes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null |
| [exclusive](exclusive.md) | [jvm]<br>var [exclusive](exclusive.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true |
| [grpcServerCompatible](grpc-server-compatible.md) | [jvm]<br>var [grpcServerCompatible](grpc-server-compatible.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [includes](includes.md) | [jvm]<br>var [includes](includes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null |
| [javaInterop](java-interop.md) | [jvm]<br>var [javaInterop](java-interop.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [nameSuffix](name-suffix.md) | [jvm]<br>var [nameSuffix](name-suffix.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [out](../-wire-output/--out--.md) | [jvm]<br>var [out](../-wire-output/--out--.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null<br>Set this to override the default output directory for this [WireOutput](../-wire-output/index.md). |
| [rpcCallStyle](rpc-call-style.md) | [jvm]<br>var [rpcCallStyle](rpc-call-style.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [rpcRole](rpc-role.md) | [jvm]<br>var [rpcRole](rpc-role.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [singleMethodServices](single-method-services.md) | [jvm]<br>var [singleMethodServices](single-method-services.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
