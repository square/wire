//[wire-java-generator](../../../index.md)/[com.squareup.wire.java](../index.md)/[JavaGenerator](index.md)/[typeName](type-name.md)

# typeName

[jvm]\
open fun [typeName](type-name.md)(protoType: ProtoType): TypeName

Returns the Java type for protoType.

## Throws

| | |
|---|---|
| [java.lang.IllegalArgumentException](https://docs.oracle.com/javase/8/docs/api/java/lang/IllegalArgumentException.html) | if there is no known Java type for protoType, such as if that type wasn't in this generator's schema. |
