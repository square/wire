/*
 * Copyright (C) 2022 Square, Inc.
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
package com.squareup.wire.kotlin.grpcserver

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.withIndent
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.internal.SchemaEncoder

object FileDescriptorGenerator {
  /**
   * Adds properties to the given TypeSpec for storing com.google.protobuf.DescriptorProtos.FileDescriptorProto instances,
   * and helper functions for getting full FileDescriptors with dependencies for any included schema file.
   */
  fun addDescriptorDataProperty(builder: TypeSpec.Builder, protoFile: ProtoFile?, schema: Schema) {
    addDescriptorForFunction(builder)
    addFileDescriptorFunction(builder)

    if (protoFile != null) {
      val encoded = encodeFileAndDependencies(protoFile, schema)
      addDescriptorMapProperty(builder, encoded)
    }
  }

  /**
   * Encodes the given file from the schema as a string that can be embedded in a class.
   * Also encodes all transitive dependencies of the file.
   */
  private fun encodeFileAndDependencies(protoFile: ProtoFile, schema: Schema): MutableMap<String, String> {
    val encoded = mutableMapOf<String, String>()
    val visited = mutableSetOf<String>()
    val todo = mutableListOf(protoFile.location.path)

    while (todo.isNotEmpty()) {
      val path = todo.removeLast()

      if (!visited.contains(path)) {
        visited.add(path)
        val file = schema.protoFile(path)
        if (file != null) {
          todo.addAll(file.imports)
          encoded[file.location.path] = SchemaEncoder(schema).encode(file).base64()
        }
      }
    }
    return encoded
  }

  private fun addDescriptorMapProperty(builder: TypeSpec.Builder, encoded: MutableMap<String, String>) {
    builder.addProperty(
      PropertySpec
        .builder("descriptorMap", Map::class.parameterizedBy(
            String::class, DescriptorProtos.FileDescriptorProto::class
          )
        )
        .addModifiers(KModifier.PRIVATE)
        .initializer(
          encoded.toList().fold(CodeBlock.builder().addStatement("mapOf(")) { b, (name, data) ->
            b.withIndent {
              this.addStatement("\"$name\" to descriptorFor(arrayOf(")
                // Split the string to chunks because max string length in a class file is 64k bytes
                .withIndent {
                  data.chunked(FDS_CHUNK_SIZE).fold(this) { b, c ->
                    b.addStatement("\"$c\",")
                  }
                }.addStatement(")),")
            }
          }.addStatement(")").build()
        ).build()
    )
  }

  private fun addFileDescriptorFunction(builder: TypeSpec.Builder) {
    builder.addFunction(
      FunSpec
        .builder("fileDescriptor")
        .addModifiers(KModifier.PRIVATE)
        .addParameter("path", String::class)
        .addParameter("visited", Set::class.parameterizedBy(String::class))
        .returns(Descriptors.FileDescriptor::class)
        .addCode(
          """
            val proto = descriptorMap[path]!!
            val deps = proto.dependencyList.filter { !visited.contains(it) }.map { fileDescriptor(it, visited + path) }
            return Descriptors.FileDescriptor.buildFrom(proto, deps.toTypedArray())
          """.trimIndent()
        ).build()
    )
  }

  private fun addDescriptorForFunction(builder: TypeSpec.Builder) {
    builder.addFunction(
      FunSpec
        .builder("descriptorFor")
        .addModifiers(KModifier.PRIVATE)
        .addParameter("data", Array::class.parameterizedBy(String::class))
        .returns(DescriptorProtos.FileDescriptorProto::class)
        .addCode(
          """
            val str = data.fold(java.lang.StringBuilder()) { b, s -> b.append(s) }.toString()
            val bytes = java.util.Base64.getDecoder().decode(str)
            return DescriptorProtos.FileDescriptorProto.parseFrom(bytes)
          """.trimIndent()
        ).build()
    )
  }

  private const val FDS_CHUNK_SIZE = 80
}
