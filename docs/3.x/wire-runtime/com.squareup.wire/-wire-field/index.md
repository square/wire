//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[WireField](index.md)

# WireField

[common]\
@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.FIELD](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-f-i-e-l-d/index.html)])

annotation class [WireField](index.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), keyAdapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), adapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), label: [WireField.Label](-label/index.md), redacted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), declaredName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), jsonName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), oneofName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Annotates generated [Message](../-message/index.md) fields with metadata for serialization and deserialization.

## Constructors

| | |
|---|---|
| [WireField](-wire-field.md) | [common]<br>fun [WireField](-wire-field.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), keyAdapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "", adapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), label: [WireField.Label](-label/index.md) = Label.OPTIONAL, redacted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, declaredName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "", jsonName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "", oneofName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "") |

## Types

| Name | Summary |
|---|---|
| [Label](-label/index.md) | [common]<br>enum [Label](-label/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[WireField.Label](-label/index.md)&gt; <br>A protocol buffer label. |

## Properties

| Name | Summary |
|---|---|
| [adapter](adapter.md) | [common]<br>val [adapter](adapter.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Reference to the static field that holds a [ProtoAdapter](../-proto-adapter/index.md) that can encode and decode this field's values. The reference is a string like com.squareup.wire.protos.person.Person#ADAPTER and contains a fully-qualified class name followed by a hash symbol and a field name. |
| [declaredName](declared-name.md) | [common]<br>val [declaredName](declared-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Name of this field as declared in the proto schema. This value is set to a non-empty string only when the declared name differs from the generated one; for instance, a proto field named final generated in Java will be renamed to final_. |
| [jsonName](json-name.md) | [common]<br>val [jsonName](json-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Name representing this field as it should be used in JSON. This value is set to a non-empty string only when the json name differs from the name as declared in the proto schema. |
| [keyAdapter](key-adapter.md) | [common]<br>val [keyAdapter](key-adapter.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Reference to the static field that holds a [ProtoAdapter](../-proto-adapter/index.md) that can encode and decode this field's keys. This only applies to maps. See [adapter](adapter.md) for a description of the value format. |
| [label](label.md) | [common]<br>val [label](label.md): [WireField.Label](-label/index.md)<br>The field's protocol buffer label, one of [Label.OPTIONAL](-label/-o-p-t-i-o-n-a-l/index.md), [Label.REQUIRED](-label/-r-e-q-u-i-r-e-d/index.md), [Label.REPEATED](-label/-r-e-p-e-a-t-e-d/index.md), [Label.PACKED](-label/-p-a-c-k-e-d/index.md), or [Label.OMIT_IDENTITY](-label/-o-m-i-t_-i-d-e-n-t-i-t-y/index.md). Defaults to [Label.OPTIONAL](-label/-o-p-t-i-o-n-a-l/index.md). |
| [oneofName](oneof-name.md) | [common]<br>val [oneofName](oneof-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Name of the oneof this field belongs to. This value is set to a non-empty string only if the field is part of a oneof. |
| [redacted](redacted.md) | [common]<br>val [redacted](redacted.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false<br>Redacted fields are omitted from toString() to protect sensitive data. Defaults to false. |
| [tag](tag.md) | [common]<br>val [tag](tag.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>The tag number used to store the field's value. |
