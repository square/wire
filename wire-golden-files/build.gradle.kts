plugins {
  id("java-library")
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  java {
    includes = listOf("squareup.wire.alltypes.proto3.*")
    out = "src/main/java"
  }

  kotlin {
    includes = listOf("squareup.wire.mutable.*")
    out = "src/main/kotlin"
    mutableTypes = true
  }

  kotlin {
    includes = listOf("squareup.wire.unrecognized_constant.*")
    out = "src/main/kotlin"
    buildersOnly = true
    enumMode = "sealed_class"
  }

  kotlin {
    includes = listOf(
      "squareup.wire.buildersonly.*",
      "squareup.wire.alltypes.*",
    )
    out = "src/main/kotlin"
    buildersOnly = true
  }

  kotlin {
    includes = listOf("squareup.wire.boxedoneof.*")
    out = "src/main/kotlin"
    javaInterop = true
    boxOneOfsMinSize = 1
  }

  kotlin {
    includes = listOf("squareup.wire.sealedoneof.*")
    out = "src/main/kotlin"
    oneofMode = "sealed_class"
    buildersOnly = true
  }

  opaque("squareup.protos.opaque_types.OuterOpaqueType.InnerOpaqueType1")
  kotlin {
    includes = listOf("squareup.protos.opaque_types.*")
    out = "src/main/kotlin"
  }

  kotlin {
    includes = listOf("squareup.wire.no_immutable_copies.*")
    out = "src/main/kotlin"
    makeImmutableCopies = false
  }

  kotlin {
    out = "src/main/kotlin"
  }
}

// The generated code must stay clean under the Kotlin compiler's extra checks.
// See https://github.com/square/wire/issues/3700 and https://github.com/square/wire/issues/3701.
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
  compilerOptions {
    extraWarnings.set(true)
    // KotlinPoet emits explicit visibility modifiers on purpose. Consumers which enable
    // explicit API mode need them, so this check does not apply to generated code.
    freeCompilerArgs.add("-Xwarning-level=REDUNDANT_VISIBILITY_MODIFIER:disabled")
    // Fail the build when these checks flag generated code again. Plain allWarningsAsErrors
    // is too broad here: it also fails on repo-wide compiler flag deprecation warnings.
    freeCompilerArgs.add("-Xwarning-level=VARIABLE_INITIALIZER_IS_REDUNDANT:error")
    freeCompilerArgs.add("-Xwarning-level=CAN_BE_VAL_DELAYED_INITIALIZATION:error")
    freeCompilerArgs.add("-Xwarning-level=CAN_BE_VAL:error")
    freeCompilerArgs.add("-Xwarning-level=CAN_BE_VAL_LATEINIT:error")
  }
}

tasks.getByName("spotlessJava").dependsOn("generateMainProtos")
tasks.getByName("spotlessKotlin").dependsOn("generateMainProtos")
tasks.getByName("spotlessSwift").dependsOn("generateMainProtos")
