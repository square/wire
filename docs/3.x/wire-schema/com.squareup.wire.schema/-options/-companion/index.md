//[wire-schema](../../../../index.md)/[com.squareup.wire.schema](../../index.md)/[Options](../index.md)/[Companion](index.md)

# Companion

[common]\
object [Companion](index.md)

## Functions

| Name | Summary |
|---|---|
| [resolveFieldPath](resolve-field-path.md) | [common]<br>fun [resolveFieldPath](resolve-field-path.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), fullyQualifiedNames: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?&gt;): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;?<br>Given a path like a.b.c.d and a set of paths like {a.b.c, a.f.g, h.j}, this returns the original path split on dots such that the first element is in the set. For the above example it would return the array [a.b.c, d]. |

## Properties

| Name | Summary |
|---|---|
| [ENUM_OPTIONS](-e-n-u-m_-o-p-t-i-o-n-s.md) | [common]<br>@[JvmField](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/index.html)<br>val [ENUM_OPTIONS](-e-n-u-m_-o-p-t-i-o-n-s.md): [ProtoType](../../-proto-type/index.md) |
| [ENUM_VALUE_OPTIONS](-e-n-u-m_-v-a-l-u-e_-o-p-t-i-o-n-s.md) | [common]<br>@[JvmField](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/index.html)<br>val [ENUM_VALUE_OPTIONS](-e-n-u-m_-v-a-l-u-e_-o-p-t-i-o-n-s.md): [ProtoType](../../-proto-type/index.md) |
| [FIELD_OPTIONS](-f-i-e-l-d_-o-p-t-i-o-n-s.md) | [common]<br>@[JvmField](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/index.html)<br>val [FIELD_OPTIONS](-f-i-e-l-d_-o-p-t-i-o-n-s.md): [ProtoType](../../-proto-type/index.md) |
| [FILE_OPTIONS](-f-i-l-e_-o-p-t-i-o-n-s.md) | [common]<br>@[JvmField](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/index.html)<br>val [FILE_OPTIONS](-f-i-l-e_-o-p-t-i-o-n-s.md): [ProtoType](../../-proto-type/index.md) |
| [GOOGLE_PROTOBUF_OPTION_TYPES](-g-o-o-g-l-e_-p-r-o-t-o-b-u-f_-o-p-t-i-o-n_-t-y-p-e-s.md) | [common]<br>val [GOOGLE_PROTOBUF_OPTION_TYPES](-g-o-o-g-l-e_-p-r-o-t-o-b-u-f_-o-p-t-i-o-n_-t-y-p-e-s.md): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[ProtoType](../../-proto-type/index.md)&gt; |
| [MESSAGE_OPTIONS](-m-e-s-s-a-g-e_-o-p-t-i-o-n-s.md) | [common]<br>@[JvmField](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/index.html)<br>val [MESSAGE_OPTIONS](-m-e-s-s-a-g-e_-o-p-t-i-o-n-s.md): [ProtoType](../../-proto-type/index.md) |
| [METHOD_OPTIONS](-m-e-t-h-o-d_-o-p-t-i-o-n-s.md) | [common]<br>@[JvmField](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/index.html)<br>val [METHOD_OPTIONS](-m-e-t-h-o-d_-o-p-t-i-o-n-s.md): [ProtoType](../../-proto-type/index.md) |
| [ONEOF_OPTIONS](-o-n-e-o-f_-o-p-t-i-o-n-s.md) | [common]<br>@[JvmField](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/index.html)<br>val [ONEOF_OPTIONS](-o-n-e-o-f_-o-p-t-i-o-n-s.md): [ProtoType](../../-proto-type/index.md) |
| [SERVICE_OPTIONS](-s-e-r-v-i-c-e_-o-p-t-i-o-n-s.md) | [common]<br>@[JvmField](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/index.html)<br>val [SERVICE_OPTIONS](-s-e-r-v-i-c-e_-o-p-t-i-o-n-s.md): [ProtoType](../../-proto-type/index.md) |
