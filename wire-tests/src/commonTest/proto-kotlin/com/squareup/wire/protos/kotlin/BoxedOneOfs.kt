// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.boxed.oneof.BoxedOneOfs in boxed_oneofs.proto
@file:Suppress(
  "DEPRECATION",
  "RUNTIME_ANNOTATION_NOT_SUPPORTED",
)

package com.squareup.wire.protos.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.OneOf
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.`internal`.JvmField
import com.squareup.wire.`internal`.JvmStatic
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Long
import kotlin.Nothing
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.Set
import okio.ByteString

public class BoxedOneOfs(
  public val OneOf: OneOf<OneOf_<*>, *>? = null,
  unknownFields: ByteString = ByteString.EMPTY,
) : Message<BoxedOneOfs, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN,
  )
  override fun newBuilder(): Nothing = throw AssertionError("Builders are deprecated and only available in a javaInterop build; see https://square.github.io/wire/wire_compiler/#kotlin")

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is BoxedOneOfs) return false
    if (unknownFields != other.unknownFields) return false
    if (OneOf != other.OneOf) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + (OneOf?.hashCode() ?: 0)
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (OneOf != null) result += """OneOf=$OneOf"""
    return result.joinToString(prefix = "BoxedOneOfs{", separator = ", ", postfix = "}")
  }

  public fun copy(OneOf: OneOf<OneOf_<*>, *>? = this.OneOf, unknownFields: ByteString = this.unknownFields): BoxedOneOfs = BoxedOneOfs(OneOf, unknownFields)

  public class OneOf_<T>(
    tag: Int,
    adapter: ProtoAdapter<T>,
    declaredName: String,
  ) : OneOf.Key<T>(tag, adapter, declaredName) {
    public fun create(`value`: T): OneOf<OneOf_<T>, T> = OneOf(this, value)

    public fun decode(reader: ProtoReader): OneOf<OneOf_<T>, T> = create(adapter.decode(reader))
  }

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<BoxedOneOfs> = object : ProtoAdapter<BoxedOneOfs>(
      FieldEncoding.LENGTH_DELIMITED, 
      BoxedOneOfs::class, 
      "type.googleapis.com/squareup.protos.kotlin.boxed.oneof.BoxedOneOfs", 
      PROTO_2, 
      null, 
      "boxed_oneofs.proto"
    ) {
      override fun encodedSize(`value`: BoxedOneOfs): Int {
        var size = value.unknownFields.size
        if (value.OneOf != null) size += value.OneOf.encodedSizeWithTag()
        return size
      }

      override fun encode(writer: ProtoWriter, `value`: BoxedOneOfs) {
        if (value.OneOf != null) value.OneOf.encodeWithTag(writer)
        writer.writeBytes(value.unknownFields)
      }

      override fun encode(writer: ReverseProtoWriter, `value`: BoxedOneOfs) {
        writer.writeBytes(value.unknownFields)
        if (value.OneOf != null) value.OneOf.encodeWithTag(writer)
      }

      override fun decode(reader: ProtoReader): BoxedOneOfs {
        var OneOf: OneOf<OneOf_<*>, *>? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            else -> {
              for (choiceKey in ONEOF_KEYS) {
                if (tag == choiceKey.tag) {
                  OneOf = choiceKey.decode(reader)
                  return@forEachTag Unit
                }
              }
              reader.readUnknownField(tag)
            }
          }
        }
        return BoxedOneOfs(
          OneOf = OneOf,
          unknownFields = unknownFields
        )
      }

      override fun redact(`value`: BoxedOneOfs): BoxedOneOfs = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }

    public val ONEOF_ARRAY: OneOf_<String> =
        OneOf_<String>(tag = 1, adapter = ProtoAdapter.STRING, declaredName = "array")

    public val ONEOF_BOOLEAN: OneOf_<String> =
        OneOf_<String>(tag = 2, adapter = ProtoAdapter.STRING, declaredName = "boolean")

    public val ONEOF_CLASS: OneOf_<String> =
        OneOf_<String>(tag = 3, adapter = ProtoAdapter.STRING, declaredName = "class")

    public val ONEOF_OPEN: OneOf_<String> =
        OneOf_<String>(tag = 4, adapter = ProtoAdapter.STRING, declaredName = "open")

    public val ONEOF_FUN: OneOf_<String> =
        OneOf_<String>(tag = 5, adapter = ProtoAdapter.STRING, declaredName = "fun")

    public val ONEOF_VAL: OneOf_<String> =
        OneOf_<String>(tag = 6, adapter = ProtoAdapter.STRING, declaredName = "val")

    public val ONEOF_PRIVATE: OneOf_<String> =
        OneOf_<String>(tag = 7, adapter = ProtoAdapter.STRING, declaredName = "private")

    public val ONEOF_RETURN: OneOf_<String> =
        OneOf_<String>(tag = 8, adapter = ProtoAdapter.STRING, declaredName = "return")

    public val ONEOF_RESULT: OneOf_<String> =
        OneOf_<String>(tag = 9, adapter = ProtoAdapter.STRING, declaredName = "result")

    public val ONEOF_PUBLIC: OneOf_<String> =
        OneOf_<String>(tag = 10, adapter = ProtoAdapter.STRING, declaredName = "public")

    @JvmStatic
    public val ONEOF_KEYS: Set<OneOf_<*>> =
        setOf(ONEOF_ARRAY, ONEOF_BOOLEAN, ONEOF_CLASS, ONEOF_OPEN, ONEOF_FUN, ONEOF_VAL, ONEOF_PRIVATE, ONEOF_RETURN, ONEOF_RESULT, ONEOF_PUBLIC)

    private const val serialVersionUID: Long = 0L
  }
}
