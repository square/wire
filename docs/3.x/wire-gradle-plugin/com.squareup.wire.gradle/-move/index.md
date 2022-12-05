//[wire-gradle-plugin](../../../index.md)/[com.squareup.wire.gradle](../index.md)/[Move](index.md)

# Move

[jvm]\
open class [Move](index.md)@Injectconstructor : [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)

A directive to move a type to a new location and adjust all references to the type in this schema. Typically this is used with proto output to refactor a proto project.

wire {\
  move {\
    type = "squareup.geology.Period"\
    targetPath = "squareup/geology/geology.proto"\
  }\
\
  proto {}\
}

## Constructors

| | |
|---|---|
| [Move](-move.md) | [jvm]<br>@Inject<br>fun [Move](-move.md)() |

## Functions

| Name | Summary |
|---|---|
| [toTypeMoverMove](to-type-mover-move.md) | [jvm]<br>fun [toTypeMoverMove](to-type-mover-move.md)(): TypeMover.Move |

## Properties

| Name | Summary |
|---|---|
| [targetPath](target-path.md) | [jvm]<br>var [targetPath](target-path.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [type](type.md) | [jvm]<br>var [type](type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
