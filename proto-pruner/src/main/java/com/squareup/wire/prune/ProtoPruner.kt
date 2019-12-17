/*
 * Copyright 2017 Square Inc.
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
package com.squareup.wire.prune

import com.squareup.wire.schema.PruningRules
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.NewSchemaLoader
import com.squareup.wire.schema.ProtoFile
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.FileSystems

class ProtoPruner(
  private val fs: FileSystem,
  private val sourcePath: List<Location>,
  private val protoPath: List<Location>,
  private val outPath: String,
  private val pruningRules: PruningRules
) : Runnable {
  override fun run() {
    NewSchemaLoader(fs).use { loader ->
      loader.initRoots(sourcePath, protoPath)

      val schema = loader.loadSchema()
          .prune(pruningRules)

      val sourcePaths = loader.sourcePathFiles.map { it.location.path }.toSet()

      schema.getProtoFiles()
          .filter {
            (it.location.path in sourcePaths) && it.isNotEmpty()
          }
          .forEach { protoFile ->
            val relativePath = protoFile.location.path
                .substringBeforeLast("/", missingDelimiterValue = ".")
            val outFolder = File(outPath, relativePath)
            outFolder.mkdirs() // Ensure the directories to the file have been created.

            val outFile = File(outFolder, "${protoFile.name()}.proto")
            println("Writing $outFile...")
            outFile.writeText(protoFile.toSchema())
          }
    }
  }

  private fun ProtoFile.isNotEmpty() =
      types.isNotEmpty() || services.isNotEmpty() || extendList.isNotEmpty()

  companion object {
    @JvmStatic fun main(vararg args: String) {
      var outPath: String? = null
      val inLocations = mutableListOf<Location>()
      val identifierSetBuilder = PruningRules.Builder()

      for (arg in args) {
        if (arg == "--help") {
          println("""
              |Arguments: --in=dir --out=dir [--includes=pattern] [--excludes=pattern] [file]
              |
              |Patterns must be in one of the following formats:
              |  * Partial package name followed by a `*` (e.g., `com.example.*`).
              |  * Full package name of a type or service.
              |  * Full package name, a `#`, and the name of a member.
              |
              |Multiple patterns can be supplied to a single argument by separating them by
              |a `,` (e.g., `--include=com.example.*,com.other.package.*`).
              |
              |File must contain patterns prefixed with either `+` or `-` to signify an include
              |or exclude, respectively. All other lines are ignored.
              |""".trimMargin())
          return // Don't run the program.
        }
        if (arg.startsWith("--out=")) {
          outPath = arg.substringAfter('=')
        } else if (arg.startsWith("--in=")) {
          inLocations.add(Location.get(arg.substringAfter('=')))
        } else if (arg.startsWith("--includes=")) {
          identifierSetBuilder.include(arg.substringAfter('=').split(',').map(String::trim))
        } else if (arg.startsWith("--excludes=")) {
          identifierSetBuilder.exclude(arg.substringAfter('=').split(',').map(String::trim))
        } else if (arg.startsWith("--")) {
          throw IllegalArgumentException("Unknown argument: $arg")
        } else {
          File(arg).forEachLine { line ->
            if (line.startsWith("+")) {
              identifierSetBuilder.include(line.substring(1))
            } else if (line.startsWith("-")) {
              identifierSetBuilder.exclude(line.substring(1))
            }
          }
        }
      }

      if (inLocations.isEmpty()) {
        throw IllegalArgumentException("No input path specified")
      }
      if (outPath == null) {
        throw IllegalArgumentException("No output path specified")
      }

      ProtoPruner(
          fs = FileSystems.getDefault(),
          sourcePath = inLocations,
          protoPath = listOf(),
          outPath = outPath,
          pruningRules = identifierSetBuilder.build()
      ).run()
    }
  }
}
