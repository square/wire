//[wire-schema](../../../index.md)/[com.squareup.wire](../index.md)/[WireLogger](index.md)/[artifactSkipped](artifact-skipped.md)

# artifactSkipped

[common]\
abstract fun [artifactSkipped](artifact-skipped.md)(type: [ProtoType](../../com.squareup.wire.schema/-proto-type/index.md), targetName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

This is called when an artifact has been passed down to a com.squareup.wire.schema.Target.SchemaHandler but has been skipped. This is useful for dry-runs.

## Parameters

common

| | |
|---|---|
| type | is the unique identifier for the skipped type. |
| targetName | is used to identify the concerned target. For com.squareup.wire.schema.JavaTarget, the name will be "Java". For com.squareup.wire.schema.KotlinTarget, the name will be "Kotlin". For com.squareup.wire.schema.SwiftTarget, the name will be "Swift". For com.squareup.wire.schema.ProtoTarget, the name will be "Proto". |
