/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire

import com.squareup.wire.schema.CoreLoader.WIRE_RUNTIME_JAR
import com.squareup.wire.schema.CoreLoader.isWireRuntimeProto
import com.squareup.wire.schema.JavaTarget
import com.squareup.wire.schema.KotlinTarget
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.NullTarget
import com.squareup.wire.schema.SwiftTarget
import com.squareup.wire.schema.Target
import com.squareup.wire.schema.WireRun
import com.squareup.wire.schema.toOkioFileSystem
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip
import java.io.IOException
import java.nio.file.FileSystem as NioFileSystem

/**
 * Command line interface to the Wire Java generator.
 *
 * Usage
 * -----
 *
 * ```
 * java WireCompiler --proto_path=<path>
 *   [--java_out=<path>]
 *   [--kotlin_out=<path>]
 *   [--swift_out=<path>]
 *   [--files=<protos.include>]
 *   [--includes=<message_name>[,<message_name>...]]
 *   [--excludes=<message_name>[,<message_name>...]]
 *   [--quiet]
 *   [--dry_run]
 *   [--android]
 *   [--android-annotations]
 *   [--compact]
 *   [file [file...]]
 * ```
 *
 * `--java_out` should provide the folder where the files generated by the Java code generator
 * should be placed. Similarly, `--kotlin_out` should provide the folder where the files generated
 * by the Kotlin code generator will be written. Only one of the two should be specified.
 *
 * `--swift_out` should provide the folder where the files generated by the Swift code generator
 * should be placed.
 *
 * If the `--includes` flag is present, its argument must be a comma-separated list of
 * fully-qualified message or enum names. The output will be limited to those messages and enums
 * that are (transitive) dependencies of the listed names. The `--excludes` flag excludes types, and
 * takes precedence over `--includes`.
 *
 * If the `--registry_class` flag is present, its argument must be a Java class name. A class with
 * the given name will be generated, containing a constant list of all extension classes generated
 * during the compile. This list is suitable for passing to Wire's constructor at runtime for
 * constructing its internal extension registry.
 *
 * If `--quiet` is specified, diagnostic messages to stdout are suppressed.
 *
 * The `--dry_run` flag causes the compile to just emit the names of the source files that would be
 * generated to stdout.
 *
 * The `--android` flag will cause all messages to implement the `Parcelable`
 * interface. This implies `--android-annotations` as well.
 *
 * The `--android-annotations` flag will add the `Nullable` annotation to optional fields.
 *
 * The `--compact` flag will emit code that uses reflection for reading, writing, and
 * toString methods which are normally implemented with code generation.
 */
