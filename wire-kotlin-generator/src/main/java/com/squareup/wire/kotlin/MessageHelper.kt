package com.squareup.wire.kotlin

import com.squareup.wire.ProtoReader
import okio.ByteString

fun ProtoReader.decodeMessage(tagHandler: (Int) -> Any): ByteString {
  // var unknownFieldsBuilder = Dino.Builder()

  val token = beginMessage()
  while (true) {
    val tag = nextTag()
    if (tag == -1) break
    if (tagHandler(tag) == UNKNOWN_FIELD) {
      println("We are receiving an unkown field")
      val fieldEncoding = peekFieldEncoding()
      val value = fieldEncoding.rawProtoAdapter().decode(this)
      //  unknownFieldsBuilder.addUnknownField(tag, fieldEncoding, value)
    }
  }
  endMessage(token)
  return ByteString.EMPTY
}

object UNKNOWN_FIELD