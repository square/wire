/*
 * Copyright (C) 2021 Square, Inc.
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
package com.squareup.wire.kotlin.grpcserver

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label
import com.google.protobuf.Descriptors
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.Pruner
import com.squareup.wire.schema.PruningRules
import javax.script.ScriptEngineManager
import kotlin.test.assertEquals
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions
import org.junit.Test

internal class FileDescriptorGeneratorTest {
  private data class Schema(val path: String, val content: String)

  @Test
  fun `generated descriptor can be used correctly`() {
    val descriptor = descriptorFor(
      "test.proto",
      PruningRules.Builder(),
      Schema(
        "test.proto",
        """
      |syntax = "proto2";
      |
      |package test;
      |import "imported.proto";
      |
      |message Test {}
      |
        """.trimMargin(),
      ),
      Schema(
        "imported.proto",
        """
      |syntax = "proto2";
      |
      |package test;
      |
      |message Imported {}
      |
        """.trimMargin(),
      ),
    )

    assertEquals("test", descriptor.`package`)
    assertEquals("test.proto", descriptor.name)
    assertEquals(descriptor.messageTypes.size, 1)
    assertEquals("Test", descriptor.messageTypes.first().name)
    assertEquals(1, descriptor.dependencies.size)
  }

  @Test
  fun `generates descriptors correctly to references of nested enums after pruning`() {
    val descriptor = descriptorFor(
      "test.proto",
      PruningRules.Builder().addRoot("test.Caller"),
      Schema(
        "test.proto",
        """
      |syntax = "proto2";
      |package test;
      |
      |message Test {
      |  enum Nested {
      |    NESTED_UNDEFINED = 0;
      |    NESTED_DEFINED = 1;
      |  }
      |}
      |
      |message Caller {
      |  optional Test.Nested field = 1;
      |}
      |
        """.trimMargin(),
      ),
    )

    Assertions.assertThat(descriptor.toProto()).isEqualTo(
      DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("test.proto")
        .setPackage("test")
        .addMessageType(
          DescriptorProtos.DescriptorProto.newBuilder()
            .setName("Test")
            .addEnumType(
              DescriptorProtos.EnumDescriptorProto.newBuilder()
                .setName("Nested")
                .addValue(0, DescriptorProtos.EnumValueDescriptorProto.newBuilder().setName("NESTED_UNDEFINED").setNumber(0))
                .addValue(1, DescriptorProtos.EnumValueDescriptorProto.newBuilder().setName("NESTED_DEFINED").setNumber(1)),
            ),
        )
        .addMessageType(
          DescriptorProtos.DescriptorProto.newBuilder()
            .setName("Caller")
            .addField(
              DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName("field")
                .setNumber(1)
                .setTypeName(".test.Test.Nested")
                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM)
                .setLabel(Label.LABEL_OPTIONAL),
            ),
        )
        .build(),
    )
  }

  private fun descriptorFor(fileName: String, pruning: PruningRules.Builder, vararg schemas: Schema): Descriptors.FileDescriptor {
    val schema = buildSchema {
      schemas.forEach {
        apply { add(it.path.toPath(), it.content) }
      }
    }
    val pruner = Pruner(schema, pruning.build())
    val pruned = pruner.prune()
    val protoFile = pruned.protoFile(schemas.first().path.toPath())
    val file = FileSpec.scriptBuilder("test", "test.kts")
      .addType(
        TypeSpec.classBuilder("Test")
          .apply { FileDescriptorGenerator.addDescriptorDataProperty(this, protoFile, pruned) }
          .addFunction(
            FunSpec.builder("output")
              .returns(Descriptors.FileDescriptor::class)
              .addCode("return fileDescriptor(\"$fileName\", emptySet())")
              .build(),
          )
          .build(),
      ).addCode("Test().output()").build()

    val engine = ScriptEngineManager().getEngineByExtension("kts")
    val result = engine.eval(file.toString())
    return result as Descriptors.FileDescriptor
  }
}
