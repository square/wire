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
package com.squareup.wire.schema.internal

import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.Syntax
import com.squareup.wire.internal.camelCase
import com.squareup.wire.schema.EnumConstant
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.Options
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Service
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.toByteString

/**
 * This class encodes files from a Wire schema using the types in protobuf's `descriptor.proto`.
 * Unfortunately, the two models don't line up directly:
 *
 *  * Wire keeps a heterogeneous list of messages and enums; `descriptor.proto` keeps each in its
 *    own list.
 *
 *  * Descriptors don't have first class support for [Field.EncodeMode.OMIT_IDENTITY], which is the
 *    default in proto3. Instead these are synthesized with oneofs.
 *
 *  * Descriptors don't support maps. Instead these are synthesized with entry classes.
 *
 * This file requires we manually keep tags and types in sync with `descriptor.proto`.
 *
 * TODO(jwilson): this class doesn't yet extension ranges and several other fields that are
 *     commented out below.
 */
class SchemaEncoder(
  private val schema: Schema
) {
  private val fileOptionsProtoAdapter =
    schema.protoAdapter(Options.FILE_OPTIONS.toString(), false)
  private val messageOptionsProtoAdapter =
    schema.protoAdapter(Options.MESSAGE_OPTIONS.toString(), false)
  private val fieldOptionsProtoAdapter =
    schema.protoAdapter(Options.FIELD_OPTIONS.toString(), false)
  private val enumOptionsProtoAdapter =
    schema.protoAdapter(Options.ENUM_OPTIONS.toString(), false)
  private val enumValueOptionsProtoAdapter =
    schema.protoAdapter(Options.ENUM_VALUE_OPTIONS.toString(), false)
  private val serviceOptionsProtoAdapter =
    schema.protoAdapter(Options.SERVICE_OPTIONS.toString(), false)
  private val rpcOptionsProtoAdapter =
    schema.protoAdapter(Options.METHOD_OPTIONS.toString(), false)

  fun encode(protoFile: ProtoFile): ByteString {
    return fileEncoder.encode(protoFile).toByteString()
  }

  private val fileEncoder : Encoder<ProtoFile> = object : Encoder<ProtoFile>() {
    override fun encode(writer: ProtoWriter, value: ProtoFile) {
      STRING.encodeWithTag(writer, 1, value.location.path)
      STRING.encodeWithTag(writer, 2, value.packageName)
      STRING.asRepeated().encodeWithTag(writer, 3, value.imports)
      // INT32.asRepeated().encodeWithTag(writer, 10, value.public_dependency)
      // INT32.asRepeated().encodeWithTag(writer, 11, value.weak_dependency)
      messageEncoder.asRepeated()
        .encodeWithTag(writer, 4, value.types.filterIsInstance<MessageType>())
      enumEncoder.asRepeated().encodeWithTag(writer, 5, value.types.filterIsInstance<EnumType>())
      serviceEncoder.asRepeated().encodeWithTag(writer, 6, value.services)

      // TODO(jwilson): can extension fields be maps?
      for (extend in value.extendList) {
        fieldEncoder.asRepeated().encodeWithTag(writer, 7,
          extend.fields.map {
            EncodedField(
              syntax = value.syntax,
              field = it,
              extendee = extend.type!!.dotName
            )
          })
      }

      fileOptionsProtoAdapter.encodeWithTag(writer, 8, value.options.toJsonOptions())
      // SourceCodeInfo.ADAPTER.encodeWithTag(writer, 9, value.source_code_info)

      if (value.syntax != Syntax.PROTO_2) {
        STRING.encodeWithTag(writer, 12, value.syntax?.toString())
      }
    }
  }

  private val messageEncoder : Encoder<MessageType> = object : Encoder<MessageType>() {
    override fun encode(writer: ProtoWriter, value: MessageType) {
      val syntax = schema.protoFile(value.type)!!.syntax

      val syntheticMaps = collectSyntheticMapEntries(value.type.toString(), value.declaredFields)

      val encodedOneOfs = mutableListOf<EncodedOneOf>()

      // Collect the true oneofs.
      for (oneOf in value.oneOfs) {
        val oneOfFields = oneOf.fields.map {
          EncodedField(
            syntax = syntax,
            field = it,
            oneOfIndex = encodedOneOfs.size
          )
        }
        encodedOneOfs.add(EncodedOneOf(oneOf.name, fields = oneOfFields))
      }

      // Collect encoded fields, synthesizing map types and oneofs.
      val encodedFields = mutableListOf<EncodedField>()
      for (field in value.declaredFields) {
        val syntheticMap = syntheticMaps[field]
        var encodedField = EncodedField(
          syntax = syntax,
          field = field,
          type = syntheticMap?.fieldType ?: field.type!!
        )
        if (encodedField.isProto3Optional) {
          encodedField = encodedField.copy(oneOfIndex = encodedOneOfs.size)
          encodedOneOfs += EncodedOneOf("_${field.name}")
        }
        encodedFields += encodedField
      }

      STRING.encodeWithTag(writer, 1, value.type.simpleName)

      val fieldsAndOneOfFields = (encodedFields + encodedOneOfs.flatMap { it.fields })
        .sortedBy { it.field.tag }
      fieldEncoder.asRepeated().encodeWithTag(writer, 2, fieldsAndOneOfFields)

      // FieldDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 6, value.extension)

      // Real and synthetic nested types.
      this.asRepeated().encodeWithTag(writer, 3, value.nestedTypes.filterIsInstance<MessageType>())
      syntheticMapEntryEncoder.asRepeated().encodeWithTag(writer, 3, syntheticMaps.values.toList())

      enumEncoder.asRepeated()
        .encodeWithTag(writer, 4, value.nestedTypes.filterIsInstance<EnumType>())
      // ExtensionRange.ADAPTER.asRepeated().encodeWithTag(writer, 5, value.extension_range)

      // Real and synthetic oneofs.
      oneOfEncoder.asRepeated().encodeWithTag(writer, 8, encodedOneOfs)

      messageOptionsProtoAdapter.encodeWithTag(writer, 7, value.options.toJsonOptions())

      // ReservedRange.ADAPTER.asRepeated().encodeWithTag(writer, 9, value.reserved_range)
      // STRING.asRepeated().encodeWithTag(writer, 10, value.reserved_name)
    }
  }

  /**
   * Create synthetic map entry types for [fields]. These replace the fields' natural types and will
   * be emitted as children of the fields' declaring message.
   */
  private fun collectSyntheticMapEntries(
    enclosingTypeOrPackage: String?,
    fields: List<Field>
  ): Map<Field, SyntheticMapEntry> {
    val result = mutableMapOf<Field, SyntheticMapEntry>()
    for (field in fields) {
      val fieldType = field.type!!
      if (fieldType.isMap) {
        val name = camelCase(field.name, upperCamel = true) + "Entry"
        result[field] = SyntheticMapEntry(
          enclosingTypeOrPackage = enclosingTypeOrPackage,
          name = name,
          keyType = fieldType.keyType!!,
          valueType = fieldType.valueType!!
        )
      }
    }
    return result
  }

  private class SyntheticMapEntry(
    enclosingTypeOrPackage: String?,
    val name: String,
    val keyType: ProtoType,
    val valueType: ProtoType
  ) {
    val fieldType = ProtoType.get(enclosingTypeOrPackage, name)
  }

  private val syntheticMapEntryEncoder: Encoder<SyntheticMapEntry> =
    object : Encoder<SyntheticMapEntry>() {
      val keyFieldEncoder = object : Encoder<SyntheticMapEntry>() {
        override fun encode(writer: ProtoWriter, value: SyntheticMapEntry) {
          STRING.encodeWithTag(writer, 1, "key")
          INT32.encodeWithTag(writer, 3, 1)
          INT32.encodeWithTag(writer, 4, 1) // 1 = Field.Label.OPTIONAL
          INT32.encodeWithTag(writer, 5, value.keyType.typeTag)
          if (!value.keyType.isScalar) {
            STRING.encodeWithTag(writer, 6, value.keyType.dotName)
          }
        }
      }

      val valueFieldEncoder = object : Encoder<SyntheticMapEntry>() {
        override fun encode(writer: ProtoWriter, value: SyntheticMapEntry) {
          STRING.encodeWithTag(writer, 1, "value")
          INT32.encodeWithTag(writer, 3, 2)
          INT32.encodeWithTag(writer, 4, 1) // 1 = Field.Label.OPTIONAL
          INT32.encodeWithTag(writer, 5, value.valueType.typeTag)
          if (!value.valueType.isScalar) {
            STRING.encodeWithTag(writer, 6, value.valueType.dotName)
          }
        }
      }

      override fun encode(writer: ProtoWriter, value: SyntheticMapEntry) {
        STRING.encodeWithTag(writer, 1, value.name)
        keyFieldEncoder.encodeWithTag(writer, 2, value)
        valueFieldEncoder.encodeWithTag(writer, 2, value)
        messageOptionsProtoAdapter.encodeWithTag(writer, 7, mapOf("map_entry" to true))
      }
    }

  /** Supplements the schema [Field] with overrides. */
  private data class EncodedField(
    val syntax: Syntax?,
    val field: Field,
    val type: ProtoType = field.type!!,
    val extendee: String? = null,
    val oneOfIndex: Int? = null
  ) {
    val isProto3Optional
      get() = syntax == Syntax.PROTO_3 && field.label == Field.Label.OPTIONAL
  }

  private val fieldEncoder : Encoder<EncodedField> = object : Encoder<EncodedField>() {
    override fun encode(writer: ProtoWriter, value: EncodedField) {
      STRING.encodeWithTag(writer, 1, value.field.name)
      INT32.encodeWithTag(writer, 3, value.field.tag)
      INT32.encodeWithTag(writer, 4, value.field.labelTag)
      INT32.encodeWithTag(writer, 5, value.field.type!!.typeTag)
      if (!value.type.isScalar) {
        STRING.encodeWithTag(writer, 6, value.type.dotName)
      }
      STRING.encodeWithTag(writer, 2, value.extendee)
      STRING.encodeWithTag(writer, 7, value.field.default)
      if (value.syntax == Syntax.PROTO_2 && value.field.jsonName != value.field.name) {
        STRING.encodeWithTag(writer, 10, value.field.jsonName)
      }
      fieldOptionsProtoAdapter.encodeWithTag(writer, 8, value.field.options.toJsonOptions())
      if (value.isProto3Optional) {
        BOOL.encodeWithTag(writer, 17, true)
      }
      INT32.encodeWithTag(writer, 9, value.oneOfIndex)
    }
  }

  private val Field.labelTag: Int
    get() {
      return when (encodeMode) {
        Field.EncodeMode.NULL_IF_ABSENT, Field.EncodeMode.OMIT_IDENTITY -> 1
        Field.EncodeMode.REQUIRED -> 2
        Field.EncodeMode.REPEATED, Field.EncodeMode.PACKED, Field.EncodeMode.MAP -> 3
        else -> error("unexpected encodeMode: $encodeMode")
      }
    }

  private val ProtoType.dotName
    get() = ".$this"

  private val ProtoType.typeTag: Int
    get() {
      return when {
        this == ProtoType.DOUBLE -> 1
        this == ProtoType.FLOAT -> 2
        this == ProtoType.INT64 -> 3
        this == ProtoType.UINT64 -> 4
        this == ProtoType.INT32 -> 5
        this == ProtoType.FIXED64 -> 6
        this == ProtoType.FIXED32 -> 7
        this == ProtoType.BOOL -> 8
        this == ProtoType.STRING -> 9
        schema.getType(this) is MessageType -> 11
        this == ProtoType.BYTES -> 12
        this == ProtoType.UINT32 -> 13
        schema.getType(this) is EnumType -> 14
        this == ProtoType.SFIXED32 -> 15
        this == ProtoType.SFIXED64 -> 16
        this == ProtoType.SINT32 -> 17
        this == ProtoType.SINT64 -> 18
        this.isMap -> 11 // Maps are encoded as messages.
        else -> error("unexpected type: $this")
      }
    }

  private class EncodedOneOf(
    val name: String,
    val fields: List<EncodedField> = listOf()
  )

  private val oneOfEncoder : Encoder<EncodedOneOf> = object : Encoder<EncodedOneOf>() {
    override fun encode(writer: ProtoWriter, value: EncodedOneOf) {
      STRING.encodeWithTag(writer, 1, value.name)
      // OneofOptions.ADAPTER.encodeWithTag(writer, 2, value.options)
    }
  }

  private val enumEncoder : Encoder<EnumType> = object : Encoder<EnumType>() {
    override fun encode(writer: ProtoWriter, value: EnumType) {
      STRING.encodeWithTag(writer, 1, value.name)
      enumConstantEncoder.asRepeated().encodeWithTag(writer, 2, value.constants)
      enumOptionsProtoAdapter.encodeWithTag(writer, 3, value.options.toJsonOptions())
      // EnumReservedRange.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.reserved_range)
      // STRING.asRepeated().encodeWithTag(writer, 5, value.reserved_name)
    }
  }

  private val enumConstantEncoder : Encoder<EnumConstant> = object : Encoder<EnumConstant>() {
    override fun encode(writer: ProtoWriter, value: EnumConstant) {
      STRING.encodeWithTag(writer, 1, value.name)
      INT32.encodeWithTag(writer, 2, value.tag)
      enumValueOptionsProtoAdapter.encodeWithTag(writer, 3, value.options.toJsonOptions())
    }
  }

  private val serviceEncoder : Encoder<Service> = object : Encoder<Service>() {
    override fun encode(writer: ProtoWriter, value: Service) {
      STRING.encodeWithTag(writer, 1, value.name)
      rpcEncoder.asRepeated().encodeWithTag(writer, 2, value.rpcs)
      serviceOptionsProtoAdapter.encodeWithTag(writer, 3, value.options.toJsonOptions())
    }
  }

  private val rpcEncoder : Encoder<Rpc> = object : Encoder<Rpc>() {
    override fun encode(writer: ProtoWriter, value: Rpc) {
      STRING.encodeWithTag(writer, 1, value.name)
      STRING.encodeWithTag(writer, 2, value.requestType!!.dotName)
      STRING.encodeWithTag(writer, 3, value.responseType!!.dotName)
      rpcOptionsProtoAdapter.encodeWithTag(writer, 4, value.options.toJsonOptions())
      if (value.requestStreaming) {
        BOOL.encodeWithTag(writer, 5, value.requestStreaming)
      }
      if (value.responseStreaming) {
        BOOL.encodeWithTag(writer, 6, value.responseStreaming)
      }
    }
  }

  /** Encodes a synthetic map type. */
  private abstract class Encoder<T> : ProtoAdapter<T>(
    FieldEncoding.LENGTH_DELIMITED, null, null, Syntax.PROTO_2
  ) {
    override fun redact(value: T) = value

    /**
     * Rather than implementing encodedSize we take a shortcut and double encode. Ths is a good
     * strategy but an inefficient implementation; it's quadratic in the depth of message nesting.
     */
    override fun encodedSize(value: T): Int {
      val buffer = Buffer()
      val writer = ProtoWriter(buffer)
      encode(writer, value)
      return buffer.size.toInt()
        .also { buffer.clear() }
    }

    override fun decode(reader: ProtoReader): T {
      throw UnsupportedOperationException()
    }
  }

  /**
   * Converts this options instance to a JSON-style value that the runtime adapter can process.
   *
   * TODO: offer an alternative to SchemaProtoAdapterFactory that uses ProtoMember or tag keys so
   *     we don't need a clumsy conversion through JSON.
   */
  private fun Options.toJsonOptions(): Any? {
    val optionsMap = map
    if (optionsMap.isEmpty()) return null

    val result = mutableMapOf<String, Any>()
    for ((key, value) in optionsMap) {
      val field = schema.getField(key) ?: error("unexpected options field: $key")
      result[field.name] = toJson(field, value) ?: continue
    }

    return result
  }

  private fun toJson(field: Field, value: Any?): Any? {
    // TODO: use optionValueToInt(value) when that's available in commonMain.
    return when (field.type) {
      ProtoType.INT32 -> (value as String).toInt()
      ProtoType.BOOL -> (value as String).toBoolean()
      ProtoType.STRING -> value as String
      else -> error("field type not implemented: $field")
    }
  }
}
