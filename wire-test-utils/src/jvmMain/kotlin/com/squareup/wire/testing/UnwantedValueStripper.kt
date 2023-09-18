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
package com.squareup.wire.testing

import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorProto

class UnwantedValueStripper(
  val clearJsonName: Boolean = false,
) {

  /**
   * TODO: this strips defaults as they're not yet consistent with protoc. We should fix the
   *     implementation to match protoc.
   */
  fun stripOptionsAndDefaults(fileDescriptorProto: FileDescriptorProto): FileDescriptorProto {
    val messageTypeList = fileDescriptorProto.messageTypeList.map { stripOptionsAndDefaults(it) }
    val extensionList = fileDescriptorProto.extensionList.map { stripOptionsAndDefaults(it) }
    return fileDescriptorProto.toBuilder()
      .clearMessageType()
      .addAllMessageType(messageTypeList)
      .clearExtension()
      .addAllExtension(extensionList)
      .build()
  }

  fun stripOptionsAndDefaults(descriptorProto: DescriptorProto): DescriptorProto {
    val nestedTypeList = descriptorProto.nestedTypeList.map { stripOptionsAndDefaults(it) }
    val fieldList = descriptorProto.fieldList.map { stripOptionsAndDefaults(it) }
    return descriptorProto.toBuilder()
      .clearNestedType()
      .addAllNestedType(nestedTypeList)
      .clearField()
      .addAllField(fieldList)
      .clearExtensionRange()
      .build()
  }

  fun stripOptionsAndDefaults(fieldDescriptorProto: FieldDescriptorProto): FieldDescriptorProto {
    return fieldDescriptorProto.toBuilder()
      .clearDefaultValue()
      .apply {
        if (clearJsonName) {
          clearJsonName()
        }
      }
      .build()
  }
}
