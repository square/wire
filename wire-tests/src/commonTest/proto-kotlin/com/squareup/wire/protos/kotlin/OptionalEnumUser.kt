// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.OptionalEnumUser in optional_enum.proto
@file:Suppress(
  "DEPRECATION",
  "RUNTIME_ANNOTATION_NOT_SUPPORTED",
)

package com.squareup.wire.protos.kotlin

import com.squareup.wire.EnumAdapter
import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.WireEnum
import com.squareup.wire.WireField
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
import okio.ByteString

public class OptionalEnumUser(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.protos.kotlin.OptionalEnumUser${'$'}OptionalEnum#ADAPTER",
    schemaIndex = 0,
  )
  public val optional_enum: OptionalEnum? = null,
  unknownFields: ByteString = ByteString.EMPTY,
) : Message<OptionalEnumUser, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN,
  )
  override fun newBuilder(): Nothing = throw
      AssertionError("Builders are deprecated and only available in a javaInterop build; see https://square.github.io/wire/wire_compiler/#kotlin")

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is OptionalEnumUser) return false
    if (unknownFields != other.unknownFields) return false
    if (optional_enum != other.optional_enum) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + (optional_enum?.hashCode() ?: 0)
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (optional_enum != null) result += """optional_enum=$optional_enum"""
    return result.joinToString(prefix = "OptionalEnumUser{", separator = ", ", postfix = "}")
  }

  public fun copy(optional_enum: OptionalEnum? = this.optional_enum, unknownFields: ByteString =
      this.unknownFields): OptionalEnumUser = OptionalEnumUser(optional_enum, unknownFields)

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<OptionalEnumUser> = object : ProtoAdapter<OptionalEnumUser>(
      FieldEncoding.LENGTH_DELIMITED, 
      OptionalEnumUser::class, 
      "type.googleapis.com/squareup.protos.kotlin.OptionalEnumUser", 
      PROTO_2, 
      null, 
      "optional_enum.proto"
    ) {
      override fun encodedSize(`value`: OptionalEnumUser): Int {
        var size = value.unknownFields.size
        size += OptionalEnum.ADAPTER.encodedSizeWithTag(1, value.optional_enum)
        return size
      }

      override fun encode(writer: ProtoWriter, `value`: OptionalEnumUser) {
        OptionalEnum.ADAPTER.encodeWithTag(writer, 1, value.optional_enum)
        writer.writeBytes(value.unknownFields)
      }

      override fun encode(writer: ReverseProtoWriter, `value`: OptionalEnumUser) {
        writer.writeBytes(value.unknownFields)
        OptionalEnum.ADAPTER.encodeWithTag(writer, 1, value.optional_enum)
      }

      override fun decode(reader: ProtoReader): OptionalEnumUser {
        var optional_enum: OptionalEnum? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> try {
              optional_enum = OptionalEnum.ADAPTER.decode(reader)
            } catch (e: ProtoAdapter.EnumConstantNotFoundException) {
              reader.addUnknownField(tag, FieldEncoding.VARINT, e.value.toLong())
            }
            else -> reader.readUnknownField(tag)
          }
        }
        return OptionalEnumUser(
          optional_enum = optional_enum,
          unknownFields = unknownFields
        )
      }

      override fun redact(`value`: OptionalEnumUser): OptionalEnumUser = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }

  public enum class OptionalEnum(
    override val `value`: Int,
  ) : WireEnum {
    FOO(1),
    BAR(2),
    ;

    public companion object {
      @JvmField
      public val ADAPTER: ProtoAdapter<OptionalEnum> = object : EnumAdapter<OptionalEnum>(
        OptionalEnum::class, 
        PROTO_2, 
        null
      ) {
        override fun fromValue(`value`: Int): OptionalEnum? = OptionalEnum.fromValue(`value`)
      }

      @JvmStatic
      public fun fromValue(`value`: Int): OptionalEnum? = when (`value`) {
        1 -> FOO
        2 -> BAR
        else -> null
      }
    }
  }
}