class WireCompiler internal constructor(
  val fs: FileSystem,
  val log: WireLogger,
  val protoPaths: List<String>,
  val javaOut: String?,
  val kotlinOut: String?,
  val swiftOut: String?,
  val sourceFileNames: List<String>,
  val treeShakingRoots: List<String>,
  val treeShakingRubbish: List<String>,
  val modules: Map<String, WireRun.Module>,
  val dryRun: Boolean,
  val emitAndroid: Boolean,
  val emitAndroidAnnotations: Boolean,
  val emitCompact: Boolean,
  val emitDeclaredOptions: Boolean,
  val emitAppliedOptions: Boolean,
  val permitPackageCycles: Boolean,
  val javaInterop: Boolean,
  val kotlinBoxOneOfsMinSize: Int,
) {

  @Throws(IOException::class)
  fun compile() {
    val targets = mutableListOf<Target>()
    if (dryRun) {
      targets += NullTarget()
    } else if (javaOut != null) {
      targets += JavaTarget(
          outDirectory = javaOut,
          android = emitAndroid,
          androidAnnotations = emitAndroidAnnotations,
          compact = emitCompact,
          emitDeclaredOptions = emitDeclaredOptions,
          emitAppliedOptions = emitAppliedOptions
      )
    } else if (kotlinOut != null) {
      targets += KotlinTarget(
          outDirectory = kotlinOut,
          android = emitAndroid,
          javaInterop = javaInterop,
          emitDeclaredOptions = emitDeclaredOptions,
          emitAppliedOptions = emitAppliedOptions,
          boxOneOfsMinSize = kotlinBoxOneOfsMinSize,
      )
    } else if (swiftOut != null) {
      targets += SwiftTarget(
          outDirectory = swiftOut
      )
    }

    val sources = protoPaths.map { it.toPath() }

    val allDirectories = sources.map { Location.get(it.toString()) }.toList()
    val sourcePath: List<Location>
    val protoPath: List<Location>

    if (sourceFileNames.isNotEmpty()) {
      sourcePath = sourceFileNames.map { locationOfProto(sources, it) }
      protoPath = allDirectories
    } else {
      sourcePath = allDirectories
      protoPath = listOf()
    }

    val wireRun = WireRun(
        sourcePath = sourcePath,
        protoPath = protoPath,
        treeShakingRoots = treeShakingRoots,
        treeShakingRubbish = treeShakingRubbish,
        targets = targets,
        modules = modules,
        permitPackageCycles = permitPackageCycles
    )

    wireRun.execute(fs, log)
  }

  /** Searches [sources] trying to resolve [proto]. Returns the location if it is found. */
  private fun locationOfProto(sources: List<Path>, proto: String): Location {
    // We cache ZIP openings because they are expensive.
    val sourceToZipFileSystem = mutableMapOf<Path, FileSystem>()
    for (source in sources) {
      if (fs.metadataOrNull(source)?.isRegularFile == true) {
        sourceToZipFileSystem[source] = fs.openZip(source)
      }
    }

    val directoryEntry = sources.find { source ->
      when (val zip = sourceToZipFileSystem[source]) {
        null -> fs.exists(source / proto)
        else -> zip.exists("/".toPath() / proto)
      }
    }

    if (directoryEntry == null) {
      if (isWireRuntimeProto(proto)) return Location.get(WIRE_RUNTIME_JAR, proto)
      throw FileNotFoundException("Failed to locate $proto in $sources")
    }

    return Location.get(directoryEntry.toString(), proto)
  }

  companion object {
    const val CODE_GENERATED_BY_WIRE =
        "Code generated by Wire protocol buffer compiler, do not edit."

    private const val PROTO_PATH_FLAG = "--proto_path="
    private const val JAVA_OUT_FLAG = "--java_out="
    private const val KOTLIN_OUT_FLAG = "--kotlin_out="
    private const val SWIFT_OUT_FLAG = "--swift_out="
    private const val FILES_FLAG = "--files="
    private const val INCLUDES_FLAG = "--includes="
    private const val EXCLUDES_FLAG = "--excludes="
    private const val MANIFEST_FLAG = "--experimental-module-manifest="
    private const val DRY_RUN_FLAG = "--dry_run"
    private const val ANDROID = "--android"
    private const val ANDROID_ANNOTATIONS = "--android-annotations"
    private const val COMPACT = "--compact"
    private const val SKIP_DECLARED_OPTIONS = "--skip_declared_options"
    private const val EMIT_APPLIED_OPTIONS = "--emit_applied_options"
    private const val PERMIT_PACKAGE_CYCLES_OPTIONS = "--permit_package_cycles"
    private const val JAVA_INTEROP = "--java_interop"
    private const val KOTLIN_BOX_ONEOFS_MIN_SIZE = "--kotlin_box_oneofs_min_size="

    @Throws(IOException::class)
    @JvmStatic fun main(args: Array<String>) {
      try {
        val wireCompiler = forArgs(args = args)
        wireCompiler.compile()
      } catch (e: WireException) {
        System.err.print("Fatal: ")
        e.printStackTrace(System.err)
        System.exit(1)
      }
    }

    @Throws(WireException::class)
    @JvmStatic
    fun forArgs(
      fileSystem: NioFileSystem,
      logger: WireLogger,
      vararg args: String
    ): WireCompiler {
      return forArgs(fileSystem.toOkioFileSystem(), logger, *args)
    }

    @Throws(WireException::class)
    @JvmOverloads
    @JvmStatic
    fun forArgs(
      fileSystem: FileSystem = FileSystem.SYSTEM,
      logger: WireLogger = ConsoleWireLogger(),
      vararg args: String
    ): WireCompiler {
      val sourceFileNames = mutableListOf<String>()
      val treeShakingRoots = mutableListOf<String>()
      val treeShakingRubbish = mutableListOf<String>()
      val protoPaths = mutableListOf<String>()
      var modules = mapOf<String, WireRun.Module>()
      var javaOut: String? = null
      var kotlinOut: String? = null
      var swiftOut: String? = null
      var dryRun = false
      var emitAndroid = false
      var emitAndroidAnnotations = false
      var emitCompact = false
      var emitDeclaredOptions = true
      var emitAppliedOptions = false
      var permitPackageCycles = false
      var javaInterop = false
      var kotlinBoxOneOfsMinSize = 5_000

      for (arg in args) {
        when {
          arg.startsWith(PROTO_PATH_FLAG) -> {
            protoPaths.add(arg.substring(PROTO_PATH_FLAG.length))
          }

          arg.startsWith(JAVA_OUT_FLAG) -> {
            check(javaOut == null) { "java_out already set" }
            javaOut = arg.substring(JAVA_OUT_FLAG.length)
          }

          arg.startsWith(KOTLIN_OUT_FLAG) -> {
            check(kotlinOut == null) { "kotlin_out already set" }
            kotlinOut = arg.substring(KOTLIN_OUT_FLAG.length)
          }

          arg.startsWith(KOTLIN_BOX_ONEOFS_MIN_SIZE) -> {
            kotlinBoxOneOfsMinSize = arg.substring(KOTLIN_BOX_ONEOFS_MIN_SIZE.length).toInt()
          }

          arg.startsWith(SWIFT_OUT_FLAG) -> {
            swiftOut = arg.substring(SWIFT_OUT_FLAG.length)
          }

          arg.startsWith(FILES_FLAG) -> {
            val files = arg.substring(FILES_FLAG.length).toPath()
            try {
              fileSystem.read(files) {
                while (true) {
                  val line = readUtf8Line() ?: break
                  sourceFileNames.add(line)
                }
              }
            } catch (ex: FileNotFoundException) {
              throw WireException("Error processing argument $arg", ex)
            }
          }

          arg.startsWith(INCLUDES_FLAG) -> {
            treeShakingRoots += arg.substring(INCLUDES_FLAG.length).split(Regex(","))
          }

          arg.startsWith(EXCLUDES_FLAG) -> {
            treeShakingRubbish += arg.substring(EXCLUDES_FLAG.length).split(Regex(","))
          }

          arg.startsWith(MANIFEST_FLAG) -> {
            val yaml = fileSystem.read(arg.substring(MANIFEST_FLAG.length).toPath()) { readUtf8() }
            modules = parseManifestModules(yaml)
          }

          arg == DRY_RUN_FLAG -> dryRun = true
          arg == ANDROID -> emitAndroid = true
          arg == ANDROID_ANNOTATIONS -> emitAndroidAnnotations = true
          arg == COMPACT -> emitCompact = true
          arg == SKIP_DECLARED_OPTIONS -> emitDeclaredOptions = false
          arg == EMIT_APPLIED_OPTIONS -> emitAppliedOptions = true
          arg == PERMIT_PACKAGE_CYCLES_OPTIONS -> permitPackageCycles = true
          arg == JAVA_INTEROP -> javaInterop = true
          arg.startsWith("--") -> throw IllegalArgumentException("Unknown argument '$arg'.")
          else -> sourceFileNames.add(arg)
        }
      }

      if (javaOut == null && kotlinOut == null && swiftOut == null) {
        throw WireException(
            "Nothing to do! Specify $JAVA_OUT_FLAG, $KOTLIN_OUT_FLAG, or $SWIFT_OUT_FLAG")
      }

      if (treeShakingRoots.isEmpty()) {
        treeShakingRoots += "*"
      }

      return WireCompiler(
          fs = fileSystem,
          log = logger,
          protoPaths = protoPaths,
          javaOut = javaOut,
          kotlinOut = kotlinOut,
          swiftOut = swiftOut,
          sourceFileNames = sourceFileNames,
          treeShakingRoots = treeShakingRoots,
          treeShakingRubbish = treeShakingRubbish,
          modules = modules,
          dryRun = dryRun,
          emitAndroid = emitAndroid,
          emitAndroidAnnotations = emitAndroidAnnotations,
          emitCompact = emitCompact,
          emitDeclaredOptions = emitDeclaredOptions,
          emitAppliedOptions = emitAppliedOptions,
          permitPackageCycles = permitPackageCycles,
          javaInterop = javaInterop,
          kotlinBoxOneOfsMinSize = kotlinBoxOneOfsMinSize
      )
    }
  }
}
