/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire

import com.squareup.wire.schema.ProtoType
import okio.Path

internal class ConsoleWireLogger : WireLogger {
  var quiet: Boolean = false

  override fun unusedRoots(unusedRoots: Set<String>) {
    if (quiet) return

    println(
      """Unused element in treeShakingRoots:
      |  ${unusedRoots.joinToString(separator = "\n  ")}
      """.trimMargin(),
    )
  }

  override fun unusedPrunes(unusedPrunes: Set<String>) {
    if (quiet) return

    println(
      """Unused element in treeShakingRubbish:
      |  ${unusedPrunes.joinToString(separator = "\n  ")}
      """.trimMargin(),
    )
  }

  override fun unusedIncludesInTarget(unusedIncludes: Set<String>) {
    if (quiet) return

    println(
      """Unused includes in targets:
      |  ${unusedIncludes.joinToString(separator = "\n  ")}
      """.trimMargin(),
    )
  }

  override fun unusedExcludesInTarget(unusedExcludes: Set<String>) {
    if (quiet) return

    println(
      """Unused excludes in targets:
      |  ${unusedExcludes.joinToString(separator = "\n  ")}
      """.trimMargin(),
    )
  }

  override fun artifactHandled(outputPath: Path, qualifiedName: String, targetName: String) {
    if (quiet) return

    println("Writing $qualifiedName to $outputPath (target=$targetName)")
  }

  override fun artifactSkipped(type: ProtoType, targetName: String) {
    if (quiet) return

    println("Skipping $type (target=$targetName)")
  }
}
