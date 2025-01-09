Change Log
==========

Version 5.2.1
---------------------

_2025-01-07_

### JVM generation

* Fix support for mutable messages in Wire's Kotlin Generator. (#3233 by [Rahul Ravikumar][tikurahul])

Version 5.2.0
---------------------

_2025-01-06_

### Common

* Enforce recursion limit when parsing nested groups. (#3119)

### CLI `wire-compiler`

* It is now possible to set multiple targets. (#3106 & #3107)
* The option `opaque_types` introduced in `4.9.2` for the Wire Gradle plugin is now available on CLI. (#3147)

### JVM generation

* [KotlinPoet has been updated to `2.0.0`](https://square.github.io/kotlinpoet/changelog/#version-200) which dramatically changes how generated Kotlin files are wrapped. This is neither a source nor a binary breaking changes.
* A new `@WireEnclosingType` annotation is now applied to generated types so R8 doesn't prune too much. (#3123)
* Split the redact method into chunks when a type has more than 100 fields to avoid compilation error. (#3214 by [Damian Wieczorek][damianw])
* Add support for mutable messages in Wire's Kotlin Generator. (#3217 by [Rahul Ravikumar][tikurahul])
  * You can opt-in by adding `mutableTypes = true` on your Kotlin target. This is unsafe and we do not recommend that you use it unless you have a sound use-case for it.
* Wire is now using Palantir's JavaPoet instead of Square's JavaPoet.

### Swift

* Fix buffer overflow and data corruption when a type has more than 5 layers of nesting (#3203 by [Eric Amorde][amorde])

Version 4.9.11
---------------------

_2024-10-09_

### Common

* Fix Gradle project isolation issue when reading a property (#3078 by [Aurimas][liutikas])

Version 5.1.0
---------------------

_2024-09-11_

### Common

* Support for Kotlin `2.0.20`. (#3093)
* `srcDir(String)` has been undeprecated. (#3039)
* Some loggings now happen at the debug level, instead of info. (#3041)
* Remove some unactionable warnings on Kotlin/JS (#3047)
* Propagate the deprecated flag on EnumType after pruning by wire-gradle-plugin (#3076 by [Aaron Edwards][aaron-edwards])
* Introduce `ProtoReader32`, a specialization for Kotlin/JS (#3077)
    This is an alternative to `ProtoReader`, which uses `Long` as a cursor. It originates as an optimization for Kotlin/JS, where `Long` cursors are prohibitively expensive.
* Fix Gradle project isolation issue when reading a property (#3078 by [Aurimas][liutikas])
* Change the recursion limit to match grpc's default (#3091)

### Kotlin

* New enum option `enum_mode` to take precedence over the `enumMode` option added in `5.0.0-alpha02`. Use this if you want to migrate your enums granularly. (#2993)
* Don't throw if reading trailers fail (#3087)

### Swift

* Avoid crash when parsing an empty repeated `[packed=true]` for fixed-length types. (#3044 by [Sasha Weiss][sashaweiss-signal])

Version 5.0.0
---------------------

_2024-07-15_

### Common

* Prevent conflicts for options of different types but with similar names (#3005)
* Support all Gradle entries for file collections (#2946)
  * `srcDir(String)` has been deprecated in favor of `srcDir(Any)` which better leverages Gradle to
  handle dependencies.

Version 5.0.0-alpha04
---------------------

_2024-06-21_

### Common

* Don't crash with inlined map options (#2997)

### Swift

* Escape `error` names in some cases (#2999 by [Adam Lickel][lickel])

Version 5.0.0-alpha03
---------------------

_2024-06-19_

### JSON

* A performance degradation in JSON serialization was added in the previous `5.0.0` alphas. This
  change has been reverted.

Version 5.0.0-alpha02
---------------------

_2024-06-17_

### Common

* ⚠ Paths and Definitions are properly claims by a SchemaHandler only when it generated something (#2943)
* No eager source access on Task for the Wire Gradle plugin (#2923)
* Fix the UTF-32 BOM in SchemaLoader (#2937 by [Endeavour233][Endeavour233])
* Add support for parsing extensionRangeOptions (#2966)

### Kotlin

* expose the grpc url when converting Response to GrpcException (#2920 by [Travis Johnson][traviscj])
* Support very long field names with builders (#2959)
* New `enumMode` option added to Kotlin targets in order to generate enum types as sealed classes.
  Its value can be either `enum_class` (default value) or `sealed_class`. `enum_class` will generate
  enum types as Kotlin enum classes, which is the current behavior. `sealed_class` will generate enum
  types as Kotlin sealed classes, generated each constant of the enum type as data objects. On top of
  those constants, the sealed class will contain a `Unrecognized` data class which will contain the
  real decoded value for this enum if the runtime didn't have any constant matching it. This is the
  analogue feature of protoc generating a `UNRECOGNIZED(-1)` constant for enums on proto3. Note
  however that Wire doesn't limit this option to proto3 enums, this can be used for proto2 enums
  too.

  ```kotlin
  wire {
    kotlin {
      enumMode = "sealed_class"
    }
  }
  ```

  Switching to generating sealed class for enums can break the call-site for your consumers. In
  order to allow gradual migration of enum generation from/to sealed classes, a Protobuf enum option
  has also been created. This, when set in the `.proto` file, takes precedence over the global enum
  mode.

  ```protobuf
  import "wire/extensions.proto";

  enum Period {
    option (wire.enum_mode) = "sealed_class"; // or `enum_class`.
    CRETACEOUS = 1;
    JURASSIC = 2;
    TRIASSIC = 3;
  }
  ```

Version 5.0.0-alpha01
---------------------

_2024-04-16_

### Common

* ⚠ Reject unused prunes and roots by default (#2846)
    This behavior used to be opt-in, now it’s opt-out. You can opt out with `rejectUnusedRootsOrPrunes = false`
    in your `wire { }` block.
* ⚠ Remove deprecated `SchemaHandler#create` method (#2851)
    Override the other method `create(includes, excludes, exclusive, outDirectory, options): SchemaHandler` instead if you were not already doing it.
* ⚠ Adding a project dependency via `srcJar` used to work but not anymore. Use `srcProject(":my-project")` instead.
* Don't warn about an unused emitting rule, when that rule is '*' (#2829)
* The internal of our Wire Gradle plugin has been refactored where behavior changes should not be
    expected. If you see anything different, please [let us know](https://github.com/square/wire/issues).
* Allow custom options to be passed in WireCompiler (#2909)

### Kotlin

* Propagates Request timeout as grpc-timeout (#2840 by [Francisco Rojas][frojasg])
* Don't override RealGrpcCall.timeout when it's manually set (#2893 by [Jeff Gulbronson][JGulbronson])
* Publish the gRPC Dispatcher (#2872)
    A new helper class [GrpcDispatcher](https://github.com/square/wire/blob/master/wire-grpc-mockwebserver/src/main/java/com/squareup/wire/mockwebserver/GrpcDispatcher.kt#L64) for gRPC testing is available under `com.squareup.wire:wire-grpc-mockwebserver`.
    Note that it does not support streaming calls. If you want to contribute, please do!

### Swift

* ⚠ Refactored how extensions are emitted and provides a new API to get/set extension fields.
    Due to the refactor above, Codable fields that are defined in extensions will now break as part of this change.
    All done by [Dimitris Koutsogiorgas][dnkoutso] and [Adam Lickel][lickel]!

### wire-grpc-server

* ⚠ `wire-grpc-server` has been moved into its own repo: https://github.com/square/wire-grpc-server.
    If you were using it, you can see how migration is to happen by checking these [instructions](https://github.com/square/wire/blob/master/docs/wire_grpc.md#wire-grpc-server).

### Version bumps

* kotlin -> `1.9.23`
* kotlinpoet -> `1.16.0`
* okhttp -> `5.0.0-alpha.12`
* okio -> `3.9.0`

Version 4.9.9
-------------

_2024-04-02_

### Common

* Backport JSON serialization for Android SDK below 26 (#2881 by [Damian Wieczorek][damianw])

Version 4.9.8
-------------

_2024-03-18_

### Common

* Retain imports for types used in maps (#2864)

Version 4.9.7
-------------

_2024-02-12_

### Common

* Support all struct types for JSON encoding (#2824 by [Arnaud Frugier][quanturium])

Version 4.9.6
-------------

_2024-02-02_

### Common

* Add public dependencies when encoding the schema (#2801 by [Michael Peyper][mpeyper])

### Kotlin

* Fix RealGrpcCall timeout (#2816 by [Jeff Gulbronson][JGulbronson])

Version 4.9.5
-------------

_2024-01-27_

### Common

* Fix over pruning imports when only option types are being extended in a file (#2800 by [Michael Peyper][mpeyper])

Version 4.9.4
-------------

_2024-01-26_

### Common

* Added `linuxArm64` target to wire-runtime module (#2733 by [Shay Oinif][ShayOinif])
* Add `ProtoReader` API for length-delimited stream reads (#2747 by [Jake Wharton][JakeWharton])
  * You can now call `ProtoReader#nextLengthDelimited` to read and return the length of the next message in a length-delimited stream.
* Ensure `ConsoleWireLogger` does not print anything if in quiet mode (#2754 by [Dimitris Koutsogiorgas][dnkoutso])
* Provide a built in public `EmptyWireLoggerFactory` class (#2776 by [Dimitris Koutsogiorgas][dnkoutso])
* Stop logging includes and excludes (#2777)
* Implement equals/hash for Duration and Instant on native and JS (#2781)
* Prune imports no longer used by retained entities in the schema (#2797 by [Michael Peyper][mpeyper])

### Kotlin

* Generate Grpc `SERVICE_NAME` as const (#2773 by [Marius Volkhart][MariusVolkhart])
* Use `%N` in `KotlinGenerator` to ensure names are escaped (#2784 by [Egor Andreevich][Egorand])
* Add `escapeKotlinKeywords` parameter to Kotlin target (#2785 by [Egor Andreevich][Egorand])
  * You can now set `escapeKotlinKeywords = true` within our `kotlin {}` targets to escape Kotlin keywords with backticks rather than having them suffixed with an `_`.
* Don't retransmit a `PipeDuplexRequestBody` (#2791)
  * We had crashes that occurred when OkHttp attempted to retry sending the request body of a gRPC streaming connection.

### Swift

* Remove deprecated cruft (#2721 by [Adam Lickel][lickel])
* Always qualify names for types that conflict with common Swift types (#2764 by [Dimitris Koutsogiorgas][dnkoutso])
* Fix edge case for heap based messages using a redacted description field (#2772 by [Dimitris Koutsogiorgas][dnkoutso])
* Change `unknownFields` type to `[UInt32: Data]` from just `Data`. (#2789 by [Dimitris Koutsogiorgas][dnkoutso])
* Introduce a `ProtoExtensible` protocol that all messages that have been extended conform to. (#2790 by [Dimitris Koutsogiorgas][dnkoutso])

### Announcement

Wire 5.0 is happening soon. This will happen:

* `SchemaHandler.Factory#create()` will be removed. Override its counterpart for painless migration `SchemaHandler.Factory#create(includes: List<String>, excludes: List<String>, exclusive: Boolean, outDirectory: String, options: Map<String, String>)`.
* The module `wire-grpc-server` will be extracted into its own repository: https://github.com/square/wire-grpc-server/
  * Here is how one should migrate: // TODO

Version 4.9.3
-------------

_2023-11-23_

* Breaking: Encoding and decoding of negative `Int32`s was broken in Swift and did not match protobuf spec.
    Negative `Int32`s are now correctly encoded as unsigned `Int64`s. Prior versions of Wire that have
    serialized a proto containing negative `Int32` fields will not be correctly decoded using this
    version onwards.
* Fix: Swift `ProtoDefaulted` was incorrectly applied in cross-module contexts
* Fix: Message's Builder won't break compilation on multiplatform projects.
* Fix: No hanging anymore on gRPC when receiving an unexpected response.

Version 4.9.2
-------------

_2023-11-13_

* New: Opaque types. You can now specify types you want Wire to evaluate as being of type `bytes`.
    On code generation, the fields of such types will be using the platform equivalent of `bytes`,
    like `okio.ByteString` for the JVM. Use this if there's a dependency heavy type which you do
    not use. Note that scalar types cannot be opaqued.
* New: Adds a closure into generate types allowing the creation of an instance via the Kotlin DSL.
* Fix: Don't arbitrarily prune `oneOf` options.
* Change: Swift `Defaulted` has been renamed `CustomDefaulted`
* New: Swift `ProtoDefaulted` property wrapper and `ProtoDefaultedValue` protocol
  * Similar to `CustomDefaulted, this adds as projection of the protocol defined default value
  * This should not take up any additional storage
  * This is attached to optional scalar values and messages with entirely optional values
* New: `ProtoDefaulted` and `CustomDefaulted` include setter support
  * This enables you to do something like `Foo().$bar.$baz += 1`
* Change: Swift `ProtoEnum` types now have a raw value of `Int32`.
  * The runtime and generated code both need to be updated to reflect this.

Version 4.9.1
-------------

_2023-09-26_

* Fix: Missing Swift imports.

Version 4.9.0
-------------

_2023-09-20_

* Fix: Swift generates all Storage properties. This mitigates performance issues with dynamicMemberLookup
* Change: Swift codegen was reordered for readability
* Change: the type of the Wire option `use_array` has been changed from `string` to `bool`, and its tag has changed from 1180 to 1185.
* New: Swift propertyWrapper @CopyOnWrite. @Heap is now deprecated and will be removed in November 2024.
* New: Wildcards are supported when pruning members such as `my.namespace.Type#another.namespace.*`. This is handy if one wants to prune extension fields.
* New: Adds warning when one defines a protoLibrary without source sets, which happens on native and JavaScript platforms.
* New: `project(..)` dependencies are now explicitly limited to the JVM, for otherwise Wire isn't able to pick the correct platform to resolve them.
* New: Wire runtime proto files, such as `google/protobuf/descriptor.proto` or `wire/extensions.proto` will not be emitted anymore when `protoLibrary = true`.
* New: A custom logger can now be configured via the WireCompiler.
* New: `wire-schema`, and `wire-schema-tests` are now available on JavaScript and native platforms.
* Update: Okio to `3.5.0`. All `java.nio` file systems should now be supported when using Wire.
* Update: Kotlin to `1.9.10`, Gradle to `8.x`, and many others to most recent versions.

Version 4.8.1
-------------

_2023-08-17_

* New: Swift messages now have the form `init(REQUIRED FIELDS, (inout Storage) -> Void)`
* New: Swift, the member-wise initializer has been removed by default. It can be re-enabled by defining `WIRE_INCLUDE_MEMBERWISE_INITIALIZER`; however, it will be removed in November 2024. See https://github.com/square/wire/pull/2561 for details
* Fix: Correctly define sources folders vs. resources folders for Wire generated code.
* Fix: Generated `.proto` are correctly added to the built artifact.
* New: All options of KotlinTarget available on CLI.

Version 4.8.0
-------------

_2023-07-21_

* Fix: JSON to Kotlin deserialization is now _really_ bullet-proofed against
  `Class.getDeclaredFields` random ordering.
* Fix: proto3 types (`Duration`, `Struct`, etc) are now supported when doing dynamic serialization.
* Fix: `GrpcStatus` is now serializable which enables `GrpcException` serialization.
* New: `GrpcClient` is now abstract. You can customize how network calls are implemented.
* New: You can now pass an [event listener][event_listener] to receive metric events.
* New: New option for the Wire Gradle plugin. `rejectUnusedRootsOrPrunes = true` will fail the build if
  any roots or prunes are not used when refactoring the schema. This can help discover incorrect
  configurations early and avoid mis-expectations about the built schema.
* New: `OneOf`'s options are now loaded to the schema.
* New: Wire will now fail if it detects a type name collision which can happen if a same file is loaded from different paths.
* New: `wire-schema-tests` is now multiplatform.
* New: `SchemaHandler.Factory` can now receive payload set within the Wire Gradle plugin. Implement
  the method `fun create(includes, excludes, exclusive, outDirectory. options): SchemaHandler` to receive it.
* New: custom targets can now pass custom payloads to their `SchemaHandler.Factory`. The `custom {}`
  takes a map<string, string> to its new field `options`.
* Swift: Default values are now generated via a `Defaulted` property wrapper.
* Swift: Fully-qualify `Foundation.Data` to prevent name collisions with messages named `Data`.
* Move: Some types have been moved from `wire-compiler` to `wire-run`: `WireRun`, `Target`, `DirectedAcyclicGraph`, `PartitionedSchema`.
* Dependency: KotlinPoet has been bumped to `1.14.2`.

Version 4.7.2
-------------

_2023-06-18_

* Swift: Resolve Redactable regression.

Version 4.7.1
-------------

_2023-06-16_

* Swift: Heap types should use `DynamicMemberLookup` for storage.
* Fix: Don't drop the wire_package in a `protoPath`.
* Fix: Normalize conflict strategy for boxed `oneof` field names.
* Fix: Support all the scalar types in `SchemaEncoder`.

Version 4.7.0
-------------

_2023-05-24_

* New: Add a dry run option. If enabled, the compiler will just emit the names of the source files that would be
  otherwise * generated to stdout. You can use the flag `--dry_run` with the Wire compiler or define the option with
  Gradle as the following:

  ```kotlin
  wire {
    dryRun = true
  }
  ```
* Fix: Correctly set task dependencies on `processResources` if `protoLibrary` is set to true.
* Fix: If a valid grpc-status header is present, raise a GrpcException rather than an IOException.

Version 4.6.2
-------------

_2023-05-11_

* Fix: Explicitly adds `jvm` variants of multiplatform artifacts into the BOM.
* Fix: Produce Descriptors for messages without fields after pruning.

Version 4.6.1
-------------

_2023-05-10_

* Fix: Handle length-prefixed messages of length 0.
* Swift: Bump to `1.5`.

Version 4.6.0
-------------

_2023-05-01_

* Fix: the version `4.5.6` had a breaking change which has been reverted in `4.6.0` without losing
  any functionality.
* Update `descriptor.proto` from the `7dbe742` version of `protocolbuffers/protobuf`.

Version 4.5.6 (Note that this holds a breaking change, update directly to `4.6.0` to avoid it.)
-------------

_2023-04-25_

* Fix: JSON serialization is now bullet-proofed against `Class.getDeclaredFields` random ordering.
* Perf: Add option `wire.use_array` to use primitive arrays for packed scalars. This should improve
  performances as it avoids autoboxing on the JVM. It will use the appropriate array type, for
  example `repeated float` would be represented as a `FloatArray`.

Version 4.5.5
-------------

_2023-04-12_

* Perf: Optimize packed scalar list sizes in initial allocation.

Version 4.5.4
-------------

_2023-03-31_

* Kotlin: bump to `1.8.20`.

Version 4.5.3
-------------

_2023-03-23_

* Swift: fix proto3 messages not always omitting fields with default values.
* Multiplatform: Builders and their methods are now generated if the configuration requires it.
* Multiplatform: No more duplicate generations, or wrong tasks naming. Task dependencies are fixed.
* Multiplatform: Added `mingwX64` target.
* Kotlin: bump to `1.8.20RC`.

Version 4.5.2
-------------

_2023-03-06_

* Fix: Use `rawType`'s classloader when accessing `ProtoAdapter`s.
* gRPC: Decode `grpc-status-details-bin` and add it to `GrpcException`.
* Swift: Fix base64url decoding when string length is a multiple of 4.
* JSON: Support deserialization of proto3 default param when absent.

Version 4.5.1
-------------

_2023-02-11_

* Fix swift Copy-on-Write behavior.
* Support deserialization of camelCased fields in proto2.

Version 4.5.0
-------------

_2023-02-02_

* Add support for `tvOS`.
* Add ability to supply custom schema handler from command line.

### Swift

* Add `Sendable` conformance to all generated types.
* Improved support for Proto3 types such as `Struct`, `Any`, `Duration`, and `Timestamp`.
* `@JsonString` has been deprecated and will be removed in a future release.
* Codable conformance has been completely rewritten and should be much more compliant to the spec.
    * This is a somewhat breaking change and heavily dependent upon the types you were using.
    * Notably: Dictionaries with non-string keys are not forwards compatible with new models.
    * The corrected encoding are not backwards compatible with old models.
    * Encoding fixes:
        * `unknownFields` should never be encoded any more.
        * Don't encode default values by default (configurable).
        * Encode field names in camelCase by default (configurable).
        * Encode Enums as Strings by default (configurable).
        * Maps will now always encode their keys as Strings.
    * Decoding fixes:
        * Data now supports both base64 and base64url binary formats.
        * Enums can now be decoded from either String or UInt32 values.
        * Unknown Enum values can optionally be skipped.
        * Both camelCase and snake_case key names are accepted. camelCase is preferred.

Version 4.4.3
-------------

_2022-10-26_

* Fix: No more NPEs when the building project uses Kotlin `1.7.20`.

Version 4.4.2
-------------

_2022-10-06_

* Fix: Use KotlinProjectExtension to access source sets.
* Fix: Improve support for ZIP artifacts.
* Fix: When set to true, `buildersOnly` will take precedence over `javaInterop` when generating Kotlin.
* Fix: Support nested extensions in a message.

Version 4.4.1
-------------

_2022-08-05_

* New: Check HTTP2 protocol is set for passed client to `GrpcClient`.
* New: Add buildersOnly option for Java and Kotlin target, setting it to true will change the
  visibility of generate types' constructor to non-public.
* Fix: Properly define api dependency for `okio` in `wire-schema`.
* Fix: Sort input locations so they match on all platforms.
* Fix: Avoid NPEs with kotlin `1.7.20` around source sets.
* Update: Bumped KotlinPoet to `1.12.0`.

Version 4.4.0
-------------

_2022-06-07_

* New: Custom schema handlers! Wire lets you now plug in your own logic to deal with the protobuf
    schema the way you want. Check [our documentation][custom-handlers-doc] for details. You can
    also check our [recipe directory][custom-handlers-recipes] for examples.
    Note that this API obsoletes the `CustomHandlerBeta` type Wire had until now.
* New: You can now easily create an in-memory protobuf schema with the new `SchemaBuilder` class.
    This lives in the new `wire-schema-tests` artifact. For usage examples, check the tests in
    [custom handler recipes][custom-handlers-recipes].
* Breaking: the `wire-profiles` artifact has been removed and is now inlined in `wire-schema`.
* Breaking: `CoreLoader` 's `isWireRuntimeProto` methods are now static.
* Breaking: `SchemaLoader` and related classes have been moved from `wire-compiler` to
    `wire-schema`.
* New: Support packed and map fields when converting to/from JSON with Kotlin.
* New: Support typesafe accessors and version catalogs in Wire plugin.
* New: Generate annotations for repeated options.
* New: Allow parsing of `oneof` options.
* New: Support map fields in options.
* New: Add macosArm64 support to the KMP projects supporting mac.
* Fix: Properly deal with maps of scalar types, deserializing missing scala key/value into identity.
* Fix: Fix a crash where ProtoMember was populated with the wrong data.

Version 4.3.0
-------------

_2022-03-24_

* New: reserved tags and names in enum types are now honoured by Wire.
* Fix: `max` keyword is now correctly parsed for enum types.
* Fix: Wire now writes minus double zeros and minus float zeros on proto3.
* Fix: Wire doesn't write google wrappers types' identity values anymore.
* Fix: `CoreLoader` correctly loads `.proto` files present in the resources of the project.
* Fix: Propagate `GrpcExceptions` in `GrpcCalls`.
* Fix: Change `GrpcCall.isCanceled` to honor OkHttp cancellations.

Version 4.2.0
-------------

_2022-02-17_

 * New: Publish a [bill of materials (BOM)][bom] for Wire. Depend on this from Gradle or Maven to
   keep all of your Wire artifacts on the same version, even if they're declared via transitive
   dependencies. You can even omit versions when declaring other Wire dependencies.

    ```kotlin
    dependencies {
      implementation(platform("com.squareup.wire:wire-bom:4.2.0"))
      implementation("com.squareup.wire:wire-compiler")      // No version!
      implementation("com.squareup.wire:wire-gradle-plugin") // No version!
      implementation("com.squareup.wire:wire-grpc-client")   // No version!
      // Etc.
    }
    ```

Version 4.1.1
-------------

_2022-02-15_

 * Fix: Print target name in `ConsoleWireLogger`.
 * Fix: Throw early when field or constant names start with a digit.
 * Update: Bumped SwiftPoet to `1.3.1`.
 * Fix: Wire will not generate annotations for `repeated` options.

Version 4.1.0
-------------

_2022-01-28_

 * New: When using Wire JSON factories, you can now override the proto3 behavior of skipping default
   values when writing JSON. Set `writeIdentityValues` to true for either `WireJsonAdapterFactory`
   or `WireTypeAdapterFactory` to enable it.
 * Breaking: WireLogger methods has been refactored to remove platform dependencies and allow more
   precise callbacks. We might add new methods in the future for better logging still.
 * Removal: The `dry-run` option on `WireCompiler` has been removed.
 * Swift: WireCompiler podspec is now backup by a jar so consumers will not have to locally build it
    on `pod install`.

Version 4.0.1
-------------

_2021-12-07_

 * Fix: Don't fail at compilation when an enum is in a `oneof` for Swift generation.

Version 4.0.0
----------------------

_2021-12-03_

 * New: Add 'nameSuffix' parameter for configuring generated service-class names in Kotlin.
 * New: Define `oneofName` in `@WireField`.
 * New: Enable iosSimulatorArm64 for Kotlin multiplatform.
 * New: Expose the source `.proto` file at `ProtoAdaper.sourceFile`. This is null for built-in types
   and types generated prior to this release.
 * New: Generate Kotlin code whose members match the declaration order of the corresponding `.proto`
   files. In previous releases, generated members were sorted by kind (fields, oneofs), then by
   declaration order. With this update only declaration order is used. **Note that this will change
   the encoded-bytes of these messages.** This change is both forwards and backwards-compatible.
   Identical encoding of equal messages across Wire releases is typical but not guaranteed, and this
   is a rare release that changes that encoding. If you do cryptographic hashes on encoded proto
   messages, you will notice that the hashes are different in this release.
 * New: Option in `SchemaLoader` to exhaustively load imported files. By default we only load what's
   immediately necessary to generate code; this new option loads everything reachable into the
   schema.
 * New: Programmatic API to prune schemas. See `Pruner` in wire-schema.
 * New: SchemaLoader doesn't extend the `Closeable` interface anymore.
 * New: Support `rpcRole = 'none'` in the Gradle plugin to generate neither client nor server code.
 * New: Support for Android variants.
 * New: Support for glob syntax in srcJar includes.
 * New: Support for special float literals.
 * New: Swift support `Timestamp` and `Duration`.
 * New: The Wire plugin requires an output to be set. Before, it would generate Java code by
   default; it will now throw if there are no [output][wire-customizing-output] defined.
 * New: The default value of `emitAppliedOptions` for our Java and Kotlin target is now set to true.
 * New: Wire should build and execute properly on Windows.
 * New: `@WireRpc` has a new `sourceFile` attribute.
 * New: `GrpcClient.Builder.minMessageToCompress()` configures which messages are compressed. This
   will completely disable compression if the size is `Long.MAX_VALUE`. We've seen problems where
   some Golang gRPC servers don't support compression; setting this to `MAX_VALUE` is necessary to
   interop with them.
 * New: `SchemaReflector` is our initial implementation of the
   [gRPC Server Reflection Protocol][reflect]. Note that although we implement the business logic of
   gRPC reflection, we don't offer a gRPC server built into Wire.
 * New: `wire-reflector` bundles gRPC's `reflection.proto` which it is built upon.
 * New: `wire-runtime` exposes a `com.squareup.wire.VERSION` constant reflecting the project version.
 * New: change the Gradle plugin so that (unstable) custom handlers can be configured with instance
   instead of with a class name.
 * Fix: Be more aggressive about loading transitive files with `SchemaLoader.loadExhaustively`.
 * Fix: Bugs in JSON serialization of builder-less Kotlin types have been addressed.
 * Fix: Compile Kotlin/JS with both LEGACY and IR compilers.
 * Fix: Deep copy metadata on `GrpcCall.clone()`.
 * Fix: Don't break task caching by using absolute paths in the Gradle plugin. Wire now uses
   project-relative paths in any attribute that is used as a cache key.
 * Fix: Don't crash encoding schemas when an option contains a repeated field, an enum, or a double.
 * Fix: Don't depend on moshi-kotlin in wire-moshi. This caused a transitive dependency on
   kotlin-reflect, which we neither needed nor wanted.
 * Fix: Don't generate invalid code when an enum constant is named `name` or `ordinal`.
 * Fix: Don't re-use the cache if protobuf inputs have changed.
 * Fix: Emitting proper protobuf format for option values defined as a list of enum constants.
 * Fix: Explicitly defined Wire gRPC server generation as experimental: the feature isn't complete.
 * Fix: Generate `@Deprecated` annotations on deprecated messages, fields, enums, and enum
   constants.
 * Fix: Handle out of order proto fields when initializing Kotlin constructors.
 * Fix: Handle writing/reading exceptions for duplex calls in Wire gRPC.
 * Fix: In Java, rename instances to avoid field and class name conflicts.
 * Fix: Locate files in the root package when importing.
 * Fix: Memory fixes found with Address Sanitizer in Swift.
 * Fix: Permit values other than `0` and `1` when decoding protobuf-encoded booleans. Previously we
   threw an `IOException` for other values; now all non-zero values are true.
 * Fix: Redact boxed `OneOf` fields.
 * Fix: Redacted Kotlin scalars now respect nullability.
 * Fix: Retain field order when emitting a schema as `.proto` files.
 * Fix: Reverse the topological sort of dependent files in `SchemaReflector`. We had problems with
   `grpc-curl` which expects the requested file to be listed first.
 * Fix: Support Kotlin-generated annotations on Java fields.
 * Fix: Support for serializing builder-less Kotlin generated classes to JSON.
 * Fix: Support reporting errors in `CustomHandlerBeta`.
 * Fix: Suppress deprecation warnings on generated enum's `fromValue` method in Kotlin.
 * Fix: Swift adapters will throw an error when encountering an unexpected
   `ProtoReader.beginMessage()` rather than calling `fatalError()`.
 * Fix: Update the Wire Gradle plugin to clear the output directory before generating code. This
   prevents the need to do a clean build after removing a message type.
 * Fix: Update the Wire Gradle plugin to register generated `.java` sources with the Java compiler.
   Previously this was broken if the Kotlin plugin was installed.
 * Fix: Use Gradle's logging mechanism to reduce output when Wire generates code.
 * Fix: Use correct type when referencing a custom adapter in Kotlin generated code.
 * Fix: Use relative path sensitivity and file collection.
 * Fix: Validate enum constant uniqueness for the entire package.
 * Fix: Wire Gradle plugin tasks have been modernized with configuration caching support.
 * Fix: Wire will not generate respective built-in types for Java, Kotlin, and Swift generation.
   Those are usually the google types for which Wire will provide its own implementation.
 * Upgrade: Update KotlinPoet to `1.8.0`.
 * Upgrade: [OkHttp 4.9.3][okhttp_4_9_3].
 * Upgrade: [Okio 3.0.0][okio_3_0_0]. We now use Okio 3's `FileSystem` in `SchemaLoader`, which
   makes it easier to load `.proto` files from the classpath. This is binary-incompatible with the
   Okio 3.0 alpha releases.
 * Bye: Drop support for emitKotlinxSerialization.

Version 3.7.1
-------------

_2021-11-03_

 * Fix: Properly load schema located at root without packages.

Version 3.7.0
-------------

_2021-03-25_

 * New: `srcProject(":project-name")` makes it easier to depend on `.proto` files of other projects.
 * Fix: Don't require source that `.proto` directories exist at Gradle plugin configuration time.
   This was preventing Wire from using other tasks' outputs as its inputs.
 * Fix: Don't fail if options have a `.` prefix.

Version 3.6.1
-------------

_2021-03-09_

 * Fix: The Wire Gradle plugin now supports Java only Android projects.
 * Fix: In the Wire Gradle plugin, `sourcePath` will now include only protos defined with `include`
   if the option is present. It used to include all existing `.proto` files even if `include` was
   used.
 * New: Full support Optional Int64 and UInt64 for JSONString in Swift.

Version 3.6.0
-------------

_2021-02-08_

 * New: Automatically add a dependency when a `protoPath` or `protoSource` depends on a project.
 * New: `protoPath` and `protoSource` dependencies are now *not* transitive by default.
 * New: New protoLibrary option for the Wire Gradle plugin. Configuring a project as a protoLibrary
   will cause the generated `.jar` file to include `.proto` sources.
 * New: Code generation for plain gRPC server. The Kotlin target now has a new
   `grpcServerCompatible` option which if set to `true` will generate gRPC server-compatible
   classes.
 * New: Introduce `GrpcException`.
 * New: Add GrpcMethod tag to the request.
 * New: Adds redacting support for Moshi JSON adapters.
 * New: Publish plugin marker for Gradle plugin.
 * Fix: Escape square brackets in Kotlin generated code documentation.
 * Fix: Improved proto parsing performance.

Version 3.5.0
-------------

_2020-10-27_

 * New: Wire Gradle plugin improvements:
   - A task is now created for each available sources (main, Android variants, etc).
   - The `wire-runtime` dependency is automatically added.
   - Generated code directories are automatically added into their module's source sets.
 * New: Wire's proto parser now knows about `oneOfOptions`.
 * New: Wire will throw when two enum constants are ambiguous, like `ZERO` and `zero`.
 * New: Bytes options are not eligible anymore as annotation members.
 * Fix: Optional fields in proto3 are now generated as nullable fields.
 * Fix: JSON camel-casing is updated to fit latest protobuf specifications.
 * Fix: Exception messages when gRPC fails have been improved.
 * Fix: Allow `;` as entry separator in option maps.
 * Fix: Enum constants are now properly escaped when conflicting with keywords of their generated
 target platform.
 * Fix: Update to KotlinPoet 1.7.2 which makes a lot of change in how Kotlin code is generated.

Version 3.4.0
-------------

_2020-09-24_

 * New: Stop emitting enum constant options as fields for Kotlin.
 * New: The Wire Gradle plugin task is now cacheable.
 * New: New GrpcCall function to help implement fakes.
 * New: Change GrpcStreamingCall.execute() to support structured concurrency.

Version 3.3.0
-------------

_2020-09-14_

 * New: Proto3 support! This includes the new behaviors, the new types, and the JSON.
 * New: Swift support for proto2 schemas. The details are in our [blog post][swiftblogpost].
 * New: Wire will now throw an error when:
   * two generated files end up overriding each other,
   * imports form a cycle,
   * packages form a cycle. This can be turned off with the flag `permitPackageCycles`,
   * an option within the source set cannot be resolved,
   * there are name duplications of members in a message, or of rpcs in a service,
   * a map is used as an extension.
 * New: Support for the `json_name` pseudo option.
 * New: The `wire_package` file option allows one to set the JVM package where classes generated
   from the concerned file will be placed. `wire_package` takes precedence over `java_package`.
 * New: Lists and maps in Kotlin generated code are now immutable.
 * New: Support UTF-8 with BOM in proto files.
 * New: `wire.since` and `wire.until` have been renamed with the prefix `constant_` for
   `EnumValueOptions`.
 * New: Wire generates 1) annotations for options which 2) gets assigned to the generated code where
   appropriate. Both behavior can be turn on or off via the flags:
   * `emitDeclaredOptions`: True to emit types for options declared on messages, fields, etc.
     Default to true,
   * `emitAppliedOptions`: True to emit annotations for options applied on messages, fields, etc.
     Default to false.
 * Fix: Recursive map values.
 * Fix: Long expressions in equals and encodedSize functions.

Version 3.2.2
-------------

_2020-05-15_

 * Fix: JSON serialization correctly emits all values.

Version 3.2.1
-------------

_2020-05-02_

 * New: `onlyVersion` option on the Wire Gradle plugin to target a unique version. By and large,
   service code that supports many clients would target ranges via `sinceVersion` and
   `untilVersion`, while client code would target a unique version via `onlyVersion`.
 * New: Support for optional fields in Proto3.
 * Fix: Restored the `GrpcClient.create` API to create implementations for gRPC interfaces.

Version 3.2.0
-------------

_2020-04-23_

 * New: `wire.since` and `wire.until` options on members and enum values. You can prune fields or
   constants using these two options. When generating code with the Wire Gradle plugin, define
   `sinceVersion` and/or `untilVersion` to scope the generated code.
 * New: Messages' `toString` method on Kotlin and Java now escape string values for easy parsing.
 * Fix: Link the entire `descriptor.proto` every time when building the `Schema`.
 * Fix: Properly handle members named after keywords of the target language for both Java and
   Kotlin.
 * Fix: Use the declared name for keys in JSON when emitting/reading keyword named members.
 * Fix: Generated Kotlin code is malformed for long identifiers.
 * Fix: Make the Wire Gradle plugin compatible with instant execution.

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


 [Egorand]: https://github.com/Egorand
 [Endeavour233]: https://github.com/Endeavour233
 [JGulbronson]: https://github.com/JGulbronson
 [JakeWharton]: https://github.com/JakeWharton
 [MariusVolkhart]: https://github.com/MariusVolkhart
 [ShayOinif]: https://github.com/ShayOinif
 [aaron-edwards]: https://github.com/aaron-edwards
 [amorde]: https://github.com/amorde
 [bom]: https://docs.gradle.org/6.2/userguide/platforms.html#sub:bom_import
 [custom-handlers-doc]: https://square.github.io/wire/wire_compiler/#custom-handlers
 [custom-handlers-recipes]: https://github.com/square/wire/tree/c3c5f559556ad9d41582a0e0a025679b5493f7aa/wire-library/wire-schema-tests/src/test/java/com/squareup/wire/recipes
 [damianw]: https://github.com/damianw
 [dnkoutso]: https://github.com/dnkoutso
 [event_listener]: https://github.com/square/wire/blob/3e300c492a74e80260581e3aee624d9cf3e5b1f8/wire-schema/src/commonMain/kotlin/com/squareup/wire/schema/EventListener.kt#L105-L111
 [frojasg]: https://github.com/frojasg
 [javapoet]: https://github.com/square/javapoet
 [jimfs]: https://github.com/google/jimfs
 [lickel]: https://github.com/lickel
 [liutikas]: https://github.com/liutikas
 [mpeyper]: https://github.com/mpeyper
 [okhttp_4_9_3]: https://github.com/square/okhttp/blob/master/CHANGELOG.md#version-493
 [okio_3_0_0]: https://square.github.io/okio/changelog/#version-300
 [okio_3_0_0_a_3]: https://square.github.io/okio/changelog/#version-300-alpha3
 [quanturium]: https://github.com/quanturium
 [reflect]: https://github.com/grpc/grpc/blob/master/doc/server-reflection.md
 [sashaweiss-signal]: https://github.com/sashaweiss-signal
 [swiftblogpost]: https://cashapp.github.io/2020-08-19/wire-support-for-swift-part-1
 [tikurahul]: https://github.com/tikurahul
 [traviscj]: https://github.com/traviscj
 [wire-customizing-output]: https://square.github.io/wire/wire_compiler/#customizing-output
