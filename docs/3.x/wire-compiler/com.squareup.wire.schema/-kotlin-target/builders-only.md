//[wire-compiler](../../../index.md)/[com.squareup.wire.schema](../index.md)/[KotlinTarget](index.md)/[buildersOnly](builders-only.md)

# buildersOnly

[jvm]\
val [buildersOnly](builders-only.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false

If true, the constructor of all generated types will be non-public, and they will be instantiable via their builders, regardless of the value of [javaInterop](java-interop.md).
