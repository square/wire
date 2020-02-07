// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: deprecated.proto
package com.squareup.wire.protos.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

class DeprecatedProto(
  @Deprecated(message = "foo is deprecated")
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  val foo: String? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<DeprecatedProto, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing = throw AssertionError()

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is DeprecatedProto) return false
    return unknownFields == other.unknownFields
        && foo == other.foo
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + foo.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (foo != null) result += """foo=$foo"""
    return result.joinToString(prefix = "DeprecatedProto{", separator = ", ", postfix = "}")
  }

  fun copy(foo: String? = this.foo, unknownFields: ByteString = this.unknownFields): DeprecatedProto
      = DeprecatedProto(foo, unknownFields)

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<DeprecatedProto> = object : ProtoAdapter<DeprecatedProto>(
      FieldEncoding.LENGTH_DELIMITED, 
      DeprecatedProto::class, 
      "type.googleapis.com/squareup.protos.kotlin.DeprecatedProto"
    ) {
      override fun encodedSize(value: DeprecatedProto): Int = 
        ProtoAdapter.STRING.encodedSizeWithTag(1, value.foo) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: DeprecatedProto) {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.foo)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): DeprecatedProto {
        var foo: String? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> foo = ProtoAdapter.STRING.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return DeprecatedProto(
          foo = foo,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: DeprecatedProto): DeprecatedProto = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
