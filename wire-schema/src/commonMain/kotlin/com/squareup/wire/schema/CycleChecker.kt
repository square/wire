/*
 * Copyright (C) 2020 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.squareup.wire.schema.internal.DagChecker

/**
 * Check for forbidden cycles in proto schemas.
 *
 * Wire doesn't care, but protoc and some of the languages it targets care, and it's good if the
 * schemas created with Wire also work on those tools.
 *
 * This class doesn't do any graph traversal; that's delegated to [DagChecker]. Instead this class
 * is concerned with turning our schemas into graphs that [DagChecker] can understand, and
 * formatting errors if it finds problems.
 */
internal class CycleChecker(
  private val fileLinkers: Map<String, FileLinker>,
  private val errors: ErrorCollector,
) {
  private val goPackageOption = ProtoMember.get("google.protobuf.FileOptions#go_package")

  private val FileLinker.importsAndPublicImports: List<String>
    get() = protoFile.imports + protoFile.publicImports

  private val FileLinker.cycleCheckPackageName: String
    get() {
      val goPackage = protoFile.options.get(goPackageOption) as? String
      if (goPackage != null) return goPackage
      return protoFile.packageName ?: "<default>"
    }

  fun checkForImportCycles() {
    val dagChecker = DagChecker(fileLinkers.keys) {
      val fileLinker = fileLinkers[it] ?: return@DagChecker listOf<String>()
      fileLinker.protoFile.imports + fileLinker.protoFile.publicImports
    }

    val cycles = dagChecker.check()

    for (cycle in cycles) {
      errors += importCycleMessageError(cycle)
    }
  }

  /** Returns an error message that describes cyclic imports in [files]. */
  private fun importCycleMessageError(files: List<String>): String {
    return buildString {
      append("imports form a cycle:")

      for (file in files) {
        val fileLinker = fileLinkers[file] ?: continue

        append("\n  $file:")
        for (import in fileLinker.protoFile.imports) {
          if (import in files) {
            append("\n    import \"$import\";")
          }
        }
        for (import in fileLinker.protoFile.publicImports) {
          if (import in files) {
            append("\n    import public \"$import\";")
          }
        }
      }
    }
  }

  /**
   * In good programming languages like Java and Kotlin it's completely fine to have dependency
   * cycles between packages. But Go forbids cycles between packages and that effectively means
   * that proto must as well when it targets Go.
   *
   * This checks for cycles between packages that would offend the Go compiler if these .proto
   * sources were ever generated with it.
   */
  fun checkForPackageCycles() {
    val packageDag = mutableMapOf<String, MutableSet<String>>()
    for (fileLinker in fileLinkers.values) {
      val targets = packageDag.getOrPut(fileLinker.cycleCheckPackageName) { mutableSetOf() }
      for (path in fileLinker.importsAndPublicImports) {
        val imported = fileLinkers[path] ?: continue
        targets += imported.cycleCheckPackageName
      }
      targets.remove(fileLinker.cycleCheckPackageName)
    }

    val dagChecker = DagChecker(packageDag.keys) {
      packageDag[it]!!
    }

    val cycles = dagChecker.check()

    for (cycle in cycles) {
      errors += packagesCycleMessageError(cycle)
    }
  }

  /**
   * Returns an error message that describes cyclic imports in [packages]. The error attempts to
   * show which imports are responsible for the package cycles and looks like this:
   *
   * ```
   * packages form a cycle:
   *   locations imports people
   *     locations/office.proto:
   *       import "people/office_manager.proto";
   *   people imports locations
   *     people/employee.proto:
   *       import "locations/office.proto";
   *       import "locations/residence.proto";
   * ```
   */
  private fun packagesCycleMessageError(packages: List<String>): String {
    return buildString {
      append("packages form a cycle:")

      val sortedFileLinkers = fileLinkers.entries.sortedBy { it.value.cycleCheckPackageName }

      var lastSourcePackage: String? = null
      var lastTargetPackage: String? = null
      var lastSourcePath: String? = null

      for ((sourcePath, sourceFileLinker) in sortedFileLinkers) {
        val sourcePackage = sourceFileLinker.cycleCheckPackageName
        if (sourcePackage !in packages) continue

        for (targetPath in sourceFileLinker.importsAndPublicImports) {
          val targetFileLinker = fileLinkers[targetPath] ?: continue
          val targetPackage = targetFileLinker.cycleCheckPackageName
          if (targetPackage == sourcePackage || targetPackage !in packages) continue

          if (lastSourcePackage != sourcePackage || lastTargetPackage != targetPackage) {
            append("\n  $sourcePackage imports $targetPackage")
            lastSourcePackage = sourcePackage
            lastTargetPackage = targetPackage
            lastSourcePath = null
          }

          if (lastSourcePath != sourcePath) {
            append("\n    $sourcePath:")
            lastSourcePath = sourcePath
          }

          append("\n      import \"$targetPath\";")
        }
      }
    }
  }
}
