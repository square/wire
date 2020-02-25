/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema

import com.squareup.wire.FieldEncoding.LENGTH_DELIMITED
import com.squareup.wire.FieldEncoding.VARINT
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import kotlin.jvm.JvmField

/**
 * Creates type adapters to read and write protocol buffer data from a schema model. This doesn't
 * require an intermediate code gen step.
 */
internal class SchemaProtoAdapterFactory(
  val schema: Schema,
  private val includeUnknown: Boolean
) {
  private val adapterMap = mutableMapOf<ProtoType?, ProtoAdapter<*>>(
      ProtoType.BOOL to ProtoAdapter.BOOL,
      ProtoType.BYTES to ProtoAdapter.BYTES,
      ProtoType.DOUBLE to ProtoAdapter.DOUBLE,
      ProtoType.FLOAT to ProtoAdapter.FLOAT,
      ProtoType.FIXED32 to ProtoAdapter.FIXED32,
      ProtoType.FIXED64 to ProtoAdapter.FIXED64,
      ProtoType.INT32 to ProtoAdapter.INT32,
      ProtoType.INT64 to ProtoAdapter.INT64,
      ProtoType.SFIXED32 to ProtoAdapter.SFIXED32,
      ProtoType.SFIXED64 to ProtoAdapter.SFIXED64,
      ProtoType.SINT32 to ProtoAdapter.SINT32,
      ProtoType.SINT64 to ProtoAdapter.SINT64,
      ProtoType.STRING to ProtoAdapter.STRING,
      ProtoType.UINT32 to ProtoAdapter.UINT32,
      ProtoType.UINT64 to ProtoAdapter.UINT64
  )

  operator fun get(protoType: ProtoType): ProtoAdapter<Any> {
    if (protoType.isMap) throw UnsupportedOperationException("map types not supported")
    val result = adapterMap[protoType]
    if (result != null) {
      return result as ProtoAdapter<Any>
    }
    val type = requireNotNull(schema.getType(protoType)) { "unknown type: $protoType" }
    if (type is EnumType) {
      val enumAdapter = EnumAdapter(type)
      adapterMap[protoType] = enumAdapter
      return enumAdapter
    }
    if (type is MessageType) {
      val messageAdapter = MessageAdapter(type.type.typeUrl, includeUnknown)
      // Put the adapter in the map early to mitigate the recursive calls to get() made below.
      adapterMap[protoType] = messageAdapter
      for (field in type.fields()) {
        val fieldAdapter = Field(field.name, field.tag, field.isRepeated, get(field.type!!))
        messageAdapter.fieldsByName[field.name] = fieldAdapter
        messageAdapter.fieldsByTag[field.tag] = fieldAdapter
      }
      return messageAdapter as ProtoAdapter<Any>
    }
    throw IllegalArgumentException("unexpected type: $protoType")
  }

  internal class EnumAdapter(
    private val enumType: EnumType
  ) : ProtoAdapter<Any>(VARINT, Any::class, null) {
    override fun encodedSize(value: Any): Int {
      throw UnsupportedOperationException()
    }

    override fun encode(writer: ProtoWriter, value: Any) {
      if (value is String) {
        val constant = enumType.constant(value)
        writer.writeVarint32(constant!!.tag)
      } else if (value is Int) {
        writer.writeVarint32(value)
      } else {
        throw IllegalArgumentException("unexpected " + enumType.type + ": " + value)
      }
    }

    override fun decode(reader: ProtoReader): Any {
      val value = UINT32.decode(reader)
      val constant = enumType.constant(value)
      return constant?.name ?: value
    }

    override fun redact(value: Any): Any {
      throw UnsupportedOperationException()
    }
  }

  internal class MessageAdapter(
    typeUrl: String?,
    private val includeUnknown: Boolean
  ) : ProtoAdapter<Map<String, Any>>(LENGTH_DELIMITED, MutableMap::class, typeUrl) {
    @JvmField val fieldsByTag = mutableMapOf<Int, Field>()
    @JvmField val fieldsByName = mutableMapOf<String, Field>()

    override fun redact(value: Map<String, Any>): Map<String, Any> {
      throw UnsupportedOperationException()
    }

    override fun encodedSize(value: Map<String, Any>): Int {
      var size = 0
      for ((key, value1) in value) {
        val field = fieldsByName[key] ?: continue
        // Ignore unknown values!
        val protoAdapter = field.protoAdapter as ProtoAdapter<Any>
        if (field.repeated) {
          for (o in value1 as List<*>) {
            size += protoAdapter.encodedSizeWithTag(field.tag, o)
          }
        } else {
          size += protoAdapter.encodedSizeWithTag(field.tag, value1)
        }
      }
      return size
    }

    override fun encode(writer: ProtoWriter, value: Map<String, Any>) {
      for ((key, value1) in value) {
        val field = fieldsByName[key] ?: continue
        // Ignore unknown values!
        val protoAdapter = field.protoAdapter as ProtoAdapter<Any>
        if (field.repeated) {
          for (o in value1 as List<*>) {
            protoAdapter.encodeWithTag(writer, field.tag, o)
          }
        } else {
          protoAdapter.encodeWithTag(writer, field.tag, value1)
        }
      }
    }

    override fun decode(reader: ProtoReader): Map<String, Any> {
      val result = mutableMapOf<String, Any>()
      val token = reader.beginMessage()
      while (true) {
        val tag = reader.nextTag()
        if (tag == -1) break

        var field = fieldsByTag[tag]
        if (field == null) {
          field = if (includeUnknown) {
            val name = tag.toString()
            Field(name, tag, true, reader.peekFieldEncoding()!!.rawProtoAdapter())
          } else {
            reader.skip()
            continue
          }
        }
        val value = field.protoAdapter.decode(reader)!!
        if (field.repeated) {
          val values = result.getOrPut(field.name) { mutableListOf<Any>() } as MutableList<Any>
          values.add(value)
        } else {
          result[field.name] = value
        }
      }
      reader.endMessageAndGetUnknownFields(token) // Ignore return value
      return result
    }

    override fun toString(value: Map<String, Any>): String {
      throw UnsupportedOperationException()
    }
  }

  internal class Field(
    val name: String,
    val tag: Int,
    val repeated: Boolean,
    val protoAdapter: ProtoAdapter<*>
  )
}
