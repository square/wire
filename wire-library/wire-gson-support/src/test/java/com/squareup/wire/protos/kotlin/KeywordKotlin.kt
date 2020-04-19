// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: keyword.proto
package com.squareup.wire.protos.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.internal.sanitize
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

class KeywordKotlin(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    declaredName = "object"
  )
  @JvmField
  val object_: String? = null,
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#INT32",
    declaredName = "when"
  )
  @JvmField
  val when_: Int? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<KeywordKotlin, KeywordKotlin.Builder>(ADAPTER, unknownFields) {
  override fun newBuilder(): Builder {
    val builder = Builder()
    builder.object_ = object_
    builder.when_ = when_
    builder.addUnknownFields(unknownFields)
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is KeywordKotlin) return false
    return unknownFields == other.unknownFields
        && object_ == other.object_
        && when_ == other.when_
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + object_.hashCode()
      result = result * 37 + when_.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (object_ != null) result += """object_=${sanitize(object_)}"""
    if (when_ != null) result += """when_=$when_"""
    return result.joinToString(prefix = "KeywordKotlin{", separator = ", ", postfix = "}")
  }

  fun copy(
    object_: String? = this.object_,
    when_: Int? = this.when_,
    unknownFields: ByteString = this.unknownFields
  ): KeywordKotlin = KeywordKotlin(object_, when_, unknownFields)

  class Builder : Message.Builder<KeywordKotlin, Builder>() {
    @JvmField
    var object_: String? = null

    @JvmField
    var when_: Int? = null

    fun object_(object_: String?): Builder {
      this.object_ = object_
      return this
    }

    fun when_(when_: Int?): Builder {
      this.when_ = when_
      return this
    }

    override fun build(): KeywordKotlin = KeywordKotlin(
      object_ = object_,
      when_ = when_,
      unknownFields = buildUnknownFields()
    )
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<KeywordKotlin> = object : ProtoAdapter<KeywordKotlin>(
      FieldEncoding.LENGTH_DELIMITED, 
      KeywordKotlin::class, 
      "type.googleapis.com/com.squareup.wire.protos.kotlin.KeywordKotlin"
    ) {
      override fun encodedSize(value: KeywordKotlin): Int = 
        ProtoAdapter.STRING.encodedSizeWithTag(1, value.object_) +
        ProtoAdapter.INT32.encodedSizeWithTag(2, value.when_) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: KeywordKotlin) {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.object_)
        ProtoAdapter.INT32.encodeWithTag(writer, 2, value.when_)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): KeywordKotlin {
        var object_: String? = null
        var when_: Int? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> object_ = ProtoAdapter.STRING.decode(reader)
            2 -> when_ = ProtoAdapter.INT32.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return KeywordKotlin(
          object_ = object_,
          when_ = when_,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: KeywordKotlin): KeywordKotlin = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
