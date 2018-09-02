/*
 * Copyright 2018 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import okio.Buffer
import okio.ByteString

class UnknownFieldsBuilder {
  var byteString = ByteString.EMPTY
  var buffer: Buffer? = null
  var writer: ProtoWriter? = null

  init {
    prepareUnknownFields()
  }

  fun addUnknownField(
    tag: Int,
    fieldEncoding: FieldEncoding,
    value: Any
  ) {
    val protoAdapter = fieldEncoding.rawProtoAdapter() as ProtoAdapter<Any>
    protoAdapter.encodeWithTag(writer, tag, value)
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
      writer!!.writeBytes(byteString)
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

