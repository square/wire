Wire Compiler & Gradle Plugin
=============================

Wire has two key components: a **compiler** that generates source code at build time, and a
**runtime library** that supports the generated code when your program executes. The compiler is
very configurable; this guide explains its features and their use.

Getting Started
---------------

The best way to configure and execute the Wire compiler is via our [Gradle][gradle] plugin. It
requires Gradle 5.5 or newer.

A typical project has `.proto` files in the standard `src/main/proto` directory.

```
src/
  main/
    proto/
      com/
        example/
          pizza/
            pizza_delivery.proto
            pizza.proto
          sales/
            price.proto
            store.proto
          geo/
            address.proto
            country.proto
    kotlin/
      com/
        example/
          pizza/
            PizzaApp.kt
  test/
    kotlin/
      com/
        example/
          pizza/
            PizzaAppTest.kt
build.gradle
```

Because this project uses the default configuration, plugin setup is easy. Just a simple
block in `build.gradle`,

Using plugin application:
```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.wire:wire-gradle-plugin:<version>'
  }
}

apply plugin: 'com.squareup.wire'

wire {
  kotlin {}
}
```

Using the plugins DSL:
```groovy
plugins {
  id 'application'
  id 'org.jetbrains.kotlin.jvm'
  id 'com.squareup.wire'
}

wire {
  kotlin {}
}
```

The plugin will read the `.proto` schema definitions, validate them, link them, and generate Kotlin
code. It uses [KotlinPoet][kotlinpoet] internally to generate compact and legible source files.

![Basic](images/gradle_basic@2x.png)

If `sourcePath` isn't set, Wire will look for `.proto` files in `src/main/proto` by default.

Wire will generate files in `build/generated/source/wire`. It'll also register this directory as a
source directory for the project so the generated sources are compiled by the Kotlin compiler.

Replace `kotlin` with `java` to generate the Java sources instead.


Inputs and Outputs
------------------

Wire can pull source `.proto` schema definitions from a local directory, a local `.jar` file, or an
external artifact specified with [Maven coordinates][maven_coordinates].

```groovy
wire {
  sourcePath {
    srcDir 'src/main/protos'
  }

  sourcePath {
    srcJar 'lib/pizza-protos.jar'
  }

  sourcePath {
    srcJar 'com.example.pizza:pizza-protos:1.0.0'
  }

  ...
}
```

If your resource or directory contains more `.proto` files than you need to compile you can
constrain the inputs to specific files:

```groovy
wire {
  sourcePath {
    srcDir 'src/main/protos'
    include 'com/example/pizza/pizza_delivery.proto'
    include 'com/example/pizza/pizza.proto'
  }
}
```

Wildcards (with two stars) are also accepted:

```groovy
wire {
  sourcePath {
    srcDir 'src/main/protos'
    include 'com/example/pizza/**'
  }
}
```

You can also do the opposite and exclude some files:
```groovy
wire {
  sourcePath {
    srcDir 'src/main/protos'
    exclude 'com/example/juice/**'
    exclude 'com/example/pizza/toppings.proto'
  }
}
```

Wire can emit multiple languages in the same build. Use `includes` to specify which types are
emitted for a target language; subsequent languages will emit what's left over.

```groovy
wire {
  ...

  kotlin {
    // Kotlin emits the matched types only.
    includes = ['com.example.pizza.*']
  }
  java {
    // Java gets everything else!
  }
}
```

Note that sources are specified as file system paths (slash delimiter) and targets are specified as
Proto type names (dot delimiter).

![Inputs and Outputs](images/gradle_inputs_and_outputs@2x.png)

You may also specify targets with excludes. If you have both includes and excludes, excludes take
precedence.

```groovy
wire {
  ...

  kotlin {
    // Kotlin emits everything but sales and geo packages.
    excludes = ['com.example.sales.*', 'com.example.geo.*']
  }
  java {
    // Java gets those because they're left over.
  }
}
```

Wire will print a warning if any name in includes or excludes is unused.

When configuring Wire you must use the package name from the `.proto` file's `package` declaration.
The `option java_package` name is not used for Wire configuration.


Proto Path for Libraries
------------------------

Large projects may span multiple modules. To support this Wire has a 'proto path'. The `.proto`
schema files on this path are used for linking and validation, but they do not yield files in the
generated output.

The proto path supports the same inputs as the source path: directories, `.jar` files, and Maven
coordinates. Similarly, the proto path may be filtered with `include`.

