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
package com.squareup.wire

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.ExtensionRegistry
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.SchemaLoader
import com.squareup.wire.schema.internal.SchemaEncoder
import java.nio.file.FileSystems
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import squareup.proto2.kotlin.alltypes.AllTypesOuterClass as AllTypesOuterClassP2
import squareup.proto2.kotlin.interop.InteropServiceOuterClass as InteropServiceOuterClassP2
import squareup.proto2.kotlin.interop.InteropTest as InteropTestP2
import squareup.proto3.kotlin.alltypes.AllTypesOuterClass as AllTypesOuterClassP3
import squareup.proto3.kotlin.interop.type.InteropTypes as InteropTypesP2

class SchemaEncoderInteropTest {
  private val schema = SchemaLoader(FileSystems.getDefault())
    .apply {
      initRoots(sourcePath = listOf(Location.get("src/main/proto")))
    }
    .loadSchema()

  private val extensionRegistry = ExtensionRegistry.newInstance()
    .apply {
      InteropServiceOuterClassP2.registerAllExtensions(this)
    }

  @Test fun `proto2 interop_test`() {
    checkFileSchemasMatch(
      wireProtoFile = schema.protoFile("squareup/proto2/kotlin/interop/interop_test.proto")!!,
      protocProtoFile = InteropTestP2.getDescriptor().toProto()
    )
  }

  @Test fun `proto3 interop_types`() {
    checkFileSchemasMatch(
      wireProtoFile = schema.protoFile("squareup/proto3/kotlin/interop/type/interop_types.proto")!!,
      protocProtoFile = InteropTypesP2.getDescriptor().toProto()
    )
  }

  @Test fun `proto2 interop_service`() {
    checkFileSchemasMatch(
      wireProtoFile = schema.protoFile("squareup/proto2/kotlin/interop/interop_service.proto")!!,
      protocProtoFile = InteropServiceOuterClassP2.getDescriptor().toProto()
    )
  }

  @Test fun `proto2 all_types`() {
    checkFileSchemasMatch(
      wireProtoFile = schema.protoFile("squareup/proto2/kotlin/alltypes/all_types.proto")!!,
      protocProtoFile = AllTypesOuterClassP2.getDescriptor().toProto()
    )
  }

  @Test fun `proto3 all_types`() {
    checkFileSchemasMatch(
      wireProtoFile = schema.protoFile("squareup/proto3/kotlin/alltypes/all_types_test_proto3_optional.proto")!!,
      protocProtoFile = AllTypesOuterClassP3.getDescriptor().toProto()
    )
  }

  /**
   * Confirm the schemas described by [wireProtoFile] and [protocProtoFile] are equal.
   *
   */
  private fun checkFileSchemasMatch(
    wireProtoFile: ProtoFile,
    protocProtoFile: FileDescriptorProto
  ) {
    val wireBytes = SchemaEncoder(schema).encode(wireProtoFile)
    val wireDescriptor = FileDescriptorProto.parseFrom(wireBytes.toByteArray(), extensionRegistry)
    val unwantedValueStripper = UnwantedValueStripper(clearJsonName = true)
    assertThat(unwantedValueStripper.stripOptionsAndDefaults(wireDescriptor))
      .isEqualTo(unwantedValueStripper.stripOptionsAndDefaults(protocProtoFile))
  }
}
