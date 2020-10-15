// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.redacted_test.RedactedExtension in redacted_test.proto
package com.squareup.wire.protos.kotlin.redacted

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

public class RedactedExtension(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    redacted = true
  )
  public val d: String? = null,
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public val e: String? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<RedactedExtension, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  public override fun newBuilder(): Nothing = throw AssertionError()

  public override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is RedactedExtension) return false
    if (unknownFields != other.unknownFields) return false
    if (d != other.d) return false
    if (e != other.e) return false
    return true
  }

  public override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + d.hashCode()
      result = result * 37 + e.hashCode()
      super.hashCode = result
    }
    return result
  }

  public override fun toString(): String {
    val result = mutableListOf<String>()
    if (d != null) result += """d=██"""
    if (e != null) result += """e=${sanitize(e)}"""
    return result.joinToString(prefix = "RedactedExtension{", separator = ", ", postfix = "}")
  }

  public fun copy(
    d: String? = this.d,
    e: String? = this.e,
    unknownFields: ByteString = this.unknownFields
  ): RedactedExtension = RedactedExtension(d, e, unknownFields)

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<RedactedExtension> = object : ProtoAdapter<RedactedExtension>(
      FieldEncoding.LENGTH_DELIMITED, 
      RedactedExtension::class, 
      "type.googleapis.com/squareup.protos.kotlin.redacted_test.RedactedExtension", 
      PROTO_2, 
      null
    ) {
      public override fun encodedSize(value: RedactedExtension): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.d)
        size += ProtoAdapter.STRING.encodedSizeWithTag(2, value.e)
        return size
      }

      public override fun encode(writer: ProtoWriter, value: RedactedExtension): Unit {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.d)
        ProtoAdapter.STRING.encodeWithTag(writer, 2, value.e)
        writer.writeBytes(value.unknownFields)
      }

      public override fun decode(reader: ProtoReader): RedactedExtension {
        var d: String? = null
        var e: String? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> d = ProtoAdapter.STRING.decode(reader)
            2 -> e = ProtoAdapter.STRING.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return RedactedExtension(
          d = d,
          e = e,
          unknownFields = unknownFields
        )
      }

      public override fun redact(value: RedactedExtension): RedactedExtension = value.copy(
        d = null,
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}
