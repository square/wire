package com.squareup.wire.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import okio.Buffer
import okio.ByteString
import java.io.IOException

class UnknownFieldsBuilder {
  var byteString = ByteString.EMPTY
  var buffer: Buffer? = null
  var writer: ProtoWriter? = null

  constructor() {
    prepareUnknownFields()
  }

  fun addUnknownField(
      tag: Int,
      fieldEncoding: FieldEncoding,
      value: Any
  ) {
    try {
      val protoAdapter = fieldEncoding.rawProtoAdapter() as ProtoAdapter<Any>
      protoAdapter.encodeWithTag(writer, tag, value)
    } catch (_: IOException) {
      throw AssertionError()
    }
  }

  fun buildUnknownFields(): ByteString {
    if (buffer != null) {
      byteString = buffer!!.readByteString()
      buffer = null
      writer = null
    }
    return byteString
  }

  private fun prepareUnknownFields() {
    if (buffer == null) {
      buffer = Buffer()
      writer = ProtoWriter(buffer!!)
      try {
        writer!!.writeBytes(byteString)
      } catch (_: IOException) {
        // Impossible; we’re not doing I/O.
        throw AssertionError()
      }
      byteString = ByteString.EMPTY
    }
  }

  companion object {
    val UNKNOWN_FIELD = Any()
  }

}

fun ProtoReader.decodeMessage(tagHandler: (Int) -> Any): ByteString {
  val unknownFieldsBuilder = UnknownFieldsBuilder()

  val token = beginMessage()
  while (true) {
    val tag = nextTag()
    if (tag == -1) break
    if (tagHandler(tag) == UnknownFieldsBuilder.UNKNOWN_FIELD) {
      val fieldEncoding = peekFieldEncoding()
      val value = fieldEncoding.rawProtoAdapter().decode(this)
      unknownFieldsBuilder.addUnknownField(tag, fieldEncoding, value)
    }
  }
  endMessage(token)
  return unknownFieldsBuilder.buildUnknownFields()
}

