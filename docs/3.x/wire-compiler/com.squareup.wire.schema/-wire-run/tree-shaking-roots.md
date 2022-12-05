//[wire-compiler](../../../index.md)/[com.squareup.wire.schema](../index.md)/[WireRun](index.md)/[treeShakingRoots](tree-shaking-roots.md)

# treeShakingRoots

[jvm]\
val [treeShakingRoots](tree-shaking-roots.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;

The roots of the schema model. Wire will prune the schema model to only include types in this list and the types transitively required by them.

If a member is included in this list then the enclosing type is included but its other members are not. For example, if squareup.dinosaurs.Dinosaur#name is in this list then the emitted source of the Dinosaur message will have the name field, but not the length_meters or mass_kilograms fields.
