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

import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.schema.ProtoType
import java.nio.file.Path

internal class ConsoleWireLogger : WireLogger {
  private var quiet: Boolean = false

  override fun setQuiet(quiet: Boolean) {
    this.quiet = quiet
  }

  override fun info(message: String) {
    if (!quiet) {
      println(message)
    }
  }

  override fun artifact(outputPath: Path, filePath: String) {
    if (quiet) {
      println(filePath)
    } else {
      println("Writing $filePath to $outputPath")
    }
  }

  override fun artifact(outputPath: Path, javaFile: JavaFile) {
    if (quiet) {
      println("${javaFile.packageName}.${javaFile.typeSpec.name}")
    } else {
      println("Writing ${javaFile.packageName}.${javaFile.typeSpec.name} to $outputPath")
    }
  }

  override fun artifact(outputPath: Path, kotlinFile: FileSpec) {
    val typeSpec = kotlinFile.members.first() as TypeSpec
    if (quiet) {
      println("${kotlinFile.packageName}.${typeSpec.name}")
    } else {
      println("Writing ${kotlinFile.packageName}.${typeSpec.name} to $outputPath")
    }
  }

  override fun artifactSkipped(type: ProtoType) {
    println("Skipping $type")
  }
}
