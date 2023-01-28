/*
 * Copyright (C) 2021 Square, Inc.
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

import com.google.protobuf.Descriptors
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.addLocal
import okio.Path.Companion.toPath
import okio.buffer
import okio.source
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import javax.script.ScriptEngineManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class FileDescriptorGeneratorTest {
  @Test
  fun `generated descriptor can be used correctly`() {
    val protoSchema = """
      |syntax = "proto2";
      |
      |package test;
      |import "imported.proto";
      |
      |message Test {}
      |""".trimMargin()

    val importedSchema = """
      |syntax = "proto2";
      |
      |package test;
      |
      |message Imported {}
      |""".trimMargin()

    val path = "test.proto".toPath()
    val imported = "imported.proto".toPath()
    val schema = buildSchema {
      add(imported, importedSchema)
      add(path, protoSchema)
    }

    val file = FileSpec.scriptBuilder("test", "test.kts")
      .addType(TypeSpec.classBuilder("Test")
        .apply { FileDescriptorGenerator.addDescriptorDataProperty(this, schema.protoFile(path), schema) }
        .addFunction(FunSpec.builder("output")
          .returns(Descriptors.FileDescriptor::class)
          .addCode("return fileDescriptor(\"test.proto\", emptySet())")
          .build())
        .build()
      ).addCode("Test().output()").build()

    val engine = ScriptEngineManager().getEngineByExtension("kts")
    val result = engine.eval(file.toString())
    val descriptor = result as Descriptors.FileDescriptor

    assertEquals("test", descriptor.`package`)
    assertEquals("test.proto", descriptor.name)
    assertEquals(descriptor.messageTypes.size, 1)
    assertEquals("Test", descriptor.messageTypes.first().name)
    assertEquals( 1, descriptor.dependencies.size)
  }
}
