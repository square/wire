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

import com.squareup.wire.FieldEncoding
import com.squareup.wire.FieldEncoding.VARINT
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.Syntax
import com.squareup.wire.WireField
import com.squareup.wire.internal.FieldOrOneOfBinding
import com.squareup.wire.internal.MessageBinding
import com.squareup.wire.internal.RuntimeMessageAdapter
import com.squareup.wire.schema.Field.EncodeMode
import okio.ByteString

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
      val messageBinding = SchemaMessageBinding(type.type.typeUrl, type.syntax, includeUnknown)
      // Put the adapter in the map early to mitigate the recursive calls to get() made below.
      val messageAdapter = RuntimeMessageAdapter(messageBinding)
      adapterMap[protoType] = messageAdapter
      for (field in type.fields) {
        messageBinding.fields[field.tag] = SchemaFieldOrOneOfBinding(field, null)
      }
      for (oneOf in type.oneOfs) {
        for (field in oneOf.fields) {
          messageBinding.fields[field.tag] = SchemaFieldOrOneOfBinding(field, oneOf)
        }
      }
      return messageAdapter as ProtoAdapter<Any>
    }
    throw IllegalArgumentException("unexpected type: $protoType")
  }

  private class EnumAdapter(
    private val enumType: EnumType
  ) : ProtoAdapter<Any>(VARINT, Any::class, null, enumType.syntax) {
    override fun encodedSize(value: Any): Int {
      if (value is String) {
        val constant = enumType.constant(value)!!
        return INT32.encodedSize(constant.tag)
      } else if (value is Int) {
        return INT32.encodedSize(value)
      } else {
        throw IllegalArgumentException("unexpected " + enumType.type + ": " + value)
      }
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

  private class SchemaMessageBinding(
    override val typeUrl: String?,
    override val syntax: Syntax,
    private val includeUnknown: Boolean
  ) : MessageBinding<Map<String, Any>, MutableMap<String, Any>> {

    override val messageType = Map::class

    override val fields =
      mutableMapOf<Int, FieldOrOneOfBinding<Map<String, Any>, MutableMap<String, Any>>>()

    override fun unknownFields(message: Map<String, Any>) = ByteString.EMPTY

    override fun getCachedSerializedSize(message: Map<String, Any>) = 0

    override fun setCachedSerializedSize(message: Map<String, Any>, size: Int) {
    }

    override fun newBuilder(): MutableMap<String, Any> = mutableMapOf()

    override fun build(builder: MutableMap<String, Any>) = builder.toMap()

    override fun addUnknownField(
      builder: MutableMap<String, Any>,
      tag: Int,
      fieldEncoding: FieldEncoding,
      value: Any?
    ) {
      if (!includeUnknown|| value == null) return
      val name = tag.toString()
      val values = builder.getOrPut(name) { mutableListOf<Any>() } as MutableList<Any>
      values.add(value)
    }

    override fun clearUnknownFields(builder: MutableMap<String, Any>) {
    }
  }

  private inner class SchemaFieldOrOneOfBinding(
    val field: Field,
    val oneOf: OneOf?
  ) : FieldOrOneOfBinding<Map<String, Any>, MutableMap<String, Any>>() {
    override val tag: Int = field.tag
    override val label: WireField.Label
      get() {
        if (oneOf != null) return WireField.Label.ONE_OF
        return when (this.field.encodeMode!!) {
          EncodeMode.OMIT_IDENTITY -> WireField.Label.OMIT_IDENTITY
          EncodeMode.NULL_IF_ABSENT -> WireField.Label.OPTIONAL
          EncodeMode.MAP, EncodeMode.PACKED, EncodeMode.REPEATED -> WireField.Label.REPEATED
          EncodeMode.REQUIRED -> WireField.Label.REQUIRED
        }
      }

    override val redacted: Boolean = field.isRedacted
    override val isMap: Boolean = field.type!!.isMap
    override val isMessage: Boolean = schema.getType(field.type!!) is MessageType
    override val name: String = field.name
    override val declaredName: String = field.name
    override val wireFieldJsonName: String = field.jsonName!!

    override val keyAdapter: ProtoAdapter<*>
      get() = get(this.field.type!!.keyType!!)

    override val singleAdapter: ProtoAdapter<*>
      get() = get(this.field.type!!)

    override fun value(builder: MutableMap<String, Any>, value: Any) {
      if (isMap) {
        val map = builder.getOrPut(field.name) { mutableMapOf<String, Any>() }
            as MutableMap<String, Any>
        map.putAll(value as Map<String, Any>)
      } else if (field.isRepeated) {
        val list = builder.getOrPut(field.name) { mutableListOf<Any>() }
            as MutableList<Any>
        list += value
      } else {
        set(builder, value)
      }
    }

    override fun set(builder: MutableMap<String, Any>, value: Any?) {
      builder[field.name] = value!!
    }

    override fun get(message: Map<String, Any>) = message[field.name]

    override fun getFromBuilder(builder: MutableMap<String, Any>) = builder[field.name]
  }
}
