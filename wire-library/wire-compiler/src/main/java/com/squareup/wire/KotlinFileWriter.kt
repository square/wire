/*
 * Copyright 2018 Square Inc.
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

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.kotlin.KotlinGenerator
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import java.io.IOException
import java.nio.file.FileSystem
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue

internal class KotlinFileWriter(
  private val destination: String,
  private val kotlinGenerator: KotlinGenerator,
  private val queue: ConcurrentLinkedQueue<PendingFileSpec>,
  private val fs: FileSystem,
  private val log: WireLogger,
  private val dryRun: Boolean
) : Callable<Unit> {

  @Throws(IOException::class)
  override fun call() {
    while (true) {
      val pendingFile = queue.poll() ?: return
      val kotlinFiles = when (pendingFile) {
        is PendingTypeFileSpec -> listOf(generateFileForType(pendingFile.type))
        is PendingServiceFileSpec -> generateFilesForService(pendingFile.service)
      }

      val path = fs.getPath(destination)
      kotlinFiles.forEach { file -> log.artifact(path, file) }
      if (dryRun) return

      kotlinFiles.forEach { file ->
        try {
          file.writeTo(path)
        } catch (e: IOException) {
          val className = when (pendingFile) {
            is PendingTypeFileSpec ->
              kotlinGenerator.generatedTypeName(pendingFile.type).canonicalName
            is PendingServiceFileSpec -> pendingFile.service.type().toString()
          }
          throw IOException("Error emitting ${file.packageName}.$className to $destination", e)
        }
      }
    }
  }

  private fun generateFileForType(type: Type): FileSpec {
    return generateFile(
        packageName = kotlinGenerator.generatedTypeName(type).packageName,
        typeSpec = kotlinGenerator.generateType(type),
        location = type.location
    )
  }

  private fun generateFilesForService(service: Service): List<FileSpec> {
    val files = mutableListOf<FileSpec>()
    val packageName = kotlinGenerator.generatedServiceName(service).packageName
    for (typeSpec in kotlinGenerator.generateServiceTypeSpecs(service)) {
      files.add(generateFile(
          packageName = packageName,
          typeSpec = typeSpec,
          location = service.location()
      ))
    }
    return files
  }

  private fun generateFile(packageName: String, typeSpec: TypeSpec, location: Location?): FileSpec {
    return FileSpec.builder(packageName, typeSpec.name!!)
        .addComment(WireCompiler.CODE_GENERATED_BY_WIRE)
        .indent("  ")
        .apply {
          if (location != null) {
            addComment("\nSource file: %L", location.withPathOnly())
          }
        }
        .addType(typeSpec)
        .build()
  }
}
