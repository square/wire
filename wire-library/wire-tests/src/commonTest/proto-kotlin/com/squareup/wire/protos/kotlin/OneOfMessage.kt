// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.oneof.OneOfMessage in one_of.proto
package com.squareup.wire.protos.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.WireField
import com.squareup.wire.`internal`.countNonNull
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
import kotlin.Unit
import kotlin.jvm.JvmField
import okio.ByteString

/**
 * It's a one of message.
 */
public class OneOfMessage(
  /**
   * What foo.
   */
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#INT32",
    oneofName = "choice"
  )
  public val foo: Int? = null,
  /**
   * Such bar.
   */
  @field:WireField(
    tag = 3,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    oneofName = "choice"
  )
  public val bar: String? = null,
  /**
   * Nice baz.
   */
  @field:WireField(
    tag = 4,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    oneofName = "choice"
  )
  public val baz: String? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<OneOfMessage, Nothing>(ADAPTER, unknownFields) {
  init {
    require(countNonNull(foo, bar, baz) <= 1) {
      "At most one of foo, bar, baz may be non-null"
    }
  }

  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  public override fun newBuilder(): Nothing = throw
      AssertionError("Builders are deprecated and only available in a javaInterop build; see https://square.github.io/wire/wire_compiler/#kotlin")

  public override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is OneOfMessage) return false
    if (unknownFields != other.unknownFields) return false
    if (foo != other.foo) return false
    if (bar != other.bar) return false
    if (baz != other.baz) return false
    return true
  }

  public override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + (foo?.hashCode() ?: 0)
      result = result * 37 + (bar?.hashCode() ?: 0)
      result = result * 37 + (baz?.hashCode() ?: 0)
      super.hashCode = result
    }
    return result
  }

  public override fun toString(): String {
    val result = mutableListOf<String>()
    if (foo != null) result += """foo=$foo"""
    if (bar != null) result += """bar=${sanitize(bar)}"""
    if (baz != null) result += """baz=${sanitize(baz)}"""
    return result.joinToString(prefix = "OneOfMessage{", separator = ", ", postfix = "}")
  }

  public fun copy(
    foo: Int? = this.foo,
    bar: String? = this.bar,
    baz: String? = this.baz,
    unknownFields: ByteString = this.unknownFields
  ): OneOfMessage = OneOfMessage(foo, bar, baz, unknownFields)

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<OneOfMessage> = object : ProtoAdapter<OneOfMessage>(
      FieldEncoding.LENGTH_DELIMITED, 
      OneOfMessage::class, 
      "type.googleapis.com/squareup.protos.kotlin.oneof.OneOfMessage", 
      PROTO_2, 
      null
    ) {
      public override fun encodedSize(`value`: OneOfMessage): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.INT32.encodedSizeWithTag(1, value.foo)
        size += ProtoAdapter.STRING.encodedSizeWithTag(3, value.bar)
        size += ProtoAdapter.STRING.encodedSizeWithTag(4, value.baz)
        return size
      }

      public override fun encode(writer: ProtoWriter, `value`: OneOfMessage): Unit {
        ProtoAdapter.INT32.encodeWithTag(writer, 1, value.foo)
        ProtoAdapter.STRING.encodeWithTag(writer, 3, value.bar)
        ProtoAdapter.STRING.encodeWithTag(writer, 4, value.baz)
        writer.writeBytes(value.unknownFields)
      }

      public override fun decode(reader: ProtoReader): OneOfMessage {
        var foo: Int? = null
        var bar: String? = null
        var baz: String? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> foo = ProtoAdapter.INT32.decode(reader)
            3 -> bar = ProtoAdapter.STRING.decode(reader)
            4 -> baz = ProtoAdapter.STRING.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return OneOfMessage(
          foo = foo,
          bar = bar,
          baz = baz,
          unknownFields = unknownFields
        )
      }

      public override fun redact(`value`: OneOfMessage): OneOfMessage = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}
