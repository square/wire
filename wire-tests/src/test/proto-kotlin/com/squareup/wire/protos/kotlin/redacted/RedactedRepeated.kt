// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: redacted_test.proto
package com.squareup.wire.protos.kotlin.redacted

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.TagHandler
import com.squareup.wire.WireField
import com.squareup.wire.internal.redactElements
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlin.jvm.JvmField
import okio.ByteString

data class RedactedRepeated(
  @field:WireField(tag = 1, adapter = "com.squareup.wire.ProtoAdapter#STRING", redacted = true)
      val a: List<String> = emptyList(),
  /**
   * Values in the repeated type need redacting.
   */
  @field:WireField(tag = 2, adapter = "com.squareup.wire.protos.kotlin.redacted.Redacted#ADAPTER")
      val b: List<Redacted> = emptyList(),
  val unknownFields: ByteString = ByteString.EMPTY
) : Message<RedactedRepeated, RedactedRepeated.Builder>(ADAPTER, unknownFields) {
  @Deprecated(
      message = "Shouldn't be used in Kotlin",
      level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Builder = Builder(this.copy())

  override fun toString(): String = buildString {
    append("RedactedRepeated(")
    append("""a=██""")
    append(""", b=$b""")
    append(")")
  }

  class Builder(private val message: RedactedRepeated) : Message.Builder<RedactedRepeated,
      Builder>() {
    override fun build(): RedactedRepeated = message
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<RedactedRepeated> = object : ProtoAdapter<RedactedRepeated>(
      FieldEncoding.LENGTH_DELIMITED, 
      RedactedRepeated::class
    ) {
      override fun encodedSize(value: RedactedRepeated): Int = 
        ProtoAdapter.STRING.asRepeated().encodedSizeWithTag(1, value.a) +
        com.squareup.wire.protos.kotlin.redacted.Redacted.ADAPTER.asRepeated().encodedSizeWithTag(2,
            value.b) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: RedactedRepeated) {
        ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 1, value.a)
        com.squareup.wire.protos.kotlin.redacted.Redacted.ADAPTER.asRepeated().encodeWithTag(writer,
            2, value.b)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): RedactedRepeated {
        val a = mutableListOf<String>()
        val b = mutableListOf<Redacted>()
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> a.add(ProtoAdapter.STRING.decode(reader))
            2 -> b.add(com.squareup.wire.protos.kotlin.redacted.Redacted.ADAPTER.decode(reader))
            else -> TagHandler.UNKNOWN_TAG
          }
        }
        return RedactedRepeated(
          a = a,
          b = b,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: RedactedRepeated): RedactedRepeated = value.copy(
        a = emptyList(),
        b = value.b.redactElements(com.squareup.wire.protos.kotlin.redacted.Redacted.ADAPTER),
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
