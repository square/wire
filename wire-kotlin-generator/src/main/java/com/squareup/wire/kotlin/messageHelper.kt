package com.squareup.wire.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import okio.Buffer
import okio.ByteString
import java.io.IOException

class UnkownFieldsBuilder {
  var unknownFieldsByteString = ByteString.EMPTY
  var unknownFieldsBuffer: Buffer? = null
  var unknownFieldsWriter: ProtoWriter? = null

  fun addUnknownField(
      tag: Int,
      fieldEncoding: FieldEncoding,
      value: Any
  ) {

    try {
      val protoAdapter = fieldEncoding.rawProtoAdapter() as ProtoAdapter<Any>
      protoAdapter.encodeWithTag(unknownFieldsWriter, tag, value)
    } catch (_: IOException) {
      throw AssertionError()
    }
  }

  fun buildUnknownFields(): ByteString {
    if (unknownFieldsBuffer != null) {
      // Reads and caches the unknown fields from the buffer.
      unknownFieldsByteString = unknownFieldsBuffer!!.readByteString()
      unknownFieldsBuffer = null
      unknownFieldsWriter = null
    }
    return unknownFieldsByteString
  }

  fun prepareUnkownFields() {
    if (unknownFieldsBuffer == null) {
      unknownFieldsBuffer = Buffer()
      unknownFieldsWriter = ProtoWriter(unknownFieldsBuffer!!)
      try {
        // Writes the cached unknown fields to the buffer.
        unknownFieldsWriter!!.writeBytes(unknownFieldsByteString)
      } catch (_: IOException) {
        throw AssertionError()
      }
      unknownFieldsByteString = ByteString.EMPTY
    }
  }

  companion object {
    fun create() : UnkownFieldsBuilder {
      val unkownFieldsBuilder =  UnkownFieldsBuilder()
      unkownFieldsBuilder.prepareUnkownFields()
      return unkownFieldsBuilder
    }

    val UNKNOWN_FIELD = Any()
  }

}

fun ProtoReader.decodeMessage(tagHandler: (Int) -> Any): ByteString {
  val unknownFieldsBuilder = UnkownFieldsBuilder.create()

  val token = beginMessage()
  while (true) {
    val tag = nextTag()
    if (tag == -1) break
    if (tagHandler(tag) == UnkownFieldsBuilder.UNKNOWN_FIELD) {
      println("We are receiving an unkown field")
      val fieldEncoding = peekFieldEncoding()
      val value = fieldEncoding.rawProtoAdapter().decode(this)
      unknownFieldsBuilder.addUnknownField(tag, fieldEncoding, value)
    }
  }
  endMessage(token)
  return unknownFieldsBuilder.buildUnknownFields()
}

