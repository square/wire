Change Log
==========

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
