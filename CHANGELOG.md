Change Log
==========

Version 3.1.0
-------------

_2020-02-06_

This release includes major non-backwards-compatible API changes to the `wire-schema` module. This
will break tools that use Wire's schema modeling as a standalone library. We are making big changes
to this component and we sacrificed API compatibility to accelerate these improvements.

 * New: `proto { ... }` target in the Wire Gradle plugin. Use this to perform basic source code
   transformations on collections of `.proto` files. We use it to prune large collections of protos
   to just the subset used by the application.
 * Fix: Support all forms of reserved extensions, such as `extensions 1, 3 to 5, 7;`.
 * Fix: Don't re-generate source files when their `.proto` files haven't changed.
 * New: `includes`, `excludes`, `root`, and `prune` give precedence to the most precise rule.
   Previously `excludes` always took precedence over `includes`, and `prune` always took precedence
   over `root`.
 * Fix: Generate non-instantiable class for enclosing types in Kotlin. These are emitted when a
   nested type is retained but its enclosing type is pruned.
 * Fix: Do not fail to build when the profile cannot find a dependency.


Version 3.0.3
-------------

_2019-12-23_

Starting with this version the Wire Maven plugin is no longer maintained and has been removed from
the repository.

 * New: Support for custom options in Kotlin.
 * New: Kotlin 1.3.61.
 * New: Add support for custom targets in `WireRun` and the Gradle plugin.
 * New: Improve schema evaluation algorithm when loading separate `sourcePath` and `protoPath`.
 * New: Lazy loading of `protoPath` contents.
 * New: Make it possible to customize Gradle plugin's configurations.
 * New: Make it possible to customize Gradle plugin's `generateProtos` task.
 * Fix: Use correct `ProtoAdapter` for packed fields in Kotlin.
 * Fix: Properly handle name clashes between fields and enclosing types.
 * Fix: Preserve the package name on files loaded from `protoPath`.
 * Fix: ProtoPruner: Properly evaluate Pruner's reachable objects.
 * Fix: ProtoPruner: Ensure `--excludes` properly prunes options.
 * Fix: ProtoPruner: Keep used `ServiceOptions` and `MethodOptions` when pruning.

Version 3.0.2
-------------

_2019-11-22_

 * Fix: Generate correct unknownFields code if a message field's name is a Kotlin keyword.
 * Fix: Properly handle unknown enum values in Kotlin.
 * Fix: ProtoPruner: retain used extends.
 * Fix: ProtoPruner: retain only used imports.
 * Fix: ProtoPruner: use NewSchemaLoader that correctly loads google.protobuf.descriptor.
 * Fix: ProtoPruner: print default values for scalar types for proto target within the options.
 * Fix: ProtoPruner: fix handling of options.
 * Fix: ProtoPruner: print default values for enums.

Version 3.0.1
-------------

_2019-10-18_

 * Fix: Use the correct adapter path for gRPC endpoints that customize the Java package.
 * Fix: Preserve documentation in generated services.
 * Fix: Fail to generate code if the source directory doesn't exist.
 * Fix: Make Kotlin consistent with Java for unknown enum constants. We now treat these as unknown
   fields rather than failing to decode the enclosing message.

Version 3.0.0
-------------

_2019-10-07_

 * Update: All gRPC networking calls are encoded in gzip.

Version 3.0.0-rc03
------------------

_2019-10-04_

 * Fix: Update dependency to a stable version, `2.4.1` of Okio.

Version 3.0.0-rc02
------------------

_2019-10-01_

### Kotlin

 * Fix: Nullify other oneof fields in Builder setters in Kotlin interop.
 * Fix: Use unknownFields in `hashCode()`.
 * Fix: Remove `withoutUnknownFields()` from Kotlin.

