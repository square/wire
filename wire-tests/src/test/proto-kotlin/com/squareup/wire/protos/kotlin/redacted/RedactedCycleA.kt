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
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.jvm.JvmField
import okio.ByteString

data class RedactedCycleA(@field:WireField(tag = 1, adapter =
    "com.squareup.wire.protos.kotlin.redacted.RedactedCycleB#ADAPTER") val b: RedactedCycleB? =
    null, val unknownFields: ByteString = ByteString.EMPTY) : Message<RedactedCycleA,
    RedactedCycleA.Builder>(ADAPTER, unknownFields) {
  @Deprecated(
      message = "Shouldn't be used in Kotlin",
      level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Builder = Builder(this.copy())

  class Builder(private val message: RedactedCycleA) : Message.Builder<RedactedCycleA, Builder>() {
    override fun build(): RedactedCycleA = message
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<RedactedCycleA> = object : ProtoAdapter<RedactedCycleA>(
      FieldEncoding.LENGTH_DELIMITED, 
      RedactedCycleA::class
    ) {
      override fun encodedSize(value: RedactedCycleA): Int = 
        com.squareup.wire.protos.kotlin.redacted.RedactedCycleB.ADAPTER.encodedSizeWithTag(1,
            value.b) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: RedactedCycleA) {
        com.squareup.wire.protos.kotlin.redacted.RedactedCycleB.ADAPTER.encodeWithTag(writer, 1,
            value.b)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): RedactedCycleA {
        var b: RedactedCycleB? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> b = com.squareup.wire.protos.kotlin.redacted.RedactedCycleB.ADAPTER.decode(reader)
            else -> TagHandler.UNKNOWN_TAG
          }
        }
        return RedactedCycleA(
          b = b,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: RedactedCycleA): RedactedCycleA = value.copy(
        b = value.b?.let(com.squareup.wire.protos.kotlin.redacted.RedactedCycleB.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
