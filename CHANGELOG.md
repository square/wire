Change Log
==========

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
