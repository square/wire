/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.*
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.appendDocumentation
import com.squareup.wire.schema.internal.appendIndented
import kotlin.jvm.JvmField

data class MessageElement(
  override val location: Location,
  override val name: String,
  override val documentation: String = "",
  override val nestedTypes: List<TypeElement> = emptyList(),
  override val options: List<OptionElement> = emptyList(),
  val reserveds: List<ReservedElement> = emptyList(),
  val fields: List<FieldElement> = emptyList(),
  val oneOfs: List<OneOfElement> = emptyList(),
  val extensions: List<ExtensionsElement> = emptyList(),
  val groups: List<GroupElement> = emptyList()
) : TypeElement {
  override fun toSchema() = buildString {
    appendDocumentation(documentation)
    append("message $name {")

    if (reserveds.isNotEmpty()) {
      append('\n')
      for (reserved in reserveds) {
        appendIndented(reserved.toSchema())
      }
    }
    if (options.isNotEmpty()) {
      append('\n')
      for (option in options) {
        appendIndented(option.toSchemaDeclaration())
      }
    }
    if (fields.isNotEmpty()) {
      for (field in fields) {
        append('\n')
        appendIndented(field.toSchema())
      }
    }
    if (oneOfs.isNotEmpty()) {
      for (oneOf in oneOfs) {
        append('\n')
        appendIndented(oneOf.toSchema())
      }
    }
    if (groups.isNotEmpty()) {
      for (group in groups) {
        append('\n')
        appendIndented(group.toSchema())
      }
    }
    if (extensions.isNotEmpty()) {
      append('\n')
      for (extension in extensions) {
        appendIndented(extension.toSchema())
      }
    }
    if (nestedTypes.isNotEmpty()) {
      for (type in nestedTypes) {
        append('\n')
        appendIndented(type.toSchema())
      }
    }
    append("}\n")
  }

  companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<MessageElement> = object : ProtoAdapter<MessageElement>(
      FieldEncoding.LENGTH_DELIMITED,
      MessageElement::class,
      "type.googleapis.com/google.protobuf.DescriptorProto",
      Syntax.PROTO_2,
      null
    ) {
      public override fun encodedSize(value: MessageElement): Int {
        var size = 0
        size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
//      size += FieldDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(2, value.field_)
//      size += FieldDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(6, value.extension)
//      size += DescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(3, value.nested_type)
//      size += EnumDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(4, value.enum_type)
//      size += ExtensionRange.ADAPTER.asRepeated().encodedSizeWithTag(5, value.extension_range)
//      size += OneofDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(8, value.oneof_decl)
//      size += MessageOptions.ADAPTER.encodedSizeWithTag(7, value.options)
//      size += ReservedRange.ADAPTER.asRepeated().encodedSizeWithTag(9, value.reserved_range)
//      size += ProtoAdapter.STRING.asRepeated().encodedSizeWithTag(10, value.reserved_name)
        return size
      }

      public override fun encode(writer: ProtoWriter, value: MessageElement): Unit {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
//      FieldDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 2, value.field_)
//      FieldDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 6, value.extension)
//      DescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 3, value.nested_type)
//      EnumDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.enum_type)
//      ExtensionRange.ADAPTER.asRepeated().encodeWithTag(writer, 5, value.extension_range)
//      OneofDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 8, value.oneof_decl)
//      MessageOptions.ADAPTER.encodeWithTag(writer, 7, value.options)
//      ReservedRange.ADAPTER.asRepeated().encodeWithTag(writer, 9, value.reserved_range)
//      ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 10, value.reserved_name)
      }

      public override fun decode(reader: ProtoReader): MessageElement {
        var name: String? = null
//      val field_ = mutableListOf<FieldDescriptorProto>()
//      val extension = mutableListOf<FieldDescriptorProto>()
//      val nested_type = mutableListOf<DescriptorProto>()
//      val enum_type = mutableListOf<EnumDescriptorProto>()
//      val extension_range = mutableListOf<ExtensionRange>()
//      val oneof_decl = mutableListOf<OneofDescriptorProto>()
//      var options: MessageOptions? = null
//      val reserved_range = mutableListOf<ReservedRange>()
//      val reserved_name = mutableListOf<String>()
        reader.forEachTag { tag ->
          when (tag) {
            1 -> name = ProtoAdapter.STRING.decode(reader)
//          2 -> field_.add(FieldDescriptorProto.ADAPTER.decode(reader))
//          6 -> extension.add(FieldDescriptorProto.ADAPTER.decode(reader))
//          3 -> nested_type.add(DescriptorProto.ADAPTER.decode(reader))
//          4 -> enum_type.add(EnumDescriptorProto.ADAPTER.decode(reader))
//          5 -> extension_range.add(ExtensionRange.ADAPTER.decode(reader))
//          8 -> oneof_decl.add(OneofDescriptorProto.ADAPTER.decode(reader))
//          7 -> options = MessageOptions.ADAPTER.decode(reader)
//          9 -> reserved_range.add(ReservedRange.ADAPTER.decode(reader))
//          10 -> reserved_name.add(ProtoAdapter.STRING.decode(reader))
            else -> reader.readUnknownField(tag)
          }
        }
        return MessageElement(
          location = Location("TODO", "TODO"),
          name = name ?: "",
//        field_ = field_,
//        extension = extension,
//        nested_type = nested_type,
//        enum_type = enum_type,
//        extension_range = extension_range,
//        oneof_decl = oneof_decl,
//        options = options,
//        reserved_range = reserved_range,
//        reserved_name = reserved_name,
//        unknownFields = unknownFields
        )
      }

      override fun redact(value: MessageElement) = value
    }
  }
}
