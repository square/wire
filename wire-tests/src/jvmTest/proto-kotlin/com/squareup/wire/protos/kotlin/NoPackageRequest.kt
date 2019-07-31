// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: service_without_package.proto
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
import kotlin.jvm.JvmField
import okio.ByteString

data class NoPackageRequest(
  val unknownFields: ByteString = ByteString.EMPTY
) : Message<NoPackageRequest, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing {
    throw AssertionError()
  }

  override fun equals(other: Any?): Boolean = other is NoPackageRequest

  override fun hashCode(): Int = unknownFields.hashCode()

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<NoPackageRequest> = object : ProtoAdapter<NoPackageRequest>(
      FieldEncoding.LENGTH_DELIMITED, 
      NoPackageRequest::class
    ) {
      override fun encodedSize(value: NoPackageRequest): Int = 
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: NoPackageRequest) {
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): NoPackageRequest {
        val unknownFields = reader.forEachTag(reader::readUnknownField)
        return NoPackageRequest(
          unknownFields = unknownFields
        )
      }

      override fun redact(value: NoPackageRequest): NoPackageRequest = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
