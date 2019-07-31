// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: redacted_test.proto
package com.squareup.wire.protos.kotlin.redacted

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
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

data class RedactedExtension(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    redacted = true
  )
  val d: String? = null,
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  val e: String? = null,
  val unknownFields: ByteString = ByteString.EMPTY
) : Message<RedactedExtension, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing {
    throw AssertionError()
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is RedactedExtension) return false
    return unknownFields == other.unknownFields
        && d == other.d
        && e == other.e
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = result * 37 + d.hashCode()
      result = result * 37 + e.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (d != null) result += """d=██"""
    if (e != null) result += """e=$e"""
    return result.joinToString(prefix = "RedactedExtension{", postfix = "}")
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<RedactedExtension> = object : ProtoAdapter<RedactedExtension>(
      FieldEncoding.LENGTH_DELIMITED, 
      RedactedExtension::class
    ) {
      override fun encodedSize(value: RedactedExtension): Int = 
        ProtoAdapter.STRING.encodedSizeWithTag(1, value.d) +
        ProtoAdapter.STRING.encodedSizeWithTag(2, value.e) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: RedactedExtension) {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.d)
        ProtoAdapter.STRING.encodeWithTag(writer, 2, value.e)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): RedactedExtension {
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

      override fun redact(value: RedactedExtension): RedactedExtension = value.copy(
        d = null,
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
