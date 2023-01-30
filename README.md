Wire
====

*“A man got to have a code!”* - Omar Little

See the [project website][wire] for documentation and APIs.

As our teams and programs grow, the variety and volume of data also grows. Success will turn your
simple data models into complex ones! Whether your application is storing data to disk or
transmitting it over a network, the structure and interpretation of that data should be clear.
Consumers work best with data they understand!

Schemas describe and document data models. If you have data, you should have a schema.

Protocol Buffers
----------------

[Google's Protocol Buffers][google_protos] are built around a great schema language:

 * It's cross platform and language independent. Whatever programming language you use, you'll be
   able to use proto schemas with your application.

 * Proto schemas are backwards-compatible and future-proof. You can evolve your schema as your
   application loses old features and gains new ones.

 * It's focused. Proto schemas describe your data models. That's it.

<details>
<summary>Protocol Buffer Examples</summary>

Here's a [sample message][dinosaur_proto] definition:

```proto
syntax = "proto3";

package squareup.dinosaurs;

option java_package = "com.squareup.dinosaurs";

import "squareup/geology/period.proto";

message Dinosaur {
  // Common name of this dinosaur, like "Stegosaurus".
  string name = 1;

  // URLs with images of this dinosaur.
  repeated string picture_urls = 2;

  squareup.geology.Period period = 5;
}
```

And here's an [enum][period_proto] definition:

```proto
syntax = "proto3";

package squareup.geology;


option java_package = "com.squareup.geology";

enum Period {
  // 145.5 million years ago — 66.0 million years ago.
  CRETACEOUS = 0;

  // 201.3 million years ago — 145.0 million years ago.
  JURASSIC = 1;

  // 252.17 million years ago — 201.3 million years ago.
  TRIASSIC = 2;
}
```

This schema language is Protocol Buffers' best feature. You might even use it purely for
documentation purposes, such as to describe a JSON API.

Protocol Buffers also defines a compact binary encoding of messages that conform to the schema. This
encoding is fast to encode, fast to decode, small to transmit, and small to store. The binary
encoding uses numeric tags from the schema, like the `5` for `period` above.

For example, let's encode this dinosaur:

```
{
  name: "Stegosaurus",
  period: JURASSIC
}
```

The encoded value is just 15 bytes:

```
Hex  Description
 0a  tag: name(1), field encoding: LENGTH_DELIMITED(2). 1 << 3 | 2
 0b  "Stegosaurus".length()
 53  'S'
 74  't'
 65  'e'
 67  'g'
 6f  'o'
 73  's'
 61  'a'
 75  'u'
 72  'r'
 75  'u'
 73  's'
 28  tag: period(5), field encoding: VARINT(0). 5 << 3 | 0
 02  JURASSIC(2)
```
</details>

Why Wire?
---------

The Protocol Buffers schema language and binary encoding are both defined by Google. Wire is an
independent implementation from Square that's specifically designed for Android and Java.

For each message type defined in the schema, Wire generates an immutable model class and its
builder. The generated code looks like code you'd write by hand: it's documented, formatted, and
simple. Wire's APIs should feel at home to programmers who like [Effective Java][effective_java].

That said, there are some interesting design decisions in Wire:

 * Wire messages declare `public final` fields instead of the usual getter methods. This cuts down
   on both code generated and code executed. Less code is particularly beneficial for Android
   programs.

 * Wire avoids case mapping. A field declared as `picture_urls` in a schema yields a Java field
   `picture_urls` and not the conventional `pictureUrls` camel case. Though the name feels awkward
   at first, it's fantastic whenever you use `grep` or more sophisticated search tools. No more
   mapping when navigating between schema, Java source code, and data. It also provides a gentle
   reminder to calling code that proto messages are a bit special.

 * Primitive types are always boxed. If a field is absent, its value is `null`. This is used for
   naturally optional fields, such as a dinosaur whose period is unknown. A field may also be null
   due to schema evolution: if tomorrow we add a `carnivore` boolean to our message definition,
   today's data won’t have a value for that field.

<details>
<summary>Generated Java Code</summary>

Here's the compact [generated code][dinosaur_java] for the `Dinosaur` message defined above:

```java
// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.dinosaurs.Dinosaur in squareup/dinosaurs/dinosaur.proto
package com.squareup.dinosaurs;

import com.squareup.geology.Period;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.Syntax;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import okio.ByteString;

public final class Dinosaur extends Message<Dinosaur, Dinosaur.Builder> {
  public static final ProtoAdapter<Dinosaur> ADAPTER = ProtoAdapter.newMessageAdapter(Dinosaur.class, "type.googleapis.com/squareup.dinosaurs.Dinosaur", Syntax.PROTO_3);

  private static final long serialVersionUID = 0L;

  /**
   * Common name of this dinosaur, like "Stegosaurus".
   */
  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING",
      label = WireField.Label.OMIT_IDENTITY
  )
  public final String name;

  /**
   * URLs with images of this dinosaur.
   */
  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#STRING",
      label = WireField.Label.REPEATED,
      jsonName = "pictureUrls"
  )
  public final List<String> picture_urls;

  @WireField(
      tag = 5,
      adapter = "com.squareup.geology.Period#ADAPTER",
      label = WireField.Label.OMIT_IDENTITY
  )
  public final Period period;

  public Dinosaur(String name, List<String> picture_urls, Period period) {
    this(name, picture_urls, period, ByteString.EMPTY);
  }

  public Dinosaur(String name, List<String> picture_urls, Period period, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    if (name == null) {
      throw new IllegalArgumentException("name == null");
    }
    this.name = name;
    this.picture_urls = Internal.immutableCopyOf("picture_urls", picture_urls);
    if (period == null) {
      throw new IllegalArgumentException("period == null");
    }
    this.period = period;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.name = name;
    builder.picture_urls = Internal.copyOf(picture_urls);
    builder.period = period;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Dinosaur)) return false;
    Dinosaur o = (Dinosaur) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(name, o.name)
        && picture_urls.equals(o.picture_urls)
        && Internal.equals(period, o.period);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (name != null ? name.hashCode() : 0);
      result = result * 37 + picture_urls.hashCode();
      result = result * 37 + (period != null ? period.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  public static final class Builder extends Message.Builder<Dinosaur, Builder> {
    public String name;

    public List<String> picture_urls;

    public Period period;

    public Builder() {
      name = "";
      picture_urls = Internal.newMutableList();
      period = Period.CRETACEOUS;
    }

    /**
     * Common name of this dinosaur, like "Stegosaurus".
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * URLs with images of this dinosaur.
     */
    public Builder picture_urls(List<String> picture_urls) {
      Internal.checkElementsNotNull(picture_urls);
      this.picture_urls = picture_urls;
      return this;
    }

    public Builder period(Period period) {
      this.period = period;
      return this;
    }

    @Override
    public Dinosaur build() {
      return new Dinosaur(name, picture_urls, period, super.buildUnknownFields());
    }
  }
}
```

The Java code to create and access proto models is compact and readable:

```java
Dinosaur stegosaurus = new Dinosaur.Builder()
    .name("Stegosaurus")
    .period(Period.JURASSIC)
    .build();

System.out.println("My favorite dinosaur existed in the " + stegosaurus.period + " period.");
```

Each type has a corresponding `ProtoAdapter` that can encode a message to bytes and decode bytes
back into a message.

```java
Dinosaur stegosaurus = ...
byte[] stegosaurusBytes = Dinosaur.ADAPTER.encode(stegosaurus);

byte[] tyrannosaurusBytes = ...
Dinosaur tyrannosaurus = Dinosaur.ADAPTER.decode(tyrannosaurusBytes);
```

When accessing a field, use `Wire.get()` to replace null values with the corresponding default:

```java
Period period = Wire.get(stegosaurus.period, Dinosaur.DEFAULT_PERIOD);
```

This is equivalent to the following:

```
Period period = stegosaurus.period != null ? stegosaurus.period : Dinosaur.DEFAULT_PERIOD;
```
</details>

Wire Kotlin
-----------

Since version 3.0.0, Wire can generate Kotlin code. See
[Wire Compiler & Gradle Plugin][compiler_docs] to learn how to configure your build.

Kotlin is a pragmatic and expressive programming language that makes it easy to model data. Here's
how we used Kotlin to model Protocol Buffers messages:

 * Messages feel like `data` classes, but in fact they're not. Compiler still generates `equals()`,
   `hashCode()`, `toString()` and `copy()` for you. Wire does not generate `componentN()` functions
   though, we believe that destructuring declarations are not a good fit for Protocol Buffers: a
   change in the schema that removes or adds a field might lead to a situation when your
   destructuring declaration still compiles but now describes a completely different subset of
   fields, rendering your code incorrect.
 * `copy()` is a substitute for the `Builder`, which is not used anymore. If your program relies on
   the `Builder` to be present, you may generate code in Java interoperability mode -
   [Wire Compiler & Gradle Plugin][compiler_docs] explains how that works.
 * Fields are generated as properties. While this is idiomatic in Kotlin, Java code will now have
   to access fields using getters. If your program relies on accessing fields directly, use Java
   interoperability mode - the compiler will generate `@JvmField` annotations for each field.
 * The nullability of each field's type depends on its label: `required`, `repeated` and `map`
   fields get non-nullable types, whereas `optional` fields are of nullable types.
 * With the exception of `required` fields, each field has a default value:

   * null for `optional` fields,
   * `emptyList()` for `repeated` fields,
   * `emptyMap()` for `map` fields.

<details>
<summary>Generated Kotlin Code</summary>

Here's the compact [generated code][dinosaur_kotlin] for the `Dinosaur` message defined above:

```kotlin
// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.dinosaurs.Dinosaur in squareup/dinosaurs/dinosaur.proto
package com.squareup.dinosaurs

import com.squareup.geology.Period
import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.Syntax.PROTO_3
import com.squareup.wire.WireField
import com.squareup.wire.internal.immutableCopyOf
import com.squareup.wire.internal.sanitize
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Long
import kotlin.Nothing
import kotlin.String
import kotlin.collections.List
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

class Dinosaur(
  /**
   * Common name of this dinosaur, like "Stegosaurus".
   */
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    label = WireField.Label.OMIT_IDENTITY
  )
  val name: String = "",
  picture_urls: List<String> = emptyList(),
  @field:WireField(
    tag = 5,
    adapter = "com.squareup.geology.Period#ADAPTER",
    label = WireField.Label.OMIT_IDENTITY
  )
  val period: Period = Period.CRETACEOUS,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<Dinosaur, Nothing>(ADAPTER, unknownFields) {
  /**
   * URLs with images of this dinosaur.
   */
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    label = WireField.Label.REPEATED,
    jsonName = "pictureUrls"
  )
  val picture_urls: List<String> = immutableCopyOf("picture_urls", picture_urls)

  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing = throw AssertionError()

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is Dinosaur) return false
    if (unknownFields != other.unknownFields) return false
    if (name != other.name) return false
    if (picture_urls != other.picture_urls) return false
    if (period != other.period) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + name.hashCode()
      result = result * 37 + picture_urls.hashCode()
      result = result * 37 + period.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    result += """name=${sanitize(name)}"""
    if (picture_urls.isNotEmpty()) result += """picture_urls=${sanitize(picture_urls)}"""
    result += """period=$period"""
    return result.joinToString(prefix = "Dinosaur{", separator = ", ", postfix = "}")
  }

  fun copy(
    name: String = this.name,
    picture_urls: List<String> = this.picture_urls,
    period: Period = this.period,
    unknownFields: ByteString = this.unknownFields
  ): Dinosaur = Dinosaur(name, picture_urls, period, unknownFields)

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<Dinosaur> = object : ProtoAdapter<Dinosaur>(
      FieldEncoding.LENGTH_DELIMITED,
      Dinosaur::class,
      "type.googleapis.com/squareup.dinosaurs.Dinosaur",
      PROTO_3,
      null
    ) {
      override fun encodedSize(value: Dinosaur): Int {
        var size = value.unknownFields.size
        if (value.name != "") size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
        size += ProtoAdapter.STRING.asRepeated().encodedSizeWithTag(2, value.picture_urls)
        if (value.period != Period.CRETACEOUS) size += Period.ADAPTER.encodedSizeWithTag(5,
            value.period)
        return size
      }

      override fun encode(writer: ProtoWriter, value: Dinosaur) {
        if (value.name != "") ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
        ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 2, value.picture_urls)
        if (value.period != Period.CRETACEOUS) Period.ADAPTER.encodeWithTag(writer, 5, value.period)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): Dinosaur {
        var name: String = ""
        val picture_urls = mutableListOf<String>()
        var period: Period = Period.CRETACEOUS
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> name = ProtoAdapter.STRING.decode(reader)
            2 -> picture_urls.add(ProtoAdapter.STRING.decode(reader))
            5 -> try {
              period = Period.ADAPTER.decode(reader)
            } catch (e: ProtoAdapter.EnumConstantNotFoundException) {
              reader.addUnknownField(tag, FieldEncoding.VARINT, e.value.toLong())
            }
            else -> reader.readUnknownField(tag)
          }
        }
        return Dinosaur(
          name = name,
          picture_urls = picture_urls,
          period = period,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: Dinosaur): Dinosaur = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}
```

Creating and accessing proto models is easy:

```kotlin
val stegosaurus = Dinosaur(
    name = "Stegosaurus",
    period = Period.JURASSIC
)

println("My favorite dinosaur existed in the ${stegosaurus.period} period.")
```

Here's how you can modify the object to add extra fields:

```kotlin
val stegosaurus = stegosaurus.copy(
    picture_urls = listOf("https://www.flickr.com/photos/tags/Stegosaurus/")
)

println("Here are some photos of ${stegosaurus.name}: ${stegosaurus.picture_urls}")
```
</details>

Wire Swift
-----------

Since version 3.3.0, Wire can generate Swift code. See
[Wire Compiler & Gradle Plugin][compiler_docs_swift] to learn how to configure your build.

Swift support is considered a "beta" and may still feature breaking changes.
That being said, Block is shipping it in production apps and SDKs.

Swift is a pragmatic and expressive programming language with rich support for value types.
Here's how we used Swift to model Protocol Buffers messages:

 * Messages are structs that conform to `Equatable`, `Codable` and `Sendable`. All Messages have value semantics.
 * Messages have a memberwise initializer to populate fields.
 * Fields are generated as properties.
 * The nullability of each field's type depends on its label: `required`, `repeated` and `map`
   fields get non-nullable types, whereas `optional` fields are of nullable types.

<details>
<summary>Generated Swift Code</summary>

Here's the compact [generated code][dinosaur_swift] for the `Dinosaur` message defined above:

```swift
// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.dinosaurs.Dinosaur in squareup/dinosaurs/dinosaur.proto
import Foundation
import Wire

public struct Dinosaur {

    /**
     * Common name of this dinosaur, like "Stegosaurus".
     */
    public var name: String?
    /**
     * URLs with images of this dinosaur.
     */
    public var picture_urls: [String]
    public var length_meters: Double?
    public var mass_kilograms: Double?
    public var period: Period?
    public var unknownFields: Data = .init()

    public init(
        name: String? = nil,
        picture_urls: [String] = [],
        length_meters: Double? = nil,
        mass_kilograms: Double? = nil,
        period: Period? = nil
    ) {
        self.name = name
        self.picture_urls = picture_urls
        self.length_meters = length_meters
        self.mass_kilograms = mass_kilograms
        self.period = period
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension Dinosaur : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension Dinosaur : Hashable {
}
#endif

#if swift(>=5.5)
extension Dinosaur : Sendable {
}
#endif

extension Dinosaur : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.dinosaurs.Dinosaur"
    }
}

extension Dinosaur : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var name: String? = nil
        var picture_urls: [String] = []
        var length_meters: Double? = nil
        var mass_kilograms: Double? = nil
        var period: Period? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: name = try reader.decode(String.self)
            case 2: try reader.decode(into: &picture_urls)
            case 3: length_meters = try reader.decode(Double.self)
            case 4: mass_kilograms = try reader.decode(Double.self)
            case 5: period = try reader.decode(Period.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.name = name
        self.picture_urls = picture_urls
        self.length_meters = length_meters
        self.mass_kilograms = mass_kilograms
        self.period = period
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.name)
        try writer.encode(tag: 2, value: self.picture_urls)
        try writer.encode(tag: 3, value: self.length_meters)
        try writer.encode(tag: 4, value: self.mass_kilograms)
        try writer.encode(tag: 5, value: self.period)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension Dinosaur : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: StringLiteralCodingKeys.self)
        self.name = try container.decodeIfPresent(String.self, forKey: "name")
        self.picture_urls = try container.decodeProtoArray(String.self, firstOfKeys: "pictureUrls", "picture_urls")
        self.length_meters = try container.decodeIfPresent(Double.self, firstOfKeys: "lengthMeters", "length_meters")
        self.mass_kilograms = try container.decodeIfPresent(Double.self, firstOfKeys: "massKilograms", "mass_kilograms")
        self.period = try container.decodeIfPresent(Period.self, forKey: "period")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringLiteralCodingKeys.self)
        let preferCamelCase = encoder.protoKeyNameEncodingStrategy == .camelCase
        let includeDefaults = encoder.protoDefaultValuesEncodingStrategy == .include

        try container.encodeIfPresent(self.name, forKey: "name")
        if includeDefaults || !self.picture_urls.isEmpty {
            try container.encodeProtoArray(self.picture_urls, forKey: preferCamelCase ? "pictureUrls" : "picture_urls")
        }
        try container.encodeIfPresent(self.length_meters, forKey: preferCamelCase ? "lengthMeters" : "length_meters")
        try container.encodeIfPresent(self.mass_kilograms, forKey: preferCamelCase ? "massKilograms" : "mass_kilograms")
        try container.encodeIfPresent(self.period, forKey: "period")
    }
}
#endif
```

Creating and accessing proto models is easy:

```swift
let stegosaurus = Dinosaur(
    name: "Stegosaurus",
    period: .JURASSIC
)

print("My favorite dinosaur existed in the \(stegosaurus.period) period.")
```

Here's how you can modify the object to add extra fields:

```swift
var stegosaurus = stegosaurus
    stegosaurus.picture_urls = ["https://www.flickr.com/photos/tags/Stegosaurus/"]
)

print("Here are some photos of \(stegosaurus.name): \(stegosaurus.picture_urls)")
```
</details>

Wire gRPC
-----------

Since version 3.0.0, Wire supports [gRPC][grpc_docs].

Generating Code With Wire
-------------------------

Wire can read `.proto` files from the local file system and from within `.jar` files.

The compiler can optionally prune your schema to a subset of root types and their transitive
dependencies. This is useful when sharing a schema between projects: a Java service and Android app
may each use a subset of a larger shared schema.

For more info on how to get started, see [Wire Compiler & Gradle Plugin][compiler_docs].

If you don't use Gradle, the compiler also has a command line interface. Just substitute
`wire-compiler-VERSION-jar-with-dependencies.jar` with the path to your jar. [Download](https://search.maven.org/remote_content?g=com.squareup.wire&a=wire-compiler&c=jar-with-dependencies&v=LATEST) the latest precompiled jar.

    % java -jar wire-compiler-VERSION-jar-with-dependencies.jar \
        --proto_path=src/main/proto \
        --java_out=out \
        squareup/dinosaurs/dinosaur.proto \
        squareup/geology/period.proto
    Writing com.squareup.dinosaurs.Dinosaur to out
    Writing com.squareup.geology.Period to out

Supplying the `--android` flag to the compiler causes Wire messages to implement `Parcelable`.

If you use Proguard, then you need to add `keep` rules.  The simplest option is to tell Proguard not
to touch the Wire runtime library and your generated protocol buffers (of course these simple rules
will miss opportunities to shrink and optimize the code):

    -keep class com.squareup.wire.** { *; }
    -keep class com.yourcompany.yourgeneratedcode.** { *; }


Get Wire
--------

The `wire-runtime` package contains runtime support libraries that must be included in applications
that use Wire-generated code.

With Maven:

```xml
<dependency>
  <groupId>com.squareup.wire</groupId>
  <artifactId>wire-runtime-jvm</artifactId>
  <version>4.4.3</version>
</dependency>
```

With Gradle:

```groovy
api "com.squareup.wire:wire-runtime:4.4.3"
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].


Unsupported
-----------

Wire does not support:

 * Groups - they are skipped when parsing binary input data

Wire supports custom options on messages and fields. Other custom options are ignored. Pass
`--excludes=google.protobuf.*` to the compiler to omit options from the generated code.


Further Documentation
---------------------

See [Google's excellent documentation][schema_docs] on the structure and syntax of proto schemas.

 [google_protos]: https://developers.google.com/protocol-buffers/docs/overview
 [effective_java]: https://www.amazon.ca/Effective-Java-3rd-Joshua-Bloch/dp/0134685997/
 [schema_docs]: https://developers.google.com/protocol-buffers/docs/proto
 [dl_runtime]: https://search.maven.org/remote_content?g=com.squareup.wire&a=wire-runtime&v=LATEST
 [dl_compiler]: https://search.maven.org/remote_content?g=com.squareup.wire&a=wire-compiler&v=LATEST&c=jar-with-dependencies
 [snap]: https://s01.oss.sonatype.org/content/repositories/snapshots/
 [wire]: https://square.github.io/wire/

 [compiler_docs]: docs/wire_compiler.md
 [compiler_docs_swift]: docs/wire_compiler.md#swift-support
 [grpc_docs]: docs/wire_grpc.md

 [dinosaur_proto]: samples/simple-sample/src/main/proto/squareup/dinosaurs/dinosaur.proto
 [period_proto]: samples/simple-sample/src/main/proto/squareup/geology/period.proto
 [dinosaur_java]: samples/simple-sample/src/main/java/com/squareup/dinosaurs/Sample.java
 [dinosaur_kotlin]: wire-tests/src/commonTest/proto-kotlin/com/squareup/dinosaurs/Dinosaur.kt
 [dinosaur_swift]: wire-runtime-swift/src/test/swift/sample/Dinosaur.swift
