// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.simple.ExternalMessage in external_message.proto
package com.squareup.wire.protos.kotlin.simple

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.WireField
import com.squareup.wire.`internal`.checkElementsNotNull
import com.squareup.wire.`internal`.immutableCopyOf
import kotlin.Any
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Unit
import kotlin.collections.List
import kotlin.jvm.JvmField
import okio.ByteString

public class ExternalMessage(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#FLOAT"
  )
  @JvmField
  public val f: Float? = null,
  fooext: List<Int> = emptyList(),
  /**
   * Extension source: simple_message.proto
   */
  @field:WireField(
    tag = 126,
    adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  @JvmField
  public val barext: Int? = null,
  /**
   * Extension source: simple_message.proto
   */
  @field:WireField(
    tag = 127,
    adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  @JvmField
  public val bazext: Int? = null,
  /**
   * Extension source: simple_message.proto
   */
  @field:WireField(
    tag = 128,
    adapter = "com.squareup.wire.protos.kotlin.simple.SimpleMessage${'$'}NestedMessage#ADAPTER"
  )
  @JvmField
  public val nested_message_ext: SimpleMessage.NestedMessage? = null,
  /**
   * Extension source: simple_message.proto
   */
  @field:WireField(
    tag = 129,
    adapter = "com.squareup.wire.protos.kotlin.simple.SimpleMessage${'$'}NestedEnum#ADAPTER"
  )
  @JvmField
  public val nested_enum_ext: SimpleMessage.NestedEnum? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<ExternalMessage, ExternalMessage.Builder>(ADAPTER, unknownFields) {
  /**
   * Extension source: simple_message.proto
   */
  @field:WireField(
    tag = 125,
    adapter = "com.squareup.wire.ProtoAdapter#INT32",
    label = WireField.Label.REPEATED
  )
  @JvmField
  public val fooext: List<Int> = immutableCopyOf("fooext", fooext)

  public override fun newBuilder(): Builder {
    val builder = Builder()
    builder.f = f
    builder.fooext = fooext
    builder.barext = barext
    builder.bazext = bazext
    builder.nested_message_ext = nested_message_ext
    builder.nested_enum_ext = nested_enum_ext
    builder.addUnknownFields(unknownFields)
    return builder
  }

  public override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is ExternalMessage) return false
    if (unknownFields != other.unknownFields) return false
    if (f != other.f) return false
    if (fooext != other.fooext) return false
    if (barext != other.barext) return false
    if (bazext != other.bazext) return false
    if (nested_message_ext != other.nested_message_ext) return false
    if (nested_enum_ext != other.nested_enum_ext) return false
    return true
  }

  public override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + (f?.hashCode() ?: 0)
      result = result * 37 + fooext.hashCode()
      result = result * 37 + (barext?.hashCode() ?: 0)
      result = result * 37 + (bazext?.hashCode() ?: 0)
      result = result * 37 + (nested_message_ext?.hashCode() ?: 0)
      result = result * 37 + (nested_enum_ext?.hashCode() ?: 0)
      super.hashCode = result
    }
    return result
  }

  public override fun toString(): String {
    val result = mutableListOf<String>()
    if (f != null) result += """f=$f"""
    if (fooext.isNotEmpty()) result += """fooext=$fooext"""
    if (barext != null) result += """barext=$barext"""
    if (bazext != null) result += """bazext=$bazext"""
    if (nested_message_ext != null) result += """nested_message_ext=$nested_message_ext"""
    if (nested_enum_ext != null) result += """nested_enum_ext=$nested_enum_ext"""
    return result.joinToString(prefix = "ExternalMessage{", separator = ", ", postfix = "}")
  }

  public fun copy(
    f: Float? = this.f,
    fooext: List<Int> = this.fooext,
    barext: Int? = this.barext,
    bazext: Int? = this.bazext,
    nested_message_ext: SimpleMessage.NestedMessage? = this.nested_message_ext,
    nested_enum_ext: SimpleMessage.NestedEnum? = this.nested_enum_ext,
    unknownFields: ByteString = this.unknownFields
  ): ExternalMessage = ExternalMessage(f, fooext, barext, bazext, nested_message_ext,
      nested_enum_ext, unknownFields)

  public class Builder : Message.Builder<ExternalMessage, Builder>() {
    @JvmField
    public var f: Float? = null

    @JvmField
    public var fooext: List<Int> = emptyList()

    @JvmField
    public var barext: Int? = null

    @JvmField
    public var bazext: Int? = null

    @JvmField
    public var nested_message_ext: SimpleMessage.NestedMessage? = null

    @JvmField
    public var nested_enum_ext: SimpleMessage.NestedEnum? = null

    public fun f(f: Float?): Builder {
      this.f = f
      return this
    }

    public fun fooext(fooext: List<Int>): Builder {
      checkElementsNotNull(fooext)
      this.fooext = fooext
      return this
    }

    public fun barext(barext: Int?): Builder {
      this.barext = barext
      return this
    }

    public fun bazext(bazext: Int?): Builder {
      this.bazext = bazext
      return this
    }

    public fun nested_message_ext(nested_message_ext: SimpleMessage.NestedMessage?): Builder {
      this.nested_message_ext = nested_message_ext
      return this
    }

    public fun nested_enum_ext(nested_enum_ext: SimpleMessage.NestedEnum?): Builder {
      this.nested_enum_ext = nested_enum_ext
      return this
    }

    public override fun build(): ExternalMessage = ExternalMessage(
      f = f,
      fooext = fooext,
      barext = barext,
      bazext = bazext,
      nested_message_ext = nested_message_ext,
      nested_enum_ext = nested_enum_ext,
      unknownFields = buildUnknownFields()
    )
  }

  public companion object {
    public const val DEFAULT_F: Float = 20f

    @JvmField
    public val ADAPTER: ProtoAdapter<ExternalMessage> = object : ProtoAdapter<ExternalMessage>(
      FieldEncoding.LENGTH_DELIMITED, 
      ExternalMessage::class, 
      "type.googleapis.com/squareup.protos.kotlin.simple.ExternalMessage", 
      PROTO_2, 
      null
    ) {
      public override fun encodedSize(`value`: ExternalMessage): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.FLOAT.encodedSizeWithTag(1, value.f)
        size += ProtoAdapter.INT32.asRepeated().encodedSizeWithTag(125, value.fooext)
        size += ProtoAdapter.INT32.encodedSizeWithTag(126, value.barext)
        size += ProtoAdapter.INT32.encodedSizeWithTag(127, value.bazext)
        size += SimpleMessage.NestedMessage.ADAPTER.encodedSizeWithTag(128,
            value.nested_message_ext)
        size += SimpleMessage.NestedEnum.ADAPTER.encodedSizeWithTag(129, value.nested_enum_ext)
        return size
      }

      public override fun encode(writer: ProtoWriter, `value`: ExternalMessage): Unit {
        ProtoAdapter.FLOAT.encodeWithTag(writer, 1, value.f)
        ProtoAdapter.INT32.asRepeated().encodeWithTag(writer, 125, value.fooext)
        ProtoAdapter.INT32.encodeWithTag(writer, 126, value.barext)
        ProtoAdapter.INT32.encodeWithTag(writer, 127, value.bazext)
        SimpleMessage.NestedMessage.ADAPTER.encodeWithTag(writer, 128, value.nested_message_ext)
        SimpleMessage.NestedEnum.ADAPTER.encodeWithTag(writer, 129, value.nested_enum_ext)
        writer.writeBytes(value.unknownFields)
      }

      public override fun encode(writer: ReverseProtoWriter, `value`: ExternalMessage): Unit {
        writer.writeBytes(value.unknownFields)
        SimpleMessage.NestedEnum.ADAPTER.encodeWithTag(writer, 129, value.nested_enum_ext)
        SimpleMessage.NestedMessage.ADAPTER.encodeWithTag(writer, 128, value.nested_message_ext)
        ProtoAdapter.INT32.encodeWithTag(writer, 127, value.bazext)
        ProtoAdapter.INT32.encodeWithTag(writer, 126, value.barext)
        ProtoAdapter.INT32.asRepeated().encodeWithTag(writer, 125, value.fooext)
        ProtoAdapter.FLOAT.encodeWithTag(writer, 1, value.f)
      }

      public override fun decode(reader: ProtoReader): ExternalMessage {
        var f: Float? = null
        val fooext = mutableListOf<Int>()
        var barext: Int? = null
        var bazext: Int? = null
        var nested_message_ext: SimpleMessage.NestedMessage? = null
        var nested_enum_ext: SimpleMessage.NestedEnum? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> f = ProtoAdapter.FLOAT.decode(reader)
            125 -> fooext.add(ProtoAdapter.INT32.decode(reader))
            126 -> barext = ProtoAdapter.INT32.decode(reader)
            127 -> bazext = ProtoAdapter.INT32.decode(reader)
            128 -> nested_message_ext = SimpleMessage.NestedMessage.ADAPTER.decode(reader)
            129 -> try {
              nested_enum_ext = SimpleMessage.NestedEnum.ADAPTER.decode(reader)
            } catch (e: ProtoAdapter.EnumConstantNotFoundException) {
              reader.addUnknownField(tag, FieldEncoding.VARINT, e.value.toLong())
            }
            else -> reader.readUnknownField(tag)
          }
        }
        return ExternalMessage(
          f = f,
          fooext = fooext,
          barext = barext,
          bazext = bazext,
          nested_message_ext = nested_message_ext,
          nested_enum_ext = nested_enum_ext,
          unknownFields = unknownFields
        )
      }

      public override fun redact(`value`: ExternalMessage): ExternalMessage = value.copy(
        nested_message_ext =
            value.nested_message_ext?.let(SimpleMessage.NestedMessage.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}
