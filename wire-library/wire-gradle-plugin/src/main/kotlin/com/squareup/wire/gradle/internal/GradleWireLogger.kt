/*
 * Copyright 2021 Square Inc.
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
package com.squareup.wire.gradle.internal

import com.squareup.wire.WireLogger
import com.squareup.wire.gradle.WirePlugin
import com.squareup.wire.schema.ProtoType
import okio.Path
import org.slf4j.LoggerFactory

internal object GradleWireLogger : WireLogger {
  private val slf4jLogger = LoggerFactory.getLogger(WirePlugin::class.java)

  override fun setQuiet(quiet: Boolean) {
  }

  override fun warn(message: String) {
    slf4jLogger.warn(message)
  }

  override fun artifactHandled(outputPath: Path, qualifiedName: String) {
    slf4jLogger.info("Writing $qualifiedName to $outputPath")
  }

  override fun artifactSkipped(type: ProtoType) {
    slf4jLogger.info("Skipping $type")
  }
}
