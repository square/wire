// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: service_kotlin.proto
package com.squareup.wire.protos.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.jvm.JvmField
import okio.ByteString

data class SomeResponse(
  val unknownFields: ByteString = ByteString.EMPTY
) : Message<SomeResponse, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing {
    throw AssertionError()
  }

  override fun equals(other: Any?): Boolean = other is SomeResponse

  override fun hashCode(): Int = unknownFields.hashCode()

  override fun toString(): String = "SomeResponse{}"

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<SomeResponse> = object : ProtoAdapter<SomeResponse>(
      FieldEncoding.LENGTH_DELIMITED, 
      SomeResponse::class
    ) {
      override fun encodedSize(value: SomeResponse): Int = 
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: SomeResponse) {
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): SomeResponse {
        val unknownFields = reader.forEachTag(reader::readUnknownField)
        return SomeResponse(
          unknownFields = unknownFields
        )
      }

      override fun redact(value: SomeResponse): SomeResponse = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
