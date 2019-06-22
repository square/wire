// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: redacted_test.proto
package com.squareup.wire.protos.kotlin.redacted

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import kotlin.AssertionError
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.jvm.JvmField
import okio.ByteString

data class Redacted(
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
  @field:WireField(
    tag = 10,
    adapter = "com.squareup.wire.protos.kotlin.redacted.RedactedExtension#ADAPTER"
  )
  val extension: RedactedExtension? = null,
  val unknownFields: ByteString = ByteString.EMPTY
) : Message<Redacted, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing {
    throw AssertionError()
  }

  override fun toString(): String = buildString {
    append("Redacted(")
    append("""a=██""")
    append(""", b=$b""")
    append(""", c=$c""")
    append(""", extension=$extension""")
    append(")")
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<Redacted> = object : ProtoAdapter<Redacted>(
      FieldEncoding.LENGTH_DELIMITED, 
      Redacted::class
    ) {
      override fun encodedSize(value: Redacted): Int = 
        ProtoAdapter.STRING.encodedSizeWithTag(1, value.a) +
        ProtoAdapter.STRING.encodedSizeWithTag(2, value.b) +
        ProtoAdapter.STRING.encodedSizeWithTag(3, value.c) +
        RedactedExtension.ADAPTER.encodedSizeWithTag(10, value.extension) +
        value.unknownFields.size

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
