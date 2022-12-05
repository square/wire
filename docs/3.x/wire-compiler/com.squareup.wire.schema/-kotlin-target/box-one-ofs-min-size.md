//[wire-compiler](../../../index.md)/[com.squareup.wire.schema](../index.md)/[KotlinTarget](index.md)/[boxOneOfsMinSize](box-one-ofs-min-size.md)

# boxOneOfsMinSize

[jvm]\
val [boxOneOfsMinSize](box-one-ofs-min-size.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 5_000

If a oneof has more than or [boxOneOfsMinSize](box-one-ofs-min-size.md) fields, it will be generated using boxed oneofs as defined in OneOf.
