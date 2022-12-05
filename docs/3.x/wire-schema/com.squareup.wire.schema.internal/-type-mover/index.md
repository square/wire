//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal](../index.md)/[TypeMover](index.md)

# TypeMover

[common]\
class [TypeMover](index.md)(oldSchema: [Schema](../../com.squareup.wire.schema/-schema/index.md), moves: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeMover.Move](-move/index.md)&gt;)

Refactor a schema by moving a proto type declaration.

This class attempts to avoid making unnecessary changes to the target schema. For example, it won't remove unused imports if they are unrelated to the types being moved.

## Constructors

| | |
|---|---|
| [TypeMover](-type-mover.md) | [common]<br>fun [TypeMover](-type-mover.md)(oldSchema: [Schema](../../com.squareup.wire.schema/-schema/index.md), moves: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeMover.Move](-move/index.md)&gt;) |

## Types

| Name | Summary |
|---|---|
| [Move](-move/index.md) | [common]<br>data class [Move](-move/index.md)(type: [ProtoType](../../com.squareup.wire.schema/-proto-type/index.md), targetPath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Functions

| Name | Summary |
|---|---|
| [move](move.md) | [common]<br>fun [move](move.md)(): [Schema](../../com.squareup.wire.schema/-schema/index.md) |
