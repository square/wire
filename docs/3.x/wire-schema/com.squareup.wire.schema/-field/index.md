//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Field](index.md)

# Field

[common]\
data class [Field](index.md)(namespaces: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, location: [Location](../-location/index.md), label: [Field.Label](-label/index.md)?, name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), default: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, elementType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), options: [Options](../-options/index.md), isExtension: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), isOneOf: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), declaredJsonName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?)

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |
| [EncodeMode](-encode-mode/index.md) | [common]<br>enum [EncodeMode](-encode-mode/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[Field.EncodeMode](-encode-mode/index.md)&gt; |
| [Label](-label/index.md) | [common]<br>enum [Label](-label/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[Field.Label](-label/index.md)&gt; |

## Functions

| Name | Summary |
|---|---|
| [link](link.md) | [common]<br>fun [link](link.md)(linker: [Linker](../-linker/index.md)) |
| [linkOptions](link-options.md) | [common]<br>fun [linkOptions](link-options.md)(linker: [Linker](../-linker/index.md), syntaxRules: [SyntaxRules](../-syntax-rules/index.md), validate: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [retainAll](retain-all.md) | [common]<br>fun [retainAll](retain-all.md)(schema: [Schema](../-schema/index.md), markSet: [MarkSet](../-mark-set/index.md), enclosingType: [ProtoType](../-proto-type/index.md)): [Field](index.md)? |
| [toString](to-string.md) | [common]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [validate](validate.md) | [common]<br>fun [validate](validate.md)(linker: [Linker](../-linker/index.md), syntaxRules: [SyntaxRules](../-syntax-rules/index.md)) |

## Properties

| Name | Summary |
|---|---|
| [declaredJsonName](declared-json-name.md) | [common]<br>val [declaredJsonName](declared-json-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [default](default.md) | [common]<br>val [default](default.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [documentation](documentation.md) | [common]<br>val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [encodeMode](encode-mode.md) | [common]<br>var [encodeMode](encode-mode.md): [Field.EncodeMode](-encode-mode/index.md)? = null |
| [isDeprecated](is-deprecated.md) | [common]<br>val [isDeprecated](is-deprecated.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isExtension](is-extension.md) | [common]<br>val [isExtension](is-extension.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isOneOf](is-one-of.md) | [common]<br>val [isOneOf](is-one-of.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isPacked](is-packed.md) | [common]<br>val [isPacked](is-packed.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isRedacted](is-redacted.md) | [common]<br>var [isRedacted](is-redacted.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [isRepeated](is-repeated.md) | [common]<br>val [isRepeated](is-repeated.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isRequired](is-required.md) | [common]<br>val [isRequired](is-required.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [jsonName](json-name.md) | [common]<br>var [jsonName](json-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [label](label.md) | [common]<br>val [label](label.md): [Field.Label](-label/index.md)?<br>May be null for proto3 fields, one-of's, or maps. |
| [location](location.md) | [common]<br>val [location](location.md): [Location](../-location/index.md) |
| [name](name.md) | [common]<br>val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [namespaces](namespaces.md) | [common]<br>val [namespaces](namespaces.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>The namespaces in which the field is defined. For top-level extensions in a file that has no package declaration, this may be empty. For normal fields and extensions nested inside a message, the first entry will always be the package name, which might be the empty string if defined in a file that has no package declaration. Subsequent entries will be the names of enclosing messages, outer-most to inner-most. |
| [options](options.md) | [common]<br>val [options](options.md): [Options](../-options/index.md) |
| [packageName](package-name.md) | [common]<br>val [packageName](package-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Returns the package in which this field is defined. If the file that defined this field has no package declaration, returns the empty string. |
| [qualifiedName](qualified-name.md) | [common]<br>val [qualifiedName](qualified-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Returns this field's name, prefixed with its namespaces. Uniquely identifies extension fields, such as in options. |
| [tag](tag.md) | [common]<br>val [tag](tag.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [type](type.md) | [common]<br>var [type](type.md): [ProtoType](../-proto-type/index.md)? = null |
