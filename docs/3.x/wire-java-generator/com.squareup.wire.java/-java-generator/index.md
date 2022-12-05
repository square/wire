//[wire-java-generator](../../../index.md)/[com.squareup.wire.java](../index.md)/[JavaGenerator](index.md)

# JavaGenerator

[jvm]\
class [JavaGenerator](index.md)

Generates Java source code that matches proto definitions. 

This can map type names from protocol buffers (like uint32, string, or squareup.protos.person.Person to the corresponding Java names (like int, java.lang.String, or com.squareup.protos.person.Person).

## Functions

| Name | Summary |
|---|---|
| [abstractAdapterName](abstract-adapter-name.md) | [jvm]<br>@[Nullable](https://docs.oracle.com/javase/8/docs/api/javax/annotation/Nullable.html)<br>open fun [abstractAdapterName](abstract-adapter-name.md)(protoType: ProtoType): ClassName<br>Returns the Java type of the abstract adapter class generated for a corresponding protoType. |
| [builtInType](built-in-type.md) | [jvm]<br>open fun [builtInType](built-in-type.md)(protoType: ProtoType): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [generateAdapterForCustomType](generate-adapter-for-custom-type.md) | [jvm]<br>open fun [generateAdapterForCustomType](generate-adapter-for-custom-type.md)(type: Type): TypeSpec<br>Returns a standalone adapter for type. |
| [generatedTypeName](generated-type-name.md) | [jvm]<br>open fun [generatedTypeName](generated-type-name.md)(member: ProtoMember): ClassName<br>Returns the full name of the class generated for member.<br>[jvm]<br>open fun [generatedTypeName](generated-type-name.md)(type: Type): ClassName<br>Returns the full name of the class generated for type. |
| [generateOptionType](generate-option-type.md) | [jvm]<br>@[Nullable](https://docs.oracle.com/javase/8/docs/api/javax/annotation/Nullable.html)<br>open fun [generateOptionType](generate-option-type.md)(extend: Extend, field: Field): TypeSpec |
| [generateType](generate-type.md) | [jvm]<br>open fun [generateType](generate-type.md)(type: Type): TypeSpec<br>Returns the generated code for type, which may be a top-level or a nested type. |
| [get](get.md) | [jvm]<br>open fun [get](get.md)(schema: Schema): [JavaGenerator](index.md) |
| [isEnum](is-enum.md) | [jvm]<br>open fun [isEnum](is-enum.md)(type: ProtoType): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [schema](schema.md) | [jvm]<br>open fun [schema](schema.md)(): Schema |
| [typeName](type-name.md) | [jvm]<br>open fun [typeName](type-name.md)(protoType: ProtoType): TypeName<br>Returns the Java type for protoType. |
| [withAndroid](with-android.md) | [jvm]<br>open fun [withAndroid](with-android.md)(emitAndroid: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [JavaGenerator](index.md) |
| [withAndroidAnnotations](with-android-annotations.md) | [jvm]<br>open fun [withAndroidAnnotations](with-android-annotations.md)(emitAndroidAnnotations: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [JavaGenerator](index.md) |
| [withBuildersOnly](with-builders-only.md) | [jvm]<br>open fun [withBuildersOnly](with-builders-only.md)(buildersOnly: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [JavaGenerator](index.md) |
| [withCompact](with-compact.md) | [jvm]<br>open fun [withCompact](with-compact.md)(emitCompact: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [JavaGenerator](index.md) |
| [withOptions](with-options.md) | [jvm]<br>open fun [withOptions](with-options.md)(emitDeclaredOptions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), emitAppliedOptions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [JavaGenerator](index.md) |
| [withProfile](with-profile.md) | [jvm]<br>open fun [withProfile](with-profile.md)(profile: Profile): [JavaGenerator](index.md) |
