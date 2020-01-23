/*
 * Copyright 2015 Square Inc.
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

import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.schema.ProtoType
import java.nio.file.Path

internal class StringWireLogger : WireLogger {
  private var quiet: Boolean = false
  private val buffer = StringBuilder()

  val log: String get() = buffer.toString()

  override fun setQuiet(quiet: Boolean) {
    this.quiet = quiet
  }

  @Synchronized override fun artifact(outputPath: Path, filePath: String) {
    buffer.append("$outputPath $filePath\n")
  }

  @Synchronized override fun artifact(outputPath: Path, javaFile: JavaFile) {
    buffer.append("$outputPath ${javaFile.packageName}.${javaFile.typeSpec.name}\n")
  }

  @Synchronized override fun artifact(outputPath: Path, kotlinFile: FileSpec) {
    val typeSpec = kotlinFile.members.single() as TypeSpec
    buffer.append("$outputPath ${kotlinFile.packageName}.${typeSpec.name}\n")
  }

  override fun artifactSkipped(type: ProtoType) {
    buffer.append("skipped $type\n")
  }

  @Synchronized override fun info(message: String) {
    if (!quiet) {
      buffer.append("$message\n")
    }
  }
}
