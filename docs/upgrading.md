Trying a Release Candidate
==========================

Wire publishes a release candidate, such as `7.0.0-RC01`, before a release that changes behavior or breaks compatibility.
Release candidates are on Maven Central, like every other release.
Try one before the final release, so problems in your build reach us while we can still fix them.

Check the [change log](changelog.md) for entries marked `⚠ Breaking` or `Behavior change`.
Those are the changes most likely to affect you.


Upgrade everything together
---------------------------

The Wire Gradle plugin, the compiler, and the runtime must use the same version.
New generated code can call runtime functions that older runtimes do not have.

In many builds, more than one place sets the Wire version.
Find all of them before you start:

 * The `com.squareup.wire` Gradle plugin version.
 * The `wire-runtime` dependency, or the `wire-bom` platform.
 * Convention plugins or shared build logic that put the Wire compiler on the build classpath.
 * Libraries that you depend on, which can bring their own Wire version.

Use the BOM to keep the runtime artifacts aligned:

```kotlin
dependencies {
  implementation(enforcedPlatform("com.squareup.wire:wire-bom:7.0.0-RC01"))
}
```

`enforcedPlatform` stops another dependency's BOM from moving Wire to a different version.


What to test
------------

A successful compile is not enough.
These problems showed up in real builds during the Wire 7 upgrade, and only the consumer's own tests found them:

 * **Generated code size.**
   Very large messages or oneofs can generate methods that are too large for the JVM.
   Compile all of your protos, not only the ones you changed.
 * **Persisted data.**
   If you store Wire messages with a reflection-based serializer, such as Jackson, read data written by the old version with the new one.
 * **Merge behavior.**
   If you concatenate encoded messages, check that the merged result is what you expect.
   Wire follows the protobuf specification for merging.
 * **Kotlin compiler version.**
   Build with the Kotlin version you actually use, which can be older than the one Wire builds with.
 * **Gradle plugin integration.**
   Run the tests of any build logic that configures the `wire { }` extension, adds custom outputs, or runs KSP on generated sources.
 * **Shrinking.**
   If you ship with R8 or ProGuard, test a shrunk release build, not only a debug build.


Report problems
---------------

[Open an issue][issues] with the release candidate version in the title.
Include the error, the Wire options you use, and a small proto that reproduces the problem if you can.
We publish a new release candidate or the final release after we fix the reported problems.


Snapshots
---------

Snapshots of the development version are available in [the Central Portal Snapshots repository][snap].
Use them to test a fix before the next release candidate.
Your build must be able to reach that repository.

 [issues]: https://github.com/square/wire/issues/new
 [snap]: https://central.sonatype.com/repository/maven-snapshots/
