// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.tostring.VeryLongProtoNameCausingBrokenLineBreaks in to_string.proto
package com.squareup.wire.protos.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.WireField
import com.squareup.wire.`internal`.sanitize
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Long
import kotlin.Nothing
import kotlin.String
import kotlin.Unit
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

/**
 * https://github.com/square/wire/issues/1125
 */
public class VeryLongProtoNameCausingBrokenLineBreaks(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public val foo: String? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<VeryLongProtoNameCausingBrokenLineBreaks, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  public override fun newBuilder(): Nothing = throw AssertionError()

  public override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is VeryLongProtoNameCausingBrokenLineBreaks) return false
    if (unknownFields != other.unknownFields) return false
    if (foo != other.foo) return false
    return true
  }

  public override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + foo.hashCode()
      super.hashCode = result
    }
    return result
  }

  public override fun toString(): String {
    val result = mutableListOf<String>()
    if (foo != null) result += """foo=${sanitize(foo)}"""
    return result.joinToString(prefix = "VeryLongProtoNameCausingBrokenLineBreaks{", separator =
        ", ", postfix = "}")
  }

  public fun copy(foo: String? = this.foo, unknownFields: ByteString = this.unknownFields):
      VeryLongProtoNameCausingBrokenLineBreaks = VeryLongProtoNameCausingBrokenLineBreaks(foo,
      unknownFields)

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<VeryLongProtoNameCausingBrokenLineBreaks> = object :
        ProtoAdapter<VeryLongProtoNameCausingBrokenLineBreaks>(
      FieldEncoding.LENGTH_DELIMITED, 
      VeryLongProtoNameCausingBrokenLineBreaks::class, 
      "type.googleapis.com/squareup.protos.tostring.VeryLongProtoNameCausingBrokenLineBreaks", 
      PROTO_2, 
      null
    ) {
      public override fun encodedSize(value: VeryLongProtoNameCausingBrokenLineBreaks): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.foo)
        return size
      }

      public override fun encode(writer: ProtoWriter,
          value: VeryLongProtoNameCausingBrokenLineBreaks): Unit {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.foo)
        writer.writeBytes(value.unknownFields)
      }

      public override fun decode(reader: ProtoReader): VeryLongProtoNameCausingBrokenLineBreaks {
        var foo: String? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> foo = ProtoAdapter.STRING.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return VeryLongProtoNameCausingBrokenLineBreaks(
          foo = foo,
          unknownFields = unknownFields
        )
      }

      public override fun redact(value: VeryLongProtoNameCausingBrokenLineBreaks):
          VeryLongProtoNameCausingBrokenLineBreaks = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}
