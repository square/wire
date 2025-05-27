# Wire Binary Compatibility Plugin

The Wire Binary Compatibility Kotlin compiler plugin adapts Wire-generated callsites to be more resilient
to schema changes.

## Current Support
Generated constructor callsites are rewritten at compile-time to instead use the Builder.
Generated copy() callsites are not yet supported.

## Example
Given a generated class, Dinosaur, from a proto defintion:
```protobuf
package com.squareup.dinosaurs;

message Dinosaur {
  optional string name = 1;
  optional double avg_length_meters = 2;
}
```

With existing usage:
```kotlin
val newDinosaur = Dinosaur(
  name = "triceratops",
  avg_length_meters = 9.0,
)
```

When a new field is added to the Dinosaur schema, if there are competing versions of the compiled class and the new
version is resolved at runtime, the usage above may encounter an error:
```
java.lang.NoSuchMethodError: 'void com.squareup.dinosaurs.Dinosaur<init>(java.lang.String, java.lang.Double,
{new_field})'
```

Using the Wire Binary Compatibility Plugin will adapt the compiled binary to use the equivalent Builder:
```kotlin
val newDinosaur = Dinosaur.Builder()
  .name("triceratops")
  .avg_length_meters(9.0)
  .build()
```
The rewritten, compiled code is backwards compatible with the new field.
