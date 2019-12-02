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
import com.squareup.wire.java.JavaGenerator
import java.io.IOException
import java.nio.file.FileSystem
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue

internal class JavaFileWriter(
  private val destination: String,
  private val javaGenerator: JavaGenerator,
  private val queue: ConcurrentLinkedQueue<PendingTypeFileSpec>,
  private val dryRun: Boolean,
  private val fs: FileSystem,
  private val log: WireLogger
) : Callable<Unit> {

  @Throws(IOException::class)
  override fun call() {
    while (true) {
      val type = queue.poll()?.type ?: return

      val typeSpec = javaGenerator.generateType(type)
      val javaTypeName = javaGenerator.generatedTypeName(type)
      val javaFile = JavaFile.builder(javaTypeName.packageName(), typeSpec)
          .addFileComment("\$L", WireCompiler.CODE_GENERATED_BY_WIRE)
          .apply {
            addFileComment("\nSource file: \$L", type.location.withPathOnly())
          }.build()

      val path = fs.getPath(destination)
      log.artifact(path, javaFile)
      if (dryRun) return

      try {
        javaFile.writeTo(path)
      } catch (e: IOException) {
        throw IOException(
            "Error emitting ${javaFile.packageName}.${javaFile.typeSpec.name} to $destination", e)
      }
    }
  }
}
