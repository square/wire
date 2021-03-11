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
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** A single `.proto` file.  */
data class ProtoFileElement(
  val location: Location,
  val packageName: String? = null,
  val syntax: Syntax? = null,
  val imports: List<String> = emptyList(),
  val publicImports: List<String> = emptyList(),
  val types: List<TypeElement> = emptyList(),
  val services: List<ServiceElement> = emptyList(),
  val extendDeclarations: List<ExtendElement> = emptyList(),
  val options: List<OptionElement> = emptyList()
) {
  fun toSchema() = buildString {
    append("// Proto schema formatted by Wire, do not edit.\n")
    append("// Source: ${location.withPathOnly()}\n")

    if (syntax != null) {
      append('\n')
      append("syntax = \"$syntax\";\n")
    }
    if (packageName != null) {
      append('\n')
      append("package $packageName;\n")
    }
    if (imports.isNotEmpty() || publicImports.isNotEmpty()) {
      append('\n')
      for (file in imports) {
        append("import \"$file\";\n")
      }
      for (file in publicImports) {
        append("import public \"$file\";\n")
      }
    }
    if (options.isNotEmpty()) {
      append('\n')
      for (option in options) {
        append(option.toSchemaDeclaration())
      }
    }
    if (types.isNotEmpty()) {
      for (typeElement in types) {
        append('\n')
        append(typeElement.toSchema())
      }
    }
    if (extendDeclarations.isNotEmpty()) {
      for (extendDeclaration in extendDeclarations) {
        append('\n')
        append(extendDeclaration.toSchema())
      }
    }
    if (services.isNotEmpty()) {
      for (service in services) {
        append('\n')
        append(service.toSchema())
      }
    }
  }

  companion object {
    /** Returns an empty proto file to serve as a null object when a file cannot be found. */
    @JvmStatic
    fun empty(path: String): ProtoFileElement {
      return ProtoFileElement(location = Location.get(path))
    }

      @JvmField
      public val ADAPTER: ProtoAdapter<ProtoFileElement> = object :
        ProtoAdapter<ProtoFileElement>(
          FieldEncoding.LENGTH_DELIMITED,
          ProtoFileElement::class,
          "type.googleapis.com/google.protobuf.FileDescriptorProto",
          Syntax.PROTO_2,
          null
        ) {
        public override fun encodedSize(value: ProtoFileElement): Int {
          var size = 0
          size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.location.path)
          size += ProtoAdapter.STRING.encodedSizeWithTag(2, value.packageName)
//          size += ProtoAdapter.STRING.asRepeated().encodedSizeWithTag(3, value.dependency)
//          size += ProtoAdapter.INT32.asRepeated().encodedSizeWithTag(10, value.public_dependency)
//          size += ProtoAdapter.INT32.asRepeated().encodedSizeWithTag(11, value.weak_dependency)
          size += MessageElement.ADAPTER.asRepeated().encodedSizeWithTag(4, value.types.filterIsInstance<MessageElement>())
//          size += EnumDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(5, value.enum_type)
//          size += ServiceDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(6, value.service)
//          size += FieldDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(7, value.extension)
//          size += FileOptions.ADAPTER.encodedSizeWithTag(8, value.options)
//          size += SourceCodeInfo.ADAPTER.encodedSizeWithTag(9, value.source_code_info)
          size += ProtoAdapter.STRING.encodedSizeWithTag(12, value.syntax?.name)
          return size
        }

        public override fun encode(writer: ProtoWriter, value: ProtoFileElement): Unit {
          ProtoAdapter.STRING.encodeWithTag(writer, 1, value.location.path)
          ProtoAdapter.STRING.encodeWithTag(writer, 2, value.packageName)
//          ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 3, value.dependency)
//          ProtoAdapter.INT32.asRepeated().encodeWithTag(writer, 10, value.public_dependency)
//          ProtoAdapter.INT32.asRepeated().encodeWithTag(writer, 11, value.weak_dependency)
          MessageElement.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.types.filterIsInstance<MessageElement>())
//          EnumDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 5, value.enum_type)
//          ServiceDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 6, value.service)
//          FieldDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 7, value.extension)
//          FileOptions.ADAPTER.encodeWithTag(writer, 8, value.options)
//          SourceCodeInfo.ADAPTER.encodeWithTag(writer, 9, value.source_code_info)
          ProtoAdapter.STRING.encodeWithTag(writer, 12, value.syntax?.name)
        }

        public override fun decode(reader: ProtoReader): ProtoFileElement {
          var name: String? = null
          var package_: String? = null
          val dependency = mutableListOf<String>()
          val public_dependency = mutableListOf<Int>()
          val weak_dependency = mutableListOf<Int>()
          val message_type = mutableListOf<MessageElement>()
//          val enum_type = mutableListOf<EnumDescriptorProto>()
//          val service = mutableListOf<ServiceDescriptorProto>()
//          val extension = mutableListOf<FieldDescriptorProto>()
//          var options: FileOptions? = null
//          var source_code_info: SourceCodeInfo? = null
          var syntax: String? = null
          val unknownFields = reader.forEachTag { tag ->
            when (tag) {
              1 -> name = ProtoAdapter.STRING.decode(reader)
              2 -> package_ = ProtoAdapter.STRING.decode(reader)
              3 -> dependency.add(ProtoAdapter.STRING.decode(reader))
              10 -> public_dependency.add(ProtoAdapter.INT32.decode(reader))
              11 -> weak_dependency.add(ProtoAdapter.INT32.decode(reader))
              4 -> message_type.add(MessageElement.ADAPTER.decode(reader))
//              5 -> enum_type.add(EnumDescriptorProto.ADAPTER.decode(reader))
//              6 -> service.add(ServiceDescriptorProto.ADAPTER.decode(reader))
//              7 -> extension.add(FieldDescriptorProto.ADAPTER.decode(reader))
//              8 -> options = FileOptions.ADAPTER.decode(reader)
//              9 -> source_code_info = SourceCodeInfo.ADAPTER.decode(reader)
              12 -> syntax = ProtoAdapter.STRING.decode(reader)
              else -> reader.readUnknownField(tag)
            }
          }
          return ProtoFileElement(
            location = Location.get(name ?: ""),
            packageName = package_,
//            dependency = dependency,
//            public_dependency = public_dependency,
//            weak_dependency = weak_dependency,
            types = message_type,
//            enum_type = enum_type,
//            service = service,
//            extension = extension,
//            options = options,
//            source_code_info = source_code_info,
            syntax = syntax?.let { Syntax.get(it) }
          )
        }

        public override fun redact(value: ProtoFileElement): ProtoFileElement = value
      }
  }
}
