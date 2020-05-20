// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: external_message.proto
package com.squareup.wire.protos.kotlin.simple

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Float
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.collections.List
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

class ExternalMessage(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#FLOAT"
  )
  val f: Float? = null,
  /**
   * Extension source: simple_message.proto
   */
  @field:WireField(
    tag = 125,
    adapter = "com.squareup.wire.ProtoAdapter#INT32",
    label = WireField.Label.REPEATED,
    encodeMode = WireField.EncodeMode.REPEATED
  )
  val fooext: List<Int> = emptyList(),
  /**
   * Extension source: simple_message.proto
   */
  @field:WireField(
    tag = 126,
    adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  val barext: Int? = null,
  /**
   * Extension source: simple_message.proto
   */
  @field:WireField(
    tag = 127,
    adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  val bazext: Int? = null,
  /**
   * Extension source: simple_message.proto
   */
  @field:WireField(
    tag = 128,
    adapter = "com.squareup.wire.protos.kotlin.simple.SimpleMessage${'$'}NestedMessage#ADAPTER"
  )
  val nested_message_ext: SimpleMessage.NestedMessage? = null,
  /**
   * Extension source: simple_message.proto
   */
  @field:WireField(
    tag = 129,
    adapter = "com.squareup.wire.protos.kotlin.simple.SimpleMessage${'$'}NestedEnum#ADAPTER"
  )
  val nested_enum_ext: SimpleMessage.NestedEnum? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<ExternalMessage, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing = throw AssertionError()

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is ExternalMessage) return false
    return unknownFields == other.unknownFields
        && f == other.f
        && fooext == other.fooext
        && barext == other.barext
        && bazext == other.bazext
        && nested_message_ext == other.nested_message_ext
        && nested_enum_ext == other.nested_enum_ext
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + f.hashCode()
      result = result * 37 + fooext.hashCode()
      result = result * 37 + barext.hashCode()
      result = result * 37 + bazext.hashCode()
      result = result * 37 + nested_message_ext.hashCode()
      result = result * 37 + nested_enum_ext.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (f != null) result += """f=$f"""
    if (fooext.isNotEmpty()) result += """fooext=$fooext"""
    if (barext != null) result += """barext=$barext"""
    if (bazext != null) result += """bazext=$bazext"""
    if (nested_message_ext != null) result += """nested_message_ext=$nested_message_ext"""
    if (nested_enum_ext != null) result += """nested_enum_ext=$nested_enum_ext"""
    return result.joinToString(prefix = "ExternalMessage{", separator = ", ", postfix = "}")
  }

  fun copy(
    f: Float? = this.f,
    fooext: List<Int> = this.fooext,
    barext: Int? = this.barext,
    bazext: Int? = this.bazext,
    nested_message_ext: SimpleMessage.NestedMessage? = this.nested_message_ext,
    nested_enum_ext: SimpleMessage.NestedEnum? = this.nested_enum_ext,
    unknownFields: ByteString = this.unknownFields
  ): ExternalMessage = ExternalMessage(f, fooext, barext, bazext, nested_message_ext,
      nested_enum_ext, unknownFields)

  companion object {
    const val DEFAULT_F: Float = 20f

    @JvmField
    val ADAPTER: ProtoAdapter<ExternalMessage> = object : ProtoAdapter<ExternalMessage>(
      FieldEncoding.LENGTH_DELIMITED, 
      ExternalMessage::class, 
      "type.googleapis.com/squareup.protos.kotlin.simple.ExternalMessage"
    ) {
      override fun encodedSize(value: ExternalMessage): Int = 
        ProtoAdapter.FLOAT.encodedSizeWithTag(1, value.f) +
        ProtoAdapter.INT32.asRepeated().encodedSizeWithTag(125, value.fooext) +
        ProtoAdapter.INT32.encodedSizeWithTag(126, value.barext) +
        ProtoAdapter.INT32.encodedSizeWithTag(127, value.bazext) +
        SimpleMessage.NestedMessage.ADAPTER.encodedSizeWithTag(128, value.nested_message_ext) +
        SimpleMessage.NestedEnum.ADAPTER.encodedSizeWithTag(129, value.nested_enum_ext) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: ExternalMessage) {
        ProtoAdapter.FLOAT.encodeWithTag(writer, 1, value.f)
        ProtoAdapter.INT32.asRepeated().encodeWithTag(writer, 125, value.fooext)
        ProtoAdapter.INT32.encodeWithTag(writer, 126, value.barext)
        ProtoAdapter.INT32.encodeWithTag(writer, 127, value.bazext)
        SimpleMessage.NestedMessage.ADAPTER.encodeWithTag(writer, 128, value.nested_message_ext)
        SimpleMessage.NestedEnum.ADAPTER.encodeWithTag(writer, 129, value.nested_enum_ext)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): ExternalMessage {
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

      override fun redact(value: ExternalMessage): ExternalMessage = value.copy(
        nested_message_ext =
            value.nested_message_ext?.let(SimpleMessage.NestedMessage.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