### gRPC

 * Update: Total rewrite of the generated interfaces for clients:

   Introduce two interfaces, `GrpcCall` for simple RPCs, and `GrpcStreamingCall` fox duplex ones. Both
   will provide blocking and suspending API, including a reference to the underlying
   [OkHttp](https://github.com/square/okhttp)
   [Call](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-call/) object and its timeout.

 * Fix: Send stream cancels from clients.

### Misc

 * New: Changes printing of options and enums:
   * No empty lines between options and fields for enums.
   * Print options on new lines only when more than one.
 * Fix: Don't cache Message's hash code on Native.
 * Fix: Fix handling of map values in `FieldBinding`.
 * Fix: Fix import fails on windows due to path separator.
 * Fix: Don't emit proto2 sources for proto3 syntax.

Version 3.0.0-rc01
------------------

_2019-08-02_

### Compiler + Gradle plugin

 * New: Support includes on Maven coordinate dependencies.
 * New: Track includes separately for source vs proto paths.
 * New: Follow symlinks when building.
 * New: Change the Gradle plugin to track targets as a list.
 * New: Includes and Excludes for Wire targets.
 * New: Print errors on ambiguous and missing imports.
 * Fix: Fix a bug where protopath Maven resources weren't working.
 * Fix: Don't reuse source dependencies as protopath dependencies.
 * Fix: Fix `equals()` implementation for messages with no fields.

### Kotlin

 * New: Move Wire.kt into `jvmMain` to discourage its use in common Kotlin code.
 * New: Make `Message.adapter` a `val`.
 * New: Optimize `decode()` code for protos with no fields.
 * New: Update supported Native platforms.
 * New: Make `Message.unknownFields` property non-nullable.
 * New: Make `Message.unknownFields` a `val`.
 * Fix: Don't use `KClass.simpleName` to avoid needing `kotlin-reflect` dependency.
 * Fix: Use `kotlin.UnsupportedOperationException` in generated code.
 
### gRPC

 * New: Introduce `MessageSource` and `MessageSink` interfaces in `wire-runtime`.
 * New: Honor Java package names in Wire gRPC services.
 * New: Make `PipeDuplexRequestBody` internal.
 * Fix: Workaround for `@Generated` annotation on Java 9+.
 * Fix: Fix types for blocking APIs.
 
### Misc

 * Fix: Fix deserializing null values in Gson adapter.
 * Fix: Change `wire-runtime` artifact names to preserve 2.x compatibility.

Version 3.0.0-alpha03
---------------------

_2019-06-22_

 * Similar to alpha02, but with proper `wire-runtime` multiplatform artifacts.
	
Version 3.0.0-alpha02
---------------------

_2019-06-21_

 * New: Experimental multiplatform runtime.
 
   Starting with this version, `wire-runtime` is published as a multiplatform Kotlin artifact. While
   the JVM artifact is binary- and behavior-compatible with 3.0.0-alpha01, artifacts for other 
   platforms may not work correctly at this point. The artifact name for the JVM artifact has been 
   changed to `wire-runtime-jvm`: now, in order to depend on the multiplatform runtime, use the 
   following Gradle dependency declaration:
   
   ```groovy
   api "com.squareup.wire:wire-runtime:3.0.0-alpha02"
   ``` 
   
   and if you want to depend on the JVM artifact only, use the following declaration:
   
   ```groovy
   api "com.squareup.wire:wire-runtime-jvm:3.0.0-alpha02"
   ```
   
 * New: Generate RPCs as Single Abstract Methods.
 * New: Add "singleMethod" Gradle plugin configuration for services.
 * New: Add "blockingServices" Gradle plugin configuration for services.
 * New: Support packageless services code generation.
 * New: Remove sealed classes-based oneof implementation.
 * New: Don't generate a Builder for non-interop Kotlin messages.
 * Fix: Kotlin Generator correctly generates code for Protobuf services.
 * Fix: Improved formatting of generated Kotlin code.
 * Fix: Generate correct adapter names for WireField annotation.
 * Fix: Generate labels for WireField annotation.
 * Fix: Wrap oneof error message properly.
	
Version 3.0.0-alpha01
---------------------

_2019-03-14_

 * New: Kotlin Generator
 
   Wire 3 can generate Kotlin data classes. To enable this feature via the command line API, pass in 
   the `--kotlin_out` parameter that should specify the output directory for the generated `*.kt` 
   files. 
   
   Given the following simple proto:
   
   ```proto
   message Person {
     required string name = 1;
     required int32 id = 2;
     optional string email = 3;
   }
   ```
   
   the generated Kotlin code will look like the following:
   
   ```kotlin
   data class Person(
     @field:WireField(tag = 1, adapter = "com.squareup.wire.ProtoAdapter#STRING") 
     val name: String,
     @field:WireField(tag = 2, adapter = "com.squareup.wire.ProtoAdapter#INT32") 
     val id: Int,
     @field:WireField(tag = 3, adapter = "com.squareup.wire.ProtoAdapter#STRING") 
     val email: String? = null,
     val unknownFields: ByteString = ByteString.EMPTY
   ) : Message<Person, Person.Builder>(ADAPTER, unknownFields) {
     companion object {
       @JvmField
       val ADAPTER: ProtoAdapter<Person> = ... // code omitted for brevity
   ```
   
   The `copy()` method of a data class replaces most usages of the builder. If your code relies on 
   the `Builder`, you can enable full `Builder` generation by passing the `--java_interop` parameter 
   to the compiler.
 
 * New: gRPC support
 
   In addition to generating Kotlin code from proto messages, Wire can now generate code for gRPC
   endpoints. Here's an example schema:
   
   ```proto
   service RouteGuide {
     // A simple RPC.
     //
     // Obtains the feature at a given position.
     //
     // A feature with an empty name is returned if there's no feature at the given
     // position.
     rpc GetFeature(Point) returns (Feature) {}
   }    
   ```
   
   The generated code will look like the following (message protos, referenced by the schema, are
   omitted):
   
   ```kotlin
   interface RouteGuide : Service {
     @WireRpc(
         path = "/routeguide.RouteGuide/GetFeature",
         requestAdapter = "routeguide.Point#ADAPTER",
         responseAdapter = "routeguide.Feature#ADAPTER"
     )
     suspend fun GetFeature(request: Point): Feature
   }
   ```
   
   All four gRPC modes are supported: the generated code uses suspendable functions to implement
   non-blocking asynchronous execution. In streaming modes, `ReceiveChannel` and `SendChannel` are
   used to listen to asynchronous data in a non-blocking fashion.
   
   This feature works out of the box in Wire 3 compiler as long as the input file contains a gRPC
   schema.
 
 * New: Gradle plugin
 
   Here's an example Gradle configuration:
   
   ```groovy
   apply plugin: 'com.squareup.wire'
   
   wire {
     // Keeps only 'Dinosaur#name' as the root of the object graph
     roots 'squareup.dinosaurs.Dinosaur#name'
   
     // Keeps all fields, except 'name', in 'Dinosaur'
     prunes 'squareup.dinosaurs.Dinosaur#name'
   
     // Both roots and prunes in an external file
     rules 'rules.txt'
   
     kotlin {
       javaInterop true
       out "${buildDir}/generated/custom"
     }
   }
   ```
   
   The `wire` extension introduces the concept of compilation targets, such as `kotlin` and `java`,
   where each target has its own configuration properties. Multiple targets can be supplied, which 
   benefits use cases such as migrating Java protos to Kotlin.
   
  * New: Decouple the option of using Android annotations for nullability from the option of having messages implement Parcelable. 
  * New: Wire Moshi adapter for serializing proto JSON representation using the Moshi library.
  * New: Implement support for custom enum types.
  * New: Generate AndroidX nullability annotations instead of old support library annotations.
  * New: Import JSR 305 and use it to mark nullability of public API.
  * New: Allow inline multiline comments.
  * New: Generate an empty class when a nested message is retained but its parent was pruned.
  * New: Support rendering a `ProtoFile` to its schema.
  * New: Support hexadecimal numeric literals.
  * New: Allow custom types to be constrained with a 'with' clause.
  * New: Generate a constructor which takes in a `Message.Builder` instead of all fields separately.
  * New: Add location to the error message about unsupported group elements.
  * New: Permit single files to be used on the proto path.
  * Fix: Emit '=' for syntax declaration.
  * Fix: Don't crash when a comment has a dollar sign.
  * Fix: Return subclass type instead of abstract parameterized type for newBuilder.
  * Fix: Validate enum namespace in file context are unique.

Version 2.2.0
-------------

_2016-06-17_

 * New: Support for `map` type in the schema, compiler, and runtime!
 * New: `AndroidMessage` base class consolidates everything required for supporting Android and will
   now be used for generating code with `--android`.
 * New: `stream` keyword in RPC definitions is now parsed and exposed in the schema.
 * Fix: Nested types which are retained no longer cause their enclosing type to be retained. Instead,
   non-instantiable empty types will be generated for pruned enclosing types.
 * Fix: Remove per-type `Parcelable.Creator` classes and instead use a single type which delegates
   to the message's `ProtoAdapter`.
 * Fix: Retain information on redacted fields even when options were pruned.
 * Fix: Do not generate code for handling `null` from list types (and now map types) which are
   guaranteed to never be `null`.


Version 2.1.2
-------------

_2016-04-15_

 * Fix: Gson type adapter now deserializes JSON null literals to empty list for repeated fields.


Version 2.1.1
-------------

_2016-02-01_

 * New: `reserved` keyword is now supported and enforced.
 * Fix: Defer reflection-based lookup of enum method until first use to avoid
   class loading race conditions.
 * Fix: Support single-quoted string literals.
 * Fix: Adjacent string literals are not correctly concatenated.


Version 2.1.0
-------------

_2016-01-18_

 * **Empty lists of packed values were being encoded incorrectly.** In Wire 2.0.x our message
   adapters incorrectly included empty lists for `[packed=true]` rather than omitting them. This is
   now fixed.
 * New: `Message.encode()` to concisely encode a message.
 * New: `MessageAdapter.decode(ByteString)` to decode a message from a byte string without an
   intermediate byte array.
 * New: Wire now includes a sample code generation for service interfaces.


Version 2.0.3
-------------

_2016-01-04_

 * New: `ProtoAdapter.get` overload which returns an adapter given an instance of a message.
 * New: `@Nullable` annotations are emitted for `optional` fields when using `--android`.
 * Fix: Remove the need for `javac` to generate synthetic accessor methods in the generated code.
   This results in smaller code size and less method references (for Android users).


Version 2.0.2
-------------

_2015-12-14_

 * Fix: Exclude unknown fields when encoding JSON and drop unknown fields when parsing JSON.
 * Fix: Ensure JSON encoding and decoding works in the default generation mode (not just
   `--compact`) by always adding `@WireField` metadata to message fields.
 * Fix: Update to JavaPoet 1.4 for more accurate generation of valid Java code.


Version 2.0.1
-------------

_2015-11-12_

 * Fix: Do not emit `case` statements for aliased enum constant values. The first constant for a
   value will be returned when deserializing.
 * Fix: Emit `@Deprecated` annotation on deprecated enum constants.
 * Fix: Correctly prune dependencies of excluded message, enum, or service members. Previously
   the dependencies of an excluded member were retained despite the member itself being omitted.


Version 2.0.0
-------------

_2015-10-23_

Wire 2 is a backwards-incompatible release. It makes breaking changes to the compiler, runtime,
extensions, and generated code. These changes aren’t made lightly as we’ve endured the upgrade in
our own projects! We believe the cost of migration is worth the benefits.

**We’ve created the `wire-schema` library that models `.proto` schema definitions.** This is a
capable library that packs several neat features. You can load a `Schema` from `.proto` files
located on the local file system, a ZIP or JAR file, or any `java.nio.FileSystem` like
[Jimfs][jimfs]. You can prune this schema with includes or excludes, allowing you to reuse `.proto`
definitions with minimal code. And you can decode data directly from a schema: no code generation
is necessary!

**We’ve flattened extensions.** Wire 2.0 combines the fields defined directly on messages with
fields defined far away in extensions. In the generated code, extension fields look just like every
other field! One limitation of this approach is that it’s no longer possible to compile extensions
separately from the messages they extend. For this reason we now recommend always generating all
Wire code in a single step.

**We’ve rearranged the runtime.** Types related to the protocol buffers format are now prefixed
`Proto` and types related to our implementation are prefixed `Wire`. To encode and decode messages
you must first get an adapter either from the `ADAPTER` constant or from `ProtoAdapter.get()`. You
no longer need a `Wire` instance!

#### Runtime

 * New `ADAPTER` constant on most messages gives access to encode & decode values. This replaces
   the encoding and decoding methods on `Wire`.
 * Guard against null lists. Code that passes `null` to builder methods expecting a `List` used to
   accept that; now Wire throws a `NullPointerException`. Similarly list elements must also be
   non-null.
 * New `Message.newBuilder()` API. This replaces the previous copy constructor on `Builder`.
 * New: `Message.withoutUnknownFields()` strips unknown fields.
 * Fix: Always throw `ProtocolException` when there are decoding problems.
 * Fix: Stricter checking for oneof fields. Previously it was possible to create instances with
   multiple values set!
 * Fix: Improve redacting of repeated fields.
 * Fix: `ProtoReader` now silently unpacks packed values.
 * Fix: `ProtoReader` doesn’t return groups to callers.

#### Schema & Java Generator

 * The Java generator is now standalone. Use these APIs programmatically to build plugins without
   delegating to the command line interface.
 * New: Prune schemas using includes and excludes. The `IdentifierSet` can be used to configure
   which types and members are retained and which are pruned.
 * New: Encode and decode values directly from the schema.
 * New: Improved error messages for validation.
 * Fix: Strict imports.
 * Fix: Detect and forbid conflicts on extension tags.

#### Compiler

 * New: Always use Wire’s bundled `descriptor.proto`. Previously to define custom options you
   needed to import a potentially-inconsistent descriptor.
 * New: Emit all types when no `.proto` files are explicitly specified.
 * New: Generate code for encoding and decoding messages. The previous, reflection-based
   encoder and decoder are accessible with `--compact`.
 * New: `ServiceFactory` has been removed. To generate code for your services, load a schema with
   `wire-schema` and then use a library like [JavaPoet][javapoet] to generate your own code. The
   `JavaGenerator` class can be used to look up Java names of message types.
 * New: Compiler will load all `.proto` files if none are explicitly specified.
 * New: Load `.proto` files from ZIP and JAR files.
 * New: The `--android` flag causes Wire messages to implement `Parcelable`.
 * New: Support multiple `--proto_path` arguments
 * New: The `--named_files_only` flag limits which `.proto` files yield `.java` files. This was the
   default in Wire 1.x.
 * New: The `--no_options` flag has been deleted. Use `--excludes=google.protobuf.*` instead.

#### Extensions

 * Extensions have been flattened.
 * Fix: Better field resolution for options.
 * Fix: Extension fields must not be `required`.


Version 1.8.0
-------------

_2015-06-27_

 * New: `oneof` support!
 * Fix: Correct serialization of repeated unknown fields.
 * Fix: Removed superfluous `private` modifier on enum constructor.
 * Warning: The 'protoparser' library was updated to version 4.0. This changes the type passed to
   any `ServiceWriter` implementations.

Version 1.7.0
-------------

_2015-03-05_

 * New: Messages implement `Serializable`. The serialized form follows protobuf encoding, so
   renaming fields is safe as long as tags are consistent. (Renaming classes is unsafe, however).
   Note that extension fields are deserialized as unknown fields.

Version 1.6.1
-------------

_2015-01-16_

 * New: `--quiet` and `--dry-run` command-line arguments.
 * Fix: Handle an extension registry with zero or only one item.
 * Okio dependency bumped to 1.2.0.


Version 1.6.0
-------------

_2014-10-23_

 * Fix: Correctly throw `IOException` when parsing bad input fails.
 * Fix: Ensure emitted code references to `Arrays.asList` correctly compiles in some edge cases.
 * '__UNDEFINED__' enum value has been removed.


Version 1.5.2
-------------

_2014-09-15_

 * New: '__UNDEFINED__' enum value represents values that the generated code is unable to handle.
 * New: Service writer implementation can now be specified on the command-line.


Version 1.5.1
-------------

_2014-06-18_

 * New: Generate interface definition for a `Service` with a partial list of methods.
 * Okio dependency bumped to 1.0.0.


Version 1.5.0
-------------

_2014-04-22_

 * New: Options on enums and enum values are now supported.
 * New: Options ending with `.redacted` on fields will omit values from `toString`.
 * New: `Redactor` creates copies of messages with redacted fields omitted.
 * Various internal serialization improvements and memory reduction.
 * Okio dependency bumped to 0.9.0.


Version 1.4.0
-------------

_2014-04-22_

 * Replace Wire's ByteString class with the one from Okio (https://github.com/square/okio).
   **This is a breaking API change**.

 * Add a new static method `Wire.parseFrom(okio.Source input, Class<M> messageClass)`.

 * Reimplement Wire's input handling to use Okio internally.

 * Provide basic support for generating code from service declarations (see README.md).

 * Improve deserialization performance.

 * Fix a bug where no some extension dependencies were not detected.


Version 1.3.3
-------------

_2014-03-28_

 * New: Support service declarations as roots. The request and response types of their methods will
   be included.


Version 1.3.2
-------------

_2014-03-27_

 * Fix: Enum value duplicate check now correctly looks at names instead of values.


Version 1.3.1
-------------

_2014-03-25_

 * New: Automatically add Maven plugin's generated source as a compilation root.
 * Fix: Correct Maven plugin's 'no arguments' flag to work properly.
 * Fix: Support extend declarations nested inside message types.


Version 1.3.0
-------------

_2014-03-21_

 * New: Empty repeated fields are now initialized to an empty collection.
 * New: Emit field options. Use `--no_options` flag to disable.
 * New: `@Deprecated` is now added to fields and setters on the builder.
 * New: Maven plugin for running the compiler as part of a build.
 * Fix: Treat empty and null repeated fields the same in `equals()` and `hashCode()`.

Note: This version is not backwards compatible with code generated from previous versions.

Version 1.2.0
-------------

_2013-11-01_

 * New: `--registry_class` compiler argument emits a class with a list of extensions suitable for
   passing to the `Wire` constructor.
 * Fix: Ensure all trailing whitespace is trimmed on documentation.


Version 1.1.1
-------------

_2013-10-23_

 * Fix: Correct parsing and emission of multi-line documentation.


Version 1.1.0
-------------

_2013-10-22_

 * New: Support for message options.
 * New: Check for duplicate field tag numbers.
 * New: Emit Javadoc on messages, enum values, and builder methods.
 * Fix: Emit imports for extension fields and classes outside of package.
 * Fix: Correctly generate sources for protos with the same basename.
 * Fix: Do not generate default constants for message types.
 * Fix: Avoid shadowing fields named "result", "other", or "o".


Version 1.0.1
-------------

_2013-08-27_

 * New: Support reading directly from `InputStream`.
 * New: Add '`other == this`' shortcut in generated `equals()` methods.


Version 1.0.0
-------------

_2013-08-23_

Initial version.


 [jimfs]: https://github.com/google/jimfs
 [javapoet]: https://github.com/square/javapoet
