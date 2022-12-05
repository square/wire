//[wire-kotlin-generator](../../../index.md)/[com.squareup.wire.kotlin](../index.md)/[KotlinGenerator](index.md)/[generateOptionType](generate-option-type.md)

# generateOptionType

[jvm]\
fun [generateOptionType](generate-option-type.md)(extend: Extend, field: Field): TypeSpec?

Example

@Retention(AnnotationRetention.RUNTIME)\
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)\
annotation class MyFieldOption(val value: String)