```groovy
wire {
  protoPath {
    srcDir 'src/main/address-protos'
  }

  protoPath {
    srcJar 'lib/price.jar'
  }

  protoPath {
    srcJar 'com.example:countries:1.0.0'
    include 'com/example/geo/country.proto'
  }

  protoPath {
    srcDir 'src/main/extra-protos'
    exclude 'com/example/**'
  }

  ...
}
```

The source path and proto path are linked together but only types on the source path are generated.

![Library](images/gradle_library@2x.png)

Dependencies between Gradle Modules
-----------------------------------

Wire provides support to define dependencies between modules within the same project.

A module can include its `.proto` files into the output resources. Use this when your `.jar` file
can be used as a library for other proto or Wire projects. Note that only the `.proto` files used
in the library will be included.

```groovy
wire {
  protoLibrary = true
}
```

Wire also creates two configurations, `protoPath` and `protoSource` you can use to define a
dependency on another proto or Wire project.

```groovy
dependencies {
  // The task `:common-protos:jar` will be added into the dependency
  // graph of this module for the Wire generating tasks.
  protoPath(project(':common-protos'))
  implementation(project(':common-protos'))
}

wire {
  kotlin {
  }
}
```

Note that `protoPath` and `protoSource` dependencies are not transitive by default. If needed, you
can change it manually.

```groovy
configurations.protoPath {
  transitive = true
}
```

Pruning
-------

For mobile projects it's easy to generate a lot of unnecessary code. For example, a `.proto` schema
might support types for obsolete features.

General-purpose code-shrinking tools like [R8][r8] and [ProGuard][proguard] have difficulty
shrinking Wire-generated sources because the `encode()` and `decode()` functions still use them.
Instead, Wire offers its own code shrinker to eliminate unwanted declarations early.

Use `prune` to precisely eliminate an unwanted type or member. These will be eliminated along with
all references to them.

```groovy
wire {
  ...

  prune 'com.example.store.Store'
  prune 'com.example.geo.Country'

  ...
}
```

![Pruning](images/gradle_pruning@2x.png)

Alternately you may prefer to specify what to keep, pruning everything not reachable from that set.

```groovy
wire {
  ...

  root 'com.example.pizza.PizzaDelivery'

  ...
}
```

By default this feature is strict given unknown `root` and `prune` arguments. You may prefer for
this to be lenient instead:

```groovy
wire {
  rejectUnusedRootsOrPrunes = false
  ...
}
```

### Version Matching

Another way to prune obsolete fields is to assign them a version, then to generate your code
against a version range or a unique version. The fields out of the version range will get pruned.

Members may be declared with `wire.since` and `wire.until` options; enum constant can use
`wire.constant_since` and `wire.constant_until`. For example, these options declare a field `age`
that was replaced with `birth_date` in version "5.0":

```proto
import "wire/extensions.proto";

message Singer {
  optional string name = 1;
  optional int32 age = 2 [(wire.until) = "5.0"];
  optional Date birth_date = 3 [(wire.since) = "5.0"];
}
```

Client code should typically target a single version. In this example, clients will have the
`name` and `birth_date` fields only.

```groovy
wire {
  onlyVersion "5.0"
}
```

Service code that supports many clients should support the union of versions of all supported
clients. Such code will have `name`, as well as both the `age` and `birth_date` fields.

```groovy
wire {
  sinceVersion "3.0"
  untilVersion "6.0"
}
```

### Proto Libraries

By default, `.proto` input files are not included in the generated `.jar` artifact. Use the
`protoLibrary` option to include them:

```groovy
wire {
  protoLibrary = true
}
```

This is most useful when building `.jar` files for other `wire` tasks to use as dependencies. Note
that only the true sources are included – proto messages that are pruned or not used are not
included in the output artifact.

### Dry run

With the dry run option, the compiler will just emit the names of the source files that would be otherwise generated to
stdout.

```groovy
wire {
  dryRun = true
}
```

Customizing Output
------------------

### Java

Here's an exhaustive Java configuration. Booleans are shown with their default behavior.

