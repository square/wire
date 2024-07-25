// Code generated by Wire protocol buffer compiler, do not edit.
// Source: com.squareup.wire.protos.kotlin.map.Thing in map.proto
@file:Suppress(
  "DEPRECATION",
  "RUNTIME_ANNOTATION_NOT_SUPPORTED",
)

package com.squareup.wire.protos.kotlin.map

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.WireField
import com.squareup.wire.`internal`.JvmField
import com.squareup.wire.`internal`.JvmSynthetic
import com.squareup.wire.`internal`.sanitize
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import okio.ByteString

public class Thing(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    schemaIndex = 0,
  )
  @JvmField
  public val name: String? = null,
  unknownFields: ByteString = ByteString.EMPTY,
) : Message<Thing, Thing.Builder>(ADAPTER, unknownFields) {
  override fun newBuilder(): Builder {
    val builder = Builder()
    builder.name = name
    builder.addUnknownFields(unknownFields)
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is Thing) return false
    if (unknownFields != other.unknownFields) return false
    if (name != other.name) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + (name?.hashCode() ?: 0)
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (name != null) result += """name=${sanitize(name)}"""
    return result.joinToString(prefix = "Thing{", separator = ", ", postfix = "}")
  }

  public fun copy(name: String? = this.name, unknownFields: ByteString = this.unknownFields): Thing
      = Thing(name, unknownFields)

  public class Builder : Message.Builder<Thing, Builder>() {
    @JvmField
    public var name: String? = null

    public fun name(name: String?): Builder {
      this.name = name
      return this
    }

    override fun build(): Thing = Thing(
      name = name,
      unknownFields = buildUnknownFields()
    )
  }

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<Thing> = object : ProtoAdapter<Thing>(
      FieldEncoding.LENGTH_DELIMITED, 
      Thing::class, 
      "type.googleapis.com/com.squareup.wire.protos.kotlin.map.Thing", 
      PROTO_2, 
      null, 
      "map.proto"
    ) {
      override fun encodedSize(`value`: Thing): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
        return size
      }

      override fun encode(writer: ProtoWriter, `value`: Thing) {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
        writer.writeBytes(value.unknownFields)
      }

      override fun encode(writer: ReverseProtoWriter, `value`: Thing) {
        writer.writeBytes(value.unknownFields)
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
      }

      override fun decode(reader: ProtoReader): Thing {
        var name: String? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> name = ProtoAdapter.STRING.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return Thing(
          name = name,
          unknownFields = unknownFields
        )
      }

      override fun redact(`value`: Thing): Thing = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L

    @JvmSynthetic
    public inline fun build(body: Builder.() -> Unit): Thing = Builder().apply(body).build()
  }
}
