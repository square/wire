// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: google/protobuf/descriptor.proto
package com.google.protobuf

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.internal.redactElements
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.collections.List
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

class ServiceOptions(
  /**
   * Note:  Field numbers 1 through 32 are reserved for Google's internal RPC
   *   framework.  We apologize for hoarding these numbers to ourselves, but
   *   we were already using them long before we decided to release Protocol
   *   Buffers.
   * Is this service deprecated?
   * Depending on the target platform, this can emit Deprecated annotations
   * for the service, or it will be completely ignored; in the very least,
   * this is a formalization for deprecating services.
   */
  @field:WireField(
    tag = 33,
    adapter = "com.squareup.wire.ProtoAdapter#BOOL"
  )
  val deprecated: Boolean? = null,
  /**
   * The parser stores options it doesn't recognize here. See above.
   */
  @field:WireField(
    tag = 999,
    adapter = "com.google.protobuf.UninterpretedOption#ADAPTER",
    label = WireField.Label.REPEATED
  )
  val uninterpreted_option: List<UninterpretedOption> = emptyList(),
  unknownFields: ByteString = ByteString.EMPTY
) : Message<ServiceOptions, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing = throw AssertionError()

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is ServiceOptions) return false
    var result = unknownFields == other.unknownFields
    result = result && (deprecated == other.deprecated)
    result = result && (uninterpreted_option == other.uninterpreted_option)
    return result
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + deprecated.hashCode()
      result = result * 37 + uninterpreted_option.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (deprecated != null) result += """deprecated=$deprecated"""
    if (uninterpreted_option.isNotEmpty()) result +=
        """uninterpreted_option=$uninterpreted_option"""
    return result.joinToString(prefix = "ServiceOptions{", separator = ", ", postfix = "}")
  }

  fun copy(
    deprecated: Boolean? = this.deprecated,
    uninterpreted_option: List<UninterpretedOption> = this.uninterpreted_option,
    unknownFields: ByteString = this.unknownFields
  ): ServiceOptions = ServiceOptions(deprecated, uninterpreted_option, unknownFields)

  companion object {
    const val DEFAULT_DEPRECATED: Boolean = false

    @JvmField
    val ADAPTER: ProtoAdapter<ServiceOptions> = object : ProtoAdapter<ServiceOptions>(
      FieldEncoding.LENGTH_DELIMITED, 
      ServiceOptions::class, 
      "type.googleapis.com/google.protobuf.ServiceOptions"
    ) {
      override fun encodedSize(value: ServiceOptions): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.BOOL.encodedSizeWithTag(33, value.deprecated)
        size += UninterpretedOption.ADAPTER.asRepeated().encodedSizeWithTag(999,
            value.uninterpreted_option)
        return size
      }

      override fun encode(writer: ProtoWriter, value: ServiceOptions) {
        ProtoAdapter.BOOL.encodeWithTag(writer, 33, value.deprecated)
        UninterpretedOption.ADAPTER.asRepeated().encodeWithTag(writer, 999,
            value.uninterpreted_option)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): ServiceOptions {
        var deprecated: Boolean? = null
        val uninterpreted_option = mutableListOf<UninterpretedOption>()
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            33 -> deprecated = ProtoAdapter.BOOL.decode(reader)
            999 -> uninterpreted_option.add(UninterpretedOption.ADAPTER.decode(reader))
            else -> reader.readUnknownField(tag)
          }
        }
        return ServiceOptions(
          deprecated = deprecated,
          uninterpreted_option = uninterpreted_option,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: ServiceOptions): ServiceOptions = value.copy(
        uninterpreted_option =
            value.uninterpreted_option.redactElements(UninterpretedOption.ADAPTER),
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
