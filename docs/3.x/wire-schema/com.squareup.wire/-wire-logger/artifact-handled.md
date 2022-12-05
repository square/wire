//[wire-schema](../../../index.md)/[com.squareup.wire](../index.md)/[WireLogger](index.md)/[artifactHandled](artifact-handled.md)

# artifactHandled

[common]\
abstract fun [artifactHandled](artifact-handled.md)(outputPath: Path, qualifiedName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), targetName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

This is called when an artifact is handled by a com.squareup.wire.schema.Target.SchemaHandler.

## Parameters

common

| | |
|---|---|
| outputPath | is the path where the artifact is written on disk. |
| qualifiedName | is the file path when generating a .proto file, the type or service name prefixed with its package name when generating a .java or .kt file, and the type name when generating a .swift file. |
| targetName | is used to identify the concerned target. For com.squareup.wire.schema.JavaTarget, the name will be "Java". For com.squareup.wire.schema.KotlinTarget, the name will be "Kotlin". For com.squareup.wire.schema.SwiftTarget, the name will be "Swift". For com.squareup.wire.schema.ProtoTarget, the name will be "Proto". |
