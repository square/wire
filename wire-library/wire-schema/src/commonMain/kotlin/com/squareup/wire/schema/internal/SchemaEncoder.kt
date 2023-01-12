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
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.Syntax
import com.squareup.wire.internal.camelCase
import com.squareup.wire.schema.EnumConstant
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.Options
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoMember
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Service
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

  private val fileEncoder: Encoder<ProtoFile> = object : Encoder<ProtoFile>() {
    override fun encode(writer: ReverseProtoWriter, value: ProtoFile) {
      if (value.syntax != Syntax.PROTO_2) {
        STRING.encodeWithTag(writer, 12, value.syntax?.toString())
      }

      // SourceCodeInfo.ADAPTER.encodeWithTag(writer, 9, value.source_code_info)
      fileOptionsProtoAdapter.encodeWithTag(writer, 8, value.options.toJsonOptions())

      // TODO(jwilson): can extension fields be maps?
      for (extend in value.extendList.reversed()) {
        fieldEncoder.asRepeated().encodeWithTag(
          writer, 7,
          extend.fields.map {
            EncodedField(
              syntax = value.syntax,
              field = it,
              extendee = extend.type!!.dotName
            )
          }
        )
      }

      serviceEncoder.asRepeated().encodeWithTag(writer, 6, value.services)
      enumEncoder.asRepeated().encodeWithTag(writer, 5, value.types.filterIsInstance<EnumType>())
      messageEncoder.asRepeated()
        .encodeWithTag(writer, 4, value.types.filterIsInstance<MessageType>())
      // INT32.asRepeated().encodeWithTag(writer, 11, value.weak_dependency)
      // INT32.asRepeated().encodeWithTag(writer, 10, value.public_dependency)
      STRING.asRepeated().encodeWithTag(writer, 3, value.imports)
      STRING.encodeWithTag(writer, 2, value.packageName)
      STRING.encodeWithTag(writer, 1, value.location.path)
    }
  }

  private val messageEncoder: Encoder<MessageType> = object : Encoder<MessageType>() {
    override fun encode(writer: ReverseProtoWriter, value: MessageType) {
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

      // STRING.asRepeated().encodeWithTag(writer, 10, value.reserved_name)
      // ReservedRange.ADAPTER.asRepeated().encodeWithTag(writer, 9, value.reserved_range)

      messageOptionsProtoAdapter.encodeWithTag(writer, 7, value.options.toJsonOptions())

      // Real and synthetic oneofs.
      oneOfEncoder.asRepeated().encodeWithTag(writer, 8, encodedOneOfs)

      extensionRangeEncoder.asRepeated()
        .encodeWithTag(writer, 5, value.extensionsList.flatMap { it.values })
      enumEncoder.asRepeated()
        .encodeWithTag(writer, 4, value.nestedTypes.filterIsInstance<EnumType>())

      // Real and synthetic nested types.
      syntheticMapEntryEncoder.asRepeated().encodeWithTag(writer, 3, syntheticMaps.values.toList())
      this.asRepeated().encodeWithTag(writer, 3, value.nestedTypes.filterIsInstance<MessageType>())

      // FieldDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 6, value.extension)

      val fieldsAndOneOfFields = (encodedFields + encodedOneOfs.flatMap { it.fields })
        .sortedWith(compareBy({ it.field.location.line }, { it.field.location.column }))
      fieldEncoder.asRepeated().encodeWithTag(writer, 2, fieldsAndOneOfFields)

      STRING.encodeWithTag(writer, 1, value.type.simpleName)
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
        override fun encode(writer: ReverseProtoWriter, value: SyntheticMapEntry) {
          STRING.encodeWithTag(writer, 10, "key")
          if (!value.keyType.isScalar) {
            STRING.encodeWithTag(writer, 6, value.keyType.dotName)
          }
          INT32.encodeWithTag(writer, 5, value.keyType.typeTag)
          INT32.encodeWithTag(writer, 4, 1) // 1 = Field.Label.OPTIONAL
          INT32.encodeWithTag(writer, 3, 1)
          STRING.encodeWithTag(writer, 1, "key")
        }
      }

      val valueFieldEncoder = object : Encoder<SyntheticMapEntry>() {
        override fun encode(writer: ReverseProtoWriter, value: SyntheticMapEntry) {
          STRING.encodeWithTag(writer, 10, "value")
          if (!value.valueType.isScalar) {
            STRING.encodeWithTag(writer, 6, value.valueType.dotName)
          }
          INT32.encodeWithTag(writer, 5, value.valueType.typeTag)
          INT32.encodeWithTag(writer, 4, 1) // 1 = Field.Label.OPTIONAL
          INT32.encodeWithTag(writer, 3, 2)
          STRING.encodeWithTag(writer, 1, "value")
        }
      }

      override fun encode(writer: ReverseProtoWriter, value: SyntheticMapEntry) {
        messageOptionsProtoAdapter.encodeWithTag(writer, 7, mapOf("map_entry" to true))
        valueFieldEncoder.encodeWithTag(writer, 2, value)
        keyFieldEncoder.encodeWithTag(writer, 2, value)
        STRING.encodeWithTag(writer, 1, value.name)
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

  private val fieldEncoder: Encoder<EncodedField> = object : Encoder<EncodedField>() {
    override fun encode(writer: ReverseProtoWriter, value: EncodedField) {
      INT32.encodeWithTag(writer, 9, value.oneOfIndex)
      if (value.isProto3Optional) {
        BOOL.encodeWithTag(writer, 17, true)
      }
      fieldOptionsProtoAdapter.encodeWithTag(writer, 8, value.field.options.toJsonOptions())
      if (value.syntax == Syntax.PROTO_2 && value.field.jsonName != value.field.name) {
        STRING.encodeWithTag(writer, 10, value.field.jsonName)
      }
      STRING.encodeWithTag(writer, 7, value.field.default)
      STRING.encodeWithTag(writer, 2, value.extendee)
      if (!value.type.isScalar) {
        STRING.encodeWithTag(writer, 6, value.type.dotName)
      }
      INT32.encodeWithTag(writer, 5, value.field.type!!.typeTag)
      INT32.encodeWithTag(writer, 4, value.field.labelTag)
      INT32.encodeWithTag(writer, 3, value.field.tag)
      STRING.encodeWithTag(writer, 1, value.field.name)
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

  private val oneOfEncoder: Encoder<EncodedOneOf> = object : Encoder<EncodedOneOf>() {
    override fun encode(writer: ReverseProtoWriter, value: EncodedOneOf) {
      // OneofOptions.ADAPTER.encodeWithTag(writer, 2, value.options)
      STRING.encodeWithTag(writer, 1, value.name)
    }
  }

  private val enumEncoder: Encoder<EnumType> = object : Encoder<EnumType>() {
    override fun encode(writer: ReverseProtoWriter, value: EnumType) {
      // STRING.asRepeated().encodeWithTag(writer, 5, value.reserved_name)
      // EnumReservedRange.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.reserved_range)
      enumOptionsProtoAdapter.encodeWithTag(writer, 3, value.options.toJsonOptions())
      enumConstantEncoder.asRepeated().encodeWithTag(writer, 2, value.constants)
      STRING.encodeWithTag(writer, 1, value.name)
    }
  }

  private val extensionRangeEncoder: Encoder<Any> = object : Encoder<Any>() {
    override fun encode(writer: ReverseProtoWriter, value: Any) {
      when (value) {
        is IntRange -> {
          INT32.encodeWithTag(writer, 2, value.last + 1) // Exclusive.
          INT32.encodeWithTag(writer, 1, value.first) // Inclusive.
        }
        is Int -> {
          INT32.encodeWithTag(writer, 2, value + 1) // Exclusive.
          INT32.encodeWithTag(writer, 1, value) // Inclusive.
        }
        else -> error("unexpected extension range: $value")
      }
    }
  }

  private val enumConstantEncoder: Encoder<EnumConstant> = object : Encoder<EnumConstant>() {
    override fun encode(writer: ReverseProtoWriter, value: EnumConstant) {
      enumValueOptionsProtoAdapter.encodeWithTag(writer, 3, value.options.toJsonOptions())
      INT32.encodeWithTag(writer, 2, value.tag)
      STRING.encodeWithTag(writer, 1, value.name)
    }
  }

  private val serviceEncoder: Encoder<Service> = object : Encoder<Service>() {
    override fun encode(writer: ReverseProtoWriter, value: Service) {
      serviceOptionsProtoAdapter.encodeWithTag(writer, 3, value.options.toJsonOptions())
      rpcEncoder.asRepeated().encodeWithTag(writer, 2, value.rpcs)
      STRING.encodeWithTag(writer, 1, value.name)
    }
  }

  private val rpcEncoder: Encoder<Rpc> = object : Encoder<Rpc>() {
    override fun encode(writer: ReverseProtoWriter, value: Rpc) {
      if (value.responseStreaming) {
        BOOL.encodeWithTag(writer, 6, value.responseStreaming)
      }
      if (value.requestStreaming) {
        BOOL.encodeWithTag(writer, 5, value.requestStreaming)
      }
      rpcOptionsProtoAdapter.encodeWithTag(writer, 4, value.options.toJsonOptions())
      STRING.encodeWithTag(writer, 3, value.responseType!!.dotName)
      STRING.encodeWithTag(writer, 2, value.requestType!!.dotName)
      STRING.encodeWithTag(writer, 1, value.name)
    }
  }

  /** Encodes a synthetic map type. */
  private abstract class Encoder<T> : ProtoAdapter<T>(
    FieldEncoding.LENGTH_DELIMITED, null, null, Syntax.PROTO_2
  ) {
    override fun redact(value: T) = value
    override fun encodedSize(value: T): Int = throw UnsupportedOperationException()
    override fun encode(writer: ProtoWriter, value: T) = throw UnsupportedOperationException()
    override fun decode(reader: ProtoReader): T = throw UnsupportedOperationException()
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
      result[field.name] = toJson(field, value!!)
    }

    return result
  }

  private fun toJson(field: Field, value: Any): Any {
    return when {
      field.isRepeated -> (value as List<*>).map { toJsonSingle(field.type!!, it!!) }
      else -> toJsonSingle(field.type!!, value)
    }
  }

  private fun toJsonSingle(type: ProtoType, value: Any): Any {
    // TODO: use optionValueToInt(value) when that's available in commonMain.
    return when (type) {
      ProtoType.INT32 -> (value as String).toInt()
      ProtoType.DOUBLE -> (value as String).toDouble()
      ProtoType.BOOL -> (value as String).toBoolean()
      ProtoType.STRING -> value as String
      else -> {
        when (schema.getType(type)) {
          is MessageType -> toJsonMap(value as Map<ProtoMember, Any>)
          is EnumType -> value as String
          else -> error("not implemented yet!!")
        }
      }
    }
  }

  private fun toJsonMap(map: Map<ProtoMember, Any>): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>()
    for ((key, value) in map) {
      val field = schema.getField(key) ?: continue // TODO: warn about this??
      result[key.simpleName] = toJson(field, value)
    }
    return result
  }
}
