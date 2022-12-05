//[wire-schema](../../../../index.md)/[com.squareup.wire.schema](../../index.md)/[SchemaHandler](../index.md)/[Context](index.md)/[errorCollector](error-collector.md)

# errorCollector

[common]\
val [errorCollector](error-collector.md): [ErrorCollector](../../-error-collector/index.md)

Object to be used by the [SchemaHandler](../index.md) to store errors. After all [SchemaHandler](../index.md)s are finished, Wire will throw an exception if any error are present inside the collector.
