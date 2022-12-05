//[wire-schema](../../../../index.md)/[com.squareup.wire.schema](../../index.md)/[SchemaHandler](../index.md)/[Context](index.md)/[claimedDefinitions](claimed-definitions.md)

# claimedDefinitions

[common]\
val [claimedDefinitions](claimed-definitions.md): [ClaimedDefinitions](../../-claimed-definitions/index.md)? = null

If set, the [SchemaHandler](../index.md) is to handle only types which are not claimed yet, and claim itself types it has handled. If null, the [SchemaHandler](../index.md) is to handle all types.
