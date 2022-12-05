//[wire-runtime](../../../../index.md)/[com.squareup.wire](../../index.md)/[OneOf](../index.md)/[Key](index.md)

# Key

[common]\
abstract class [Key](index.md)&lt;[T](index.md)&gt;(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), adapter: [ProtoAdapter](../../-proto-adapter/index.md)&lt;[T](index.md)&gt;, declaredName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), redacted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), jsonName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Identifies a field in a OneOf. Typically subclasses are generated by the Wire compiler and instances are declared as members of the referencing message class.

Note that the field's type may not uniquely identify the field. For example:

message ContactAddress {\
  string name = 1;\
\
  oneof address {\
    string sms_number = 2;\
    string email_address = 3;\
  }\
}

## Constructors

| | |
|---|---|
| [Key](-key.md) | [common]<br>fun &lt;[T](index.md)&gt; [Key](-key.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), adapter: [ProtoAdapter](../../-proto-adapter/index.md)&lt;[T](index.md)&gt;, declaredName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), redacted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, jsonName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "") |

## Properties

| Name | Summary |
|---|---|
| [adapter](adapter.md) | [common]<br>val [adapter](adapter.md): [ProtoAdapter](../../-proto-adapter/index.md)&lt;[T](index.md)&gt; |
| [declaredName](declared-name.md) | [common]<br>val [declaredName](declared-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [jsonName](json-name.md) | [common]<br>val [jsonName](json-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [redacted](redacted.md) | [common]<br>val [redacted](redacted.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [tag](tag.md) | [common]<br>val [tag](tag.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |