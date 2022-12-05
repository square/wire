//[wire-gradle-plugin](../../../index.md)/[com.squareup.wire.gradle](../index.md)/[CustomOutput](index.md)

# CustomOutput

[jvm]\
open class [CustomOutput](index.md)@Injectconstructor : [WireOutput](../-wire-output/index.md)

## Functions

| Name | Summary |
|---|---|
| [toTarget](to-target.md) | [jvm]<br>open override fun [toTarget](to-target.md)(outputDirectory: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): CustomTarget<br>Transforms this [WireOutput](../-wire-output/index.md) into a Target for which Wire will generate code. The Target should use [outputDirectory](to-target.md) instead of [WireOutput.out](../-wire-output/--out--.md) in all cases for its output directory. |

## Properties

| Name | Summary |
|---|---|
| [excludes](excludes.md) | [jvm]<br>var [excludes](excludes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null |
| [exclusive](exclusive.md) | [jvm]<br>var [exclusive](exclusive.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true |
| [includes](includes.md) | [jvm]<br>var [includes](includes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null |
| [out](../-wire-output/--out--.md) | [jvm]<br>var [out](../-wire-output/--out--.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null<br>Set this to override the default output directory for this [WireOutput](../-wire-output/index.md). |
| [schemaHandlerFactory](schema-handler-factory.md) | [jvm]<br>var [schemaHandlerFactory](schema-handler-factory.md): SchemaHandler.Factory? = null<br>Assign the schema handler factory instance. |
| [schemaHandlerFactoryClass](schema-handler-factory-class.md) | [jvm]<br>var [schemaHandlerFactoryClass](schema-handler-factory-class.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null<br>Assign the schema handler factory by name. If you use a class name, that class must have a no-arguments constructor. |
