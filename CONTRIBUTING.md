Contributing
============

Keeping the project small and stable limits our ability to accept new contributors.
We are not seeking new committers at this time, but some small contributions are welcome.

If you've found a security problem, please follow our [security policy][security] and [bug bounty][bug_bounty] program.

If you've found a bug, please contribute a failing test case so we can study and fix it.

For a new feature or a new generator option, open an issue first and describe the use case.
We agree on the design there before you write the code.

Before code can be accepted all contributors must complete our [Individual Contributor License Agreement (CLA)][cla].
A maintainer asks for it on your pull request if it is missing.


Building and testing
--------------------

Wire builds with the JDK version in [`.github/workflows/.java-version`][java_version].

The fastest loop runs the JVM build only, as the main CI job does:

```
./gradlew -Dkjs=false -Dknative=false -Dkwasm=false -Pswift=false build
```

To work on one module, run its tests and its binary API check:

```
./gradlew :wire-kotlin-generator:test
./gradlew :wire-runtime:apiCheck
```

The full build also needs Xcode for Swift and Kotlin/Native, Chrome for Kotlin/JS, and an Android emulator for `connectedCheck`.
You do not need them locally unless you change those targets.
CI runs all of them on every pull request.

Format the code before you push:

```
./gradlew spotlessApply
```


What CI checks
--------------

Every pull request runs these jobs in [`build.yml`][build_yml]:

 * **jvm**: the JVM build, tests, and binary API check (`apiCheck`).
 * **multiplatform**: the full build on macOS, including Kotlin/Native and Kotlin/JS.
 * **windows**: the full build on Windows.
 * **swift**: the Swift runtime, generated Swift sources, and `swift test`.
 * **android**: `connectedCheck` on emulators from API 21 to API 29.

CI fails if a build step changes a checked-in file.
If that happens, run the generation task that the failure names, review the diff, and commit it.


Where things live
-----------------

 * `wire-schema`: the parser, linker, and pruner that produce a `Schema`.
 * `wire-java-generator`, `wire-kotlin-generator`, `wire-swift-generator`: code generation.
 * `wire-compiler`: the command line compiler and the generator targets (`JavaTarget`, `KotlinTarget`, `SwiftTarget`).
 * `wire-gradle-plugin`: the Gradle plugin, its `wire { }` DSL, and its outputs.
 * `wire-runtime` and `wire-runtime-swift`: the runtime that generated code calls.
 * `wire-golden-files`: generated Java and Kotlin, checked in and compiled.
 * `wire-tests`: runtime behavior tests against generated code.
 * `wire-protoc-compatibility-tests`: tests that compare Wire with `protoc`.


Adding a generator option
-------------------------

Generator behavior belongs on the target, not in the schema.
Add the option to the target and the Gradle DSL, and keep proto options for things the schema itself describes.

An option touches these files:

 * `wire-compiler/src/main/java/com/squareup/wire/schema/Target.kt`: the option on `JavaTarget`, `KotlinTarget`, or `SwiftTarget`.
 * `wire-gradle-plugin/src/main/kotlin/com/squareup/wire/gradle/WireOutput.kt`: the DSL property.
 * `KotlinSchemaHandler.kt` or `JavaSchemaHandler.kt`: pass the option to the generator.
 * `KotlinGenerator.kt` or `JavaGenerator.java`: the behavior.
 * `KotlinWithProfilesGenerator.kt` or `JavaWithProfilesGenerator.kt`: the test helpers that must accept the new parameter.
 * `docs/wire_compiler.md`: document the option.
 * `docs/wire_vs_protoc.md`: update it if the option changes encoded bytes or semantics.
 * The `.api` dumps: run `./gradlew apiDump` and commit the diff.

New behavior is off by default.
Generated code must not change for users who do not set the option.


Tests to add
------------

Pick the tests that match your change:

 * **Generator output**: a test in `KotlinGeneratorTest` or `JavaGeneratorTest` that asserts the emitted source.
 * **Generated code compiles**: a proto in `wire-golden-files/src/main/proto/squareup/wire/` and a `java { }` or `kotlin { }` block in `wire-golden-files/build.gradle.kts`.
   Build the module, then commit the generated files.
 * **Runtime behavior**: a test in `wire-tests/multiplatform` for all platforms, or in a `wire-tests/jvm-*` module for the JVM only.
   Shared fixtures are in `wire-tests/fixtures`.
 * **Encoding defined by the protobuf specification**: a test in `wire-protoc-compatibility-tests`.
 * **JSON**: a test in `wire-tests/wire-json-shared-kotlin-tests`, which runs against every Moshi and Gson configuration.
 * **Gradle plugin**: a project in `wire-gradle-plugin/src/test/projects/` and a test case in `WirePluginTest.kt`.
 * **Swift**: a test in `wire-runtime-swift`, then run `./gradlew generateSwiftProtos generateSwiftTests` and commit the output.

Review every generated file in your diff.
Reviewers read them to see what your change does to users' code.


Compatibility rules
-------------------

Many projects build on Wire, so a change that looks local can break their builds.

 * **Binary API**: `apiCheck` must pass.
   A public API change needs an `apiDump` diff in the pull request.
   Keep old binary signatures when you can.
 * **Generated code and runtime**: if new generated code calls a new runtime function, users must upgrade both together.
   Say so in the pull request so the change log marks it as breaking.
 * **Kotlin version**: Wire compiles with Kotlin language and API version 2.0, and depends on the Kotlin 2.0 standard library.
   Do not add a dependency that is built with a newer Kotlin, because its metadata breaks users on older compilers.
 * **JVM module names**: do not change them.
   Kotlin mangles the module name into internal accessors, and some serializers store those names in persisted data.
 * **Behavior changes**: a change that fixes Wire to follow the protobuf specification is still a behavior change for users.
   Describe it in the pull request so the release notes can warn them.


Pull requests
-------------

Contribute code changes through GitHub by forking the repository and sending a pull request.
Keep each pull request to one change.
We squash all pull requests on merge.

Maintainers write the change log entry at release time.
You do not need to edit `CHANGELOG.md`.

Contributors do not cut releases.
Maintainers follow [`RELEASING.md`][releasing].

 [bug_bounty]: https://github.com/square/wire/blob/master/BUG-BOUNTY.md
 [build_yml]: https://github.com/square/wire/blob/master/.github/workflows/build.yml
 [cla]: https://spreadsheets.google.com/spreadsheet/viewform?formkey=dDViT2xzUHAwRkI3X3k5Z0lQM091OGc6MQ&ndplr=1
 [java_version]: https://github.com/square/wire/blob/master/.github/workflows/.java-version
 [releasing]: https://github.com/square/wire/blob/master/RELEASING.md
 [security]: https://github.com/square/wire/blob/master/SECURITY.md
