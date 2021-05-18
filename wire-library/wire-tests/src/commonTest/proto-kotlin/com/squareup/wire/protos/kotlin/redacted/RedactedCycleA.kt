// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.redacted_test.RedactedCycleA in redacted_test.proto
package com.squareup.wire.protos.kotlin.redacted

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.WireField
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
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

public class RedactedCycleA(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.protos.kotlin.redacted.RedactedCycleB#ADAPTER"
  )
  public val b: RedactedCycleB? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<RedactedCycleA, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  public override fun newBuilder(): Nothing = throw
      AssertionError("Builders are deprecated and only available in a javaInterop build; see https://square.github.io/wire/wire_compiler/#kotlin")

  public override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is RedactedCycleA) return false
    if (unknownFields != other.unknownFields) return false
    if (b != other.b) return false
    return true
  }

  public override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + b.hashCode()
      super.hashCode = result
    }
    return result
  }

  public override fun toString(): String {
    val result = mutableListOf<String>()
    if (b != null) result += """b=$b"""
    return result.joinToString(prefix = "RedactedCycleA{", separator = ", ", postfix = "}")
  }

  public fun copy(b: RedactedCycleB? = this.b, unknownFields: ByteString = this.unknownFields):
      RedactedCycleA = RedactedCycleA(b, unknownFields)

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<RedactedCycleA> = object : ProtoAdapter<RedactedCycleA>(
      FieldEncoding.LENGTH_DELIMITED, 
      RedactedCycleA::class, 
      "type.googleapis.com/squareup.protos.kotlin.redacted_test.RedactedCycleA", 
      PROTO_2, 
      null
    ) {
      public override fun encodedSize(value: RedactedCycleA): Int {
        var size = value.unknownFields.size
        size += RedactedCycleB.ADAPTER.encodedSizeWithTag(1, value.b)
        return size
      }

      public override fun encode(writer: ProtoWriter, value: RedactedCycleA): Unit {
        RedactedCycleB.ADAPTER.encodeWithTag(writer, 1, value.b)
        writer.writeBytes(value.unknownFields)
      }

      public override fun decode(reader: ProtoReader): RedactedCycleA {
        var b: RedactedCycleB? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> b = RedactedCycleB.ADAPTER.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return RedactedCycleA(
          b = b,
          unknownFields = unknownFields
        )
      }

      public override fun redact(value: RedactedCycleA): RedactedCycleA = value.copy(
        b = value.b?.let(RedactedCycleB.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}
