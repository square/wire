Change Log
==========

Version 2.1.2 *(2016-04-15)*
----------------------------

 * Fix: Gson type adapter now deserializes JSON null literals to empty list for repeated fields.


Version 2.1.1 *(2016-02-01)*
----------------------------

 * New: `reserved` keyword is now supported and enforced.
 * Fix: Defer reflection-based lookup of enum method until first use to avoid
   class loading race conditions.
 * Fix: Support single-quoted string literals.
 * Fix: Adjacent string literals are not correctly concatenated.


Version 2.1.0 *(2016-01-18)*
----------------------------

 * **Empty lists of packed values were being encoded incorrectly.** In Wire 2.0.x our message
   adapters incorrectly included empty lists for `[packed=true]` rather than omitting them. This is
   now fixed.
 * New: `Message.encode()` to concisely encode a message.
 * New: `MessageAdapter.decode(ByteString)` to decode a message from a byte string without an
   intermediate byte array.
 * New: Wire now includes a sample code generation for service interfaces.


Version 2.0.3 *(2016-01-04)*
----------------------------

 * New: `ProtoAdapter.get` overload which returns an adapter given an instance of a message.
 * New: `@Nullable` annotations are emitted for `optional` fields when using `--android`.
 * Fix: Remove the need for `javac` to generate synthetic accessor methods in the generated code.
   This results in smaller code size and less method references (for Android users).


Version 2.0.2 *(2015-12-14)*
----------------------------

 * Fix: Exclude unknown fields when encoding JSON and drop unknown fields when parsing JSON.
 * Fix: Ensure JSON encoding and decoding works in the default generation mode (not just
   `--compact`) by always adding `@WireField` metadata to message fields.
 * Fix: Update to JavaPoet 1.4 for more accurate generation of valid Java code.


Version 2.0.1 *(2015-11-12)*
----------------------------

 * Fix: Do not emit `case` statements for aliased enum constant values. The first constant for a
   value will be returned when deserializing.
 * Fix: Emit `@Deprecated` annotation on deprecated enum constants.
 * Fix: Correctly prune dependencies of excluded message, enum, or service members. Previously
   the dependencies of an excluded member were retained despite the member itself being omitted.


Version 2.0.0 *(2015-10-23)*
----------------------------

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


Version 1.8.0 *(2015-06-27)*
----------------------------

 * New: `oneof` support!
 * Fix: Correct serialization of repeated unknown fields.
 * Fix: Removed superfluous `private` modifier on enum constructor.
 * Warning: The 'protoparser' library was updated to version 4.0. This changes the type passed to
   any `ServiceWriter` implementations.


Version 1.7.0 *(2015-03-05)*
----------------------------

 * New: Messages implement `Serializable`. The serialized form follows protobuf encoding, so
   renaming fields is safe as long as tags are consistent. (Renaming classes is unsafe, however).
   Note that extension fields are deserialized as unknown fields.

Version 1.6.1 *(2015-01-16)*
----------------------------

 * New: `--quiet` and `--dry-run` command-line arguments.
 * Fix: Handle an extension registry with zero or only one item.
 * Okio dependency bumped to 1.2.0.


Version 1.6.0 *(2014-10-23)*
----------------------------

 * Fix: Correctly throw `IOException` when parsing bad input fails.
 * Fix: Ensure emitted code references to `Arrays.asList` correctly compiles in some edge cases.
 * '__UNDEFINED__' enum value has been removed.


Version 1.5.2 *(2014-09-15)*
----------------------------

 * New: '__UNDEFINED__' enum value represents values that the generated code is unable to handle.
 * New: Service writer implementation can now be specified on the command-line.


Version 1.5.1 *(2014-06-18)*
----------------------------

 * New: Generate interface definition for a `Service` with a partial list of methods.
 * Okio dependency bumped to 1.0.0.


Version 1.5.0 *(2014-04-22)*
----------------------------

 * New: Options on enums and enum values are now supported.
 * New: Options ending with `.redacted` on fields will omit values from `toString`.
 * New: `Redactor` creates copies of messages with redacted fields omitted.
 * Various internal serialization improvements and memory reduction.
 * Okio dependency bumped to 0.9.0.


Version 1.4.0 *(2014-04-22)*
----------------------------

 * Replace Wire's ByteString class with the one from Okio (https://github.com/square/okio).
   **This is a breaking API change**.

 * Add a new static method `Wire.parseFrom(okio.Source input, Class<M> messageClass)`.

 * Reimplement Wire's input handling to use Okio internally.

 * Provide basic support for generating code from service declarations (see README.md).

 * Improve deserialization performance.

 * Fix a bug where no some extension dependencies were not detected.


Version 1.3.3 *(2014-03-28)*
----------------------------

 * New: Support service declarations as roots. The request and response types of their methods will
   be included.


Version 1.3.2 *(2014-03-27)*
----------------------------

 * Fix: Enum value duplicate check now correctly looks at names instead of values.


Version 1.3.1 *(2014-03-25)*
----------------------------

 * New: Automatically add Maven plugin's generated source as a compilation root.
 * Fix: Correct Maven plugin's 'no arguments' flag to work properly.
 * Fix: Support extend declarations nested inside message types.


Version 1.3.0 *(2014-03-21)*
----------------------------

 * New: Empty repeated fields are now initialized to an empty collection.
 * New: Emit field options. Use `--no_options` flag to disable.
 * New: `@Deprecated` is now added to fields and setters on the builder.
 * New: Maven plugin for running the compiler as part of a build.
 * Fix: Treat empty and null repeated fields the same in `equals()` and `hashCode()`.

Note: This version is not backwards compatible with code generated from previous versions.


Version 1.2.0 *(2013-11-01)*
----------------------------

 * New: `--registry_class` compiler argument emits a class with a list of extensions suitable for
   passing to the `Wire` constructor.
 * Fix: Ensure all trailing whitespace is trimmed on documentation.


Version 1.1.1 *(2013-10-23)*
----------------------------

 * Fix: Correct parsing and emission of multi-line documentation.


Version 1.1.0 *(2013-10-22)*
----------------------------

 * New: Support for message options.
 * New: Check for duplicate field tag numbers.
 * New: Emit Javadoc on messages, enum values, and builder methods.
 * Fix: Emit imports for extension fields and classes outside of package.
 * Fix: Correctly generate sources for protos with the same basename.
 * Fix: Do not generate default constants for message types.
 * Fix: Avoid shadowing fields named "result", "other", or "o".


Version 1.0.1 *(2013-08-27)*
----------------------------

 * New: Support reading directly from `InputStream`.
 * New: Add '`other == this`' shortcut in generated `equals()` methods.


Version 1.0.0 *(2013-08-23)*
----------------------------

Initial version.


 [jimfs]: https://github.com/google/jimfs
 [javapoet]: https://github.com/square/javapoet
