// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: redacted_test.proto
package com.squareup.wire.protos.kotlin.redacted

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.internal.sanitize
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

class Redacted(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    redacted = true
  )
  val a: String? = null,
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  val b: String? = null,
  @field:WireField(
    tag = 3,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  val c: String? = null,
  /**
   * Extension source: redacted_test.proto
   */
  @field:WireField(
    tag = 10,
    adapter = "com.squareup.wire.protos.kotlin.redacted.RedactedExtension#ADAPTER"
  )
  val extension: RedactedExtension? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<Redacted, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing = throw AssertionError()

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is Redacted) return false
    var result = unknownFields == other.unknownFields
    result = result && (a == other.a)
    result = result && (b == other.b)
    result = result && (c == other.c)
    result = result && (extension == other.extension)
    return result
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + a.hashCode()
      result = result * 37 + b.hashCode()
      result = result * 37 + c.hashCode()
      result = result * 37 + extension.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (a != null) result += """a=██"""
    if (b != null) result += """b=${sanitize(b)}"""
    if (c != null) result += """c=${sanitize(c)}"""
    if (extension != null) result += """extension=$extension"""
    return result.joinToString(prefix = "Redacted{", separator = ", ", postfix = "}")
  }

  fun copy(
    a: String? = this.a,
    b: String? = this.b,
    c: String? = this.c,
    extension: RedactedExtension? = this.extension,
    unknownFields: ByteString = this.unknownFields
  ): Redacted = Redacted(a, b, c, extension, unknownFields)

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<Redacted> = object : ProtoAdapter<Redacted>(
      FieldEncoding.LENGTH_DELIMITED, 
      Redacted::class, 
      "type.googleapis.com/squareup.protos.kotlin.redacted_test.Redacted"
    ) {
      override fun encodedSize(value: Redacted): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.a)
        size += ProtoAdapter.STRING.encodedSizeWithTag(2, value.b)
        size += ProtoAdapter.STRING.encodedSizeWithTag(3, value.c)
        size += RedactedExtension.ADAPTER.encodedSizeWithTag(10, value.extension)
        return size
      }

      override fun encode(writer: ProtoWriter, value: Redacted) {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.a)
        ProtoAdapter.STRING.encodeWithTag(writer, 2, value.b)
        ProtoAdapter.STRING.encodeWithTag(writer, 3, value.c)
        RedactedExtension.ADAPTER.encodeWithTag(writer, 10, value.extension)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): Redacted {
        var a: String? = null
        var b: String? = null
        var c: String? = null
        var extension: RedactedExtension? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> a = ProtoAdapter.STRING.decode(reader)
            2 -> b = ProtoAdapter.STRING.decode(reader)
            3 -> c = ProtoAdapter.STRING.decode(reader)
            10 -> extension = RedactedExtension.ADAPTER.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return Redacted(
          a = a,
          b = b,
          c = c,
          extension = extension,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: Redacted): Redacted = value.copy(
        a = null,
        extension = value.extension?.let(RedactedExtension.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
