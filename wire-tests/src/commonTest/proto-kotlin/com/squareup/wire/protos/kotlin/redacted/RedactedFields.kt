// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.redacted_test.RedactedFields in redacted_test.proto
@file:Suppress(
  "DEPRECATION",
  "RUNTIME_ANNOTATION_NOT_SUPPORTED",
)

package com.squareup.wire.protos.kotlin.redacted

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.WireField
import com.squareup.wire.`internal`.JvmField
import com.squareup.wire.`internal`.sanitize
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

public class RedactedFields(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    redacted = true,
    schemaIndex = 0,
  )
  public val a: String? = null,
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    schemaIndex = 1,
  )
  public val b: String? = null,
  @field:WireField(
    tag = 3,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    schemaIndex = 2,
  )
  public val c: String? = null,
  /**
   * Extension source: redacted_test.proto
   */
  @field:WireField(
    tag = 10,
    adapter = "com.squareup.wire.protos.kotlin.redacted.RedactedExtension#ADAPTER",
    schemaIndex = 3,
  )
  public val extension: RedactedExtension? = null,
  unknownFields: ByteString = ByteString.EMPTY,
) : Message<RedactedFields, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN,
  )
  override fun newBuilder(): Nothing = throw AssertionError("Builders are deprecated and only available in a javaInterop build; see https://square.github.io/wire/wire_compiler/#kotlin")

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is RedactedFields) return false
    if (unknownFields != other.unknownFields) return false
    if (a != other.a) return false
    if (b != other.b) return false
    if (c != other.c) return false
    if (extension != other.extension) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + (a?.hashCode() ?: 0)
      result = result * 37 + (b?.hashCode() ?: 0)
      result = result * 37 + (c?.hashCode() ?: 0)
      result = result * 37 + (extension?.hashCode() ?: 0)
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (a != null) result += """a=██"""
    if (b != null) result += """b=${sanitize(b)}"""
    if (c != null) result += """c=${sanitize(c)}"""
    if (extension != null) result += """extension=$extension"""
    return result.joinToString(prefix = "RedactedFields{", separator = ", ", postfix = "}")
  }

  public fun copy(
    a: String? = this.a,
    b: String? = this.b,
    c: String? = this.c,
    extension: RedactedExtension? = this.extension,
    unknownFields: ByteString = this.unknownFields,
  ): RedactedFields = RedactedFields(a, b, c, extension, unknownFields)

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<RedactedFields> = object : ProtoAdapter<RedactedFields>(
      FieldEncoding.LENGTH_DELIMITED, 
      RedactedFields::class, 
      "type.googleapis.com/squareup.protos.kotlin.redacted_test.RedactedFields", 
      PROTO_2, 
      null, 
      "redacted_test.proto"
    ) {
      override fun encodedSize(`value`: RedactedFields): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.a)
        size += ProtoAdapter.STRING.encodedSizeWithTag(2, value.b)
        size += ProtoAdapter.STRING.encodedSizeWithTag(3, value.c)
        size += RedactedExtension.ADAPTER.encodedSizeWithTag(10, value.extension)
        return size
      }

      override fun encode(writer: ProtoWriter, `value`: RedactedFields) {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.a)
        ProtoAdapter.STRING.encodeWithTag(writer, 2, value.b)
        ProtoAdapter.STRING.encodeWithTag(writer, 3, value.c)
        RedactedExtension.ADAPTER.encodeWithTag(writer, 10, value.extension)
        writer.writeBytes(value.unknownFields)
      }

      override fun encode(writer: ReverseProtoWriter, `value`: RedactedFields) {
        writer.writeBytes(value.unknownFields)
        RedactedExtension.ADAPTER.encodeWithTag(writer, 10, value.extension)
        ProtoAdapter.STRING.encodeWithTag(writer, 3, value.c)
        ProtoAdapter.STRING.encodeWithTag(writer, 2, value.b)
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.a)
      }

      override fun decode(reader: ProtoReader): RedactedFields {
        var a: String? = null
        var b: String? = null
        var c: String? = null
        var extension: RedactedExtension? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> a = ProtoAdapter.STRING.decode(reader)
            2 -> b = ProtoAdapter.STRING.decode(reader)
            3 -> c = ProtoAdapter.STRING.decode(reader)
            10 -> extension = RedactedExtension.ADAPTER.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return RedactedFields(
          a = a,
          b = b,
          c = c,
          extension = extension,
          unknownFields = unknownFields
        )
      }

      override fun redact(`value`: RedactedFields): RedactedFields = value.copy(
        a = null,
        extension = value.extension?.let(RedactedExtension.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}
