/*
 * Copyright (C) 2015 Square, Inc.
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

internal class StringWireLogger : WireLogger {
  var quiet: Boolean = false
  private val buffer = StringBuilder()

  val log: String get() = buffer.toString()

  @Synchronized override fun artifactHandled(
    outputPath: Path,
    qualifiedName: String,
    targetName: String,
  ) {
    buffer.append("$outputPath $qualifiedName (target=$targetName)\n")
  }

  override fun artifactSkipped(type: ProtoType, targetName: String) {
    buffer.append("Skipped $type (target=$targetName)\n")
  }

  @Synchronized override fun unusedRoots(unusedRoots: Set<String>) {
    if (quiet) return

    buffer.append(
      """Unused element in treeShakingRoots:
      |  ${unusedRoots.joinToString(separator = "\n  ")}
      """.trimMargin(),
    )
  }

  @Synchronized override fun unusedPrunes(unusedPrunes: Set<String>) {
    if (quiet) return

    buffer.append(
      """Unused element in treeShakingRubbish:
      |  ${unusedPrunes.joinToString(separator = "\n  ")}
      """.trimMargin(),
    )
  }

  @Synchronized override fun unusedIncludesInTarget(unusedIncludes: Set<String>) {
    if (quiet) return

    buffer.append(
      """Unused includes in targets:
      |  ${unusedIncludes.joinToString(separator = "\n  ")}
      """.trimMargin(),
    )
  }

  @Synchronized override fun unusedExcludesInTarget(unusedExcludes: Set<String>) {
    if (quiet) return

    buffer.append(
      """Unused excludes in targets:
      |  ${unusedExcludes.joinToString(separator = "\n  ")}
      """.trimMargin(),
    )
  }
}
