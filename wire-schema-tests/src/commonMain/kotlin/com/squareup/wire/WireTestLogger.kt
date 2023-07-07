/*
 * Copyright (C) 2022 Square, Inc.
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

class WireTestLogger : WireLogger {

  val artifactHandled = ArrayDeque<Triple<Path, String, String>>()
  override fun artifactHandled(outputPath: Path, qualifiedName: String, targetName: String) {
    this.artifactHandled.add(Triple(outputPath, qualifiedName, targetName))
  }

  val artifactSkipped = ArrayDeque<Pair<ProtoType, String>>()
  override fun artifactSkipped(type: ProtoType, targetName: String) {
    this.artifactSkipped.add(type to targetName)
  }

  val unusedRoots = ArrayDeque<Set<String>>()
  override fun unusedRoots(unusedRoots: Set<String>) {
    this.unusedRoots.add(unusedRoots)
  }

  val unusedPrunes = ArrayDeque<Set<String>>()
  override fun unusedPrunes(unusedPrunes: Set<String>) {
    this.unusedPrunes.add(unusedPrunes)
  }

  val unusedIncludesInTarget = ArrayDeque<Set<String>>()
  override fun unusedIncludesInTarget(unusedIncludes: Set<String>) {
    this.unusedIncludesInTarget.add(unusedIncludes)
  }

  val unusedExcludesInTarget = ArrayDeque<Set<String>>()
  override fun unusedExcludesInTarget(unusedExcludes: Set<String>) {
    this.unusedExcludesInTarget.add(unusedExcludes)
  }
}
