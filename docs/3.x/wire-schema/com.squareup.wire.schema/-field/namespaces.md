//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Field](index.md)/[namespaces](namespaces.md)

# namespaces

[common]\
val [namespaces](namespaces.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;

The namespaces in which the field is defined. For top-level extensions in a file that has no package declaration, this may be empty. For normal fields and extensions nested inside a message, the first entry will always be the package name, which might be the empty string if defined in a file that has no package declaration. Subsequent entries will be the names of enclosing messages, outer-most to inner-most.