```groovy
wire {
  java {
    // Proto types to include generated sources for. Types listed here will be
    // generated for this target and not for subsequent targets in the task.
    //
    // This list should contain package names (suffixed with `.*`) and type
    // names only. It should not contain member names.
    includes = ['com.example.pizza.*']

    // Proto types to excluded generated sources for. Types listed here will
    // not be generated for this target.
    //
    // This list should contain package names (suffixed with `.*`) and type
    // names only. It should not contain member names.
    excludes = ['com.example.sales.*']

    // True if types emitted for this target should not also be emitted for
    // other targets. Use this to cause multiple outputs to be emitted for the
    // same input type.
    exclusive = true

    // Directory to emit to.
    out "${buildDir}/custom"

    // True for emitted types to implement android.os.Parcelable.
    android = false

    // True to enable the androidx.annotation. Nullable annotation
    // where applicable.
    androidAnnotations = false

    // True to emit code that uses reflection for reading, writing, and toString
    // methods which are normally implemented with generated code.
    compact = false

    // True to turn visibility of all generated types' constructors
    // to non-public.
    buildersOnly = false

    // True to emit types for options declared on messages, fields, etc.
    emitDeclaredOptions = true

    // True to emit annotations for options applied on messages, fields, etc.
    emitAppliedOptions = true
  }
}
```

### Kotlin

Here's an exhaustive Kotlin configuration. Booleans and enums are shown with their default behavior.

```groovy
wire {
  kotlin {
    // Proto types to include generated sources for. Types listed here will be
    // generated for this target and not for subsequent targets in the task.
    //
    // This list should contain package names (suffixed with `.*`) and type
    // names only. It should not contain member names.
    includes = ['com.example.pizza.*']

    // Proto types to excluded generated sources for. Types listed here will not
    // be generated for this target.
    //
    // This list should contain package names (suffixed with `.*`) and type
    // names only. It should not contain member names.
    excludes = ['com.example.sales.*']

    // True if types emitted for this target should not also be emitted for
    // other targets. Use this to cause multiple outputs to be emitted for the
    // same input type.
    exclusive = true

    // Directory to emit to.
    out "${buildDir}/custom"

    // True for emitted types to implement android.os.Parcelable.
    android = false

    // True for emitted types to implement APIs for easier migration from the
    // Java target.
    javaInterop = false

    // True to turn visibility of all generated types' constructors
    // to non-public.
    buildersOnly = false

    // True to emit types for options declared on messages, fields, etc.
    emitDeclaredOptions = true

    // True to emit annotations for options applied on messages, fields, etc.
    emitAppliedOptions = true

    // `suspending` to generate coroutines APIs that require a Kotlin
    // coroutines context.
    // `blocking` to generate blocking APIs callable by Java and Kotlin.
    rpcCallStyle = 'blocking'

    // `client` to generate interfaces best suited to sending outbound calls.
    // `server` to generate interfaces best suited to receiving inbound calls.
    // `none` to not generate services.
    rpcRole = 'server'

    // If set, the value will be appended to generated service type names. If
    // null, their rpcRole will be used as a suffix instead.
    nameSuffix = "Suffix"

    // True for emitted services to implement one interface per RPC.
    singleMethodServices = false

    // Set how many oneof choices are necessary for generated message classes to use the
    // `OneOf<Key<T>, T>` form rather than the default, where options are flattened into the
    // enclosing type.
    boxOneOfsMinSize = 5000

    // True to escape Kotlin keywords like `value` and `data` with backticks. Otherwise an
    // underscore underscore is added as a suffix, like `value_` and `data_`.
    escapeKotlinKeywords = false

    // Defines how an protobuf enum type is to be generated. See `com.squareup.wire.kotlin.EnumMode`
    enumMode = "enum_class"

    // True to emit a adapters that include a decode() function that accepts a `ProtoReader32`.
    // Use this optimization when targeting Kotlin/JS, where `Long` cursors are inefficient.
    emitProtoReader32 = false
  }
}
```

### Proto

Here's an exhaustive Proto configuration. You can use this target to generate a pruned proto schema,
or to pretty-print one. Note that even with multiple outputs set, the proto target will always emit
all types, all services, and all extensions of the schema.

```groovy
wire {
  proto {
    // Directory to emit to.
    out "${buildDir}/custom"
  }
}
```

### Custom Handlers

With a custom schema handler, you can handle a proto schema in any way you want, including code
generation or other side effects such as validation, logging, etc.

You'll need to first extend the [SchemaHandler] class, and then the [SchemaHandler.Factory]
interface which is to return an instance of the former. See our [recipes][SchemaHandlerRecipes] for
different use cases' implementations.

