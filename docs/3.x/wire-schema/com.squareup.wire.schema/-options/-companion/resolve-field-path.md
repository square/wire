//[wire-schema](../../../../index.md)/[com.squareup.wire.schema](../../index.md)/[Options](../index.md)/[Companion](index.md)/[resolveFieldPath](resolve-field-path.md)

# resolveFieldPath

[common]\
fun [resolveFieldPath](resolve-field-path.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), fullyQualifiedNames: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?&gt;): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;?

Given a path like a.b.c.d and a set of paths like {a.b.c, a.f.g, h.j}, this returns the original path split on dots such that the first element is in the set. For the above example it would return the array [a.b.c, d].

Typically the input path is a package name like a.b, followed by a dot and a sequence of field names. The first field name is an extension field; subsequent field names make a path within that extension.

https://developers.google.com/protocol-buffers/docs/overview?hl=en#packages_and_name_resolution Names can be prefixed with a . when the search should start from the outermost scope.

Note that a single input may yield multiple possible answers, such as when package names and field names collide. This method prefers shorter package names though that is an implementation detail.