Build that into an `jar` artifact and add that as a buildscript dependency to your Gradle project.

```groovy
buildscript {
  dependencies {
    classpath "com.example.my-custom-handler:my-custom-handler:1.0.0"
  }
}
```

Next configure the Wire plugin to call your custom handler. Here's an exhaustive custom
configuration. Booleans and enums are shown with their default behavior.

```groovy
wire {
  custom {
    // The name of a Java class to generate code with. This class must:
    //  * be in the buildscript dependencies for this Gradle project
    //  * be a public class
    //  * have a public no-arguments constructor
    //  * implement the com.squareup.wire.schema.SchemaHandler.Factory interface
    schemaHandlerFactoryClass = "com.example.MyCustomHandlerFactory"

    // These options work the same as the java and kotlin targets above.
    includes = ['com.example.pizza.*']
    excludes = ['com.example.sales.*']
    exclusive = true
    out "${buildDir}/custom"

    // Custom payload which can be passed to the `SchemaHandler.Factory`.
    options = [a: "one", b: "two", c: "three"]
  }
}
```

Swift Support
-------------

The easiest way to get started is to use CocoaPods.

```ruby
# Add the Wire compiler so that it is downloaded and available.
# CocoaPods will download the source and build the compiler directly,
# so you'll need Java installed.
pod 'WireCompiler'

# Add the Wire runtime to do the serializing/deserializing
pod 'Wire'
```

Then run pod install to get the dependencies and build the Wire compiler.

Swift Package Manager is also supported for linking the Wire runtime.

### Build Your Protos

The Wire compiler uses [SwiftPoet][swiftpoet] to generate Swift code.
The resulting objects automatically conform to `Equatable`, `Codable` and `Sendable`.

Assuming you've used CocoaPods to download the Wire compiler, to compile your protos into Swift files:

```sh
java -jar ./Pods/WireCompiler/compiler.jar \
  "--proto_path=<directory containing .proto files>" \
  "--swift_out=<directory where the generated .swift files go>" \
  "--experimental-module-manifest=<path to manifest yaml file>"
```

### Swift Manifest

Swift introduced a new challenge that didn’t exist with Kotlin and Java: modules.
Kotlin and Java both use fully-qualified package names, but Swift modules are defined by their compilation unit, and thus namespaces aren’t declared at the type or file level.
This meant that we needed to build a new packaging system for Swift that could deal with Swift module namespacing and imports.

We decided that the easiest way for a caller to define modules was to make those definitions handled directly by Wire.
A single manifest file defines the modules, their names, their dependencies, and the content roots and prunes mentioned above.

In this example manifest the DarkSide and LightSide modules would depend on and import the CommonProtos module:

```yaml
CommonProtos:
  roots:
    - jedi.Lightsaber
    - jedi.MindTrick
    - jedi.TheForce
    - jedi.Anakin

DarkSideProtos:
  dependencies:
    - CommonProtos
  roots:
    - darkside.*
    - jedi.Lightning
  prunes:
    - jedi.Mercy

LightSideProtos:
  dependencies:
    - CommonProtos
  roots:
    - lightside.*
    # Import the rest of the Jedi powers not already in CommonProtos
    - jedi.*
  prunes:
    # Remove unused lightsaber colors
    - jedi.LightsaberColor#red
    # Remove deprecated field. Use green_lightsaber instead.
    - lightside.Luke#blue_lightsaber
    # Remove dark-side-only types
    - jedi.Lightning
```

 [SchemaHandler.Factory]: https://github.com/square/wire/blob/master/wire-schema/src/commonMain/kotlin/com/squareup/wire/schema/SchemaHandler.kt#L194
 [SchemaHandlerRecipes]: https://github.com/square/wire/tree/master/wire-schema-tests/src/commonTest/kotlin/com/squareup/wire/recipes
 [SchemaHandler]: https://github.com/square/wire/blob/master/wire-schema/src/commonMain/kotlin/com/squareup/wire/schema/SchemaHandler.kt

 [gradle]: https://gradle.org/
 [kotlinpoet]: https://github.com/square/kotlinpoet
 [swiftpoet]: https://github.com/outfoxx/swiftpoet
 [maven_coordinates]: https://maven.apache.org/pom.html#Maven_Coordinates
 [proguard]: https://www.guardsquare.com/en/products/proguard
 [r8]: https://developer.android.com/studio/build/shrink-code
