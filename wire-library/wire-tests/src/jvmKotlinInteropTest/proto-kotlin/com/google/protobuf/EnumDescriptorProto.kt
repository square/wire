// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: google/protobuf/descriptor.proto
package com.google.protobuf

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.internal.checkElementsNotNull
import com.squareup.wire.internal.redactElements
import com.squareup.wire.internal.sanitize
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

/**
 * Describes an enum type.
 */
class EnumDescriptorProto(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  @JvmField
  val name: String? = null,
  @field:WireField(
    tag = 2,
    adapter = "com.google.protobuf.EnumValueDescriptorProto#ADAPTER",
    label = WireField.Label.REPEATED,
    encodeMode = WireField.EncodeMode.REPEATED
  )
  @JvmField
  val value: List<EnumValueDescriptorProto> = emptyList(),
  @field:WireField(
    tag = 3,
    adapter = "com.google.protobuf.EnumOptions#ADAPTER"
  )
  @JvmField
  val options: EnumOptions? = null,
  /**
   * Range of reserved numeric values. Reserved numeric values may not be used
   * by enum values in the same enum declaration. Reserved ranges may not
   * overlap.
   */
  @field:WireField(
    tag = 4,
    adapter = "com.google.protobuf.EnumDescriptorProto${'$'}EnumReservedRange#ADAPTER",
    label = WireField.Label.REPEATED,
    encodeMode = WireField.EncodeMode.REPEATED
  )
  @JvmField
  val reserved_range: List<EnumReservedRange> = emptyList(),
  /**
   * Reserved enum value names, which may not be reused. A given name may only
   * be reserved once.
   */
  @field:WireField(
    tag = 5,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    label = WireField.Label.REPEATED,
    encodeMode = WireField.EncodeMode.REPEATED
  )
  @JvmField
  val reserved_name: List<String> = emptyList(),
  unknownFields: ByteString = ByteString.EMPTY
) : Message<EnumDescriptorProto, EnumDescriptorProto.Builder>(ADAPTER, unknownFields) {
  override fun newBuilder(): Builder {
    val builder = Builder()
    builder.name = name
    builder.value = value
    builder.options = options
    builder.reserved_range = reserved_range
    builder.reserved_name = reserved_name
    builder.addUnknownFields(unknownFields)
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is EnumDescriptorProto) return false
    return unknownFields == other.unknownFields
        && name == other.name
        && value == other.value
        && options == other.options
        && reserved_range == other.reserved_range
        && reserved_name == other.reserved_name
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + name.hashCode()
      result = result * 37 + value.hashCode()
      result = result * 37 + options.hashCode()
      result = result * 37 + reserved_range.hashCode()
      result = result * 37 + reserved_name.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (name != null) result += """name=${sanitize(name)}"""
    if (value.isNotEmpty()) result += """value=$value"""
    if (options != null) result += """options=$options"""
    if (reserved_range.isNotEmpty()) result += """reserved_range=$reserved_range"""
    if (reserved_name.isNotEmpty()) result += """reserved_name=${sanitize(reserved_name)}"""
    return result.joinToString(prefix = "EnumDescriptorProto{", separator = ", ", postfix = "}")
  }

  fun copy(
    name: String? = this.name,
    value: List<EnumValueDescriptorProto> = this.value,
    options: EnumOptions? = this.options,
    reserved_range: List<EnumReservedRange> = this.reserved_range,
    reserved_name: List<String> = this.reserved_name,
    unknownFields: ByteString = this.unknownFields
  ): EnumDescriptorProto = EnumDescriptorProto(name, value, options, reserved_range, reserved_name,
      unknownFields)

  class Builder : Message.Builder<EnumDescriptorProto, Builder>() {
    @JvmField
    var name: String? = null

    @JvmField
    var value: List<EnumValueDescriptorProto> = emptyList()

    @JvmField
    var options: EnumOptions? = null

    @JvmField
    var reserved_range: List<EnumReservedRange> = emptyList()

    @JvmField
    var reserved_name: List<String> = emptyList()

    fun name(name: String?): Builder {
      this.name = name
      return this
    }

    fun value(value: List<EnumValueDescriptorProto>): Builder {
      checkElementsNotNull(value)
      this.value = value
      return this
    }

    fun options(options: EnumOptions?): Builder {
      this.options = options
      return this
    }

    /**
     * Range of reserved numeric values. Reserved numeric values may not be used
     * by enum values in the same enum declaration. Reserved ranges may not
     * overlap.
     */
    fun reserved_range(reserved_range: List<EnumReservedRange>): Builder {
      checkElementsNotNull(reserved_range)
      this.reserved_range = reserved_range
      return this
    }

    /**
     * Reserved enum value names, which may not be reused. A given name may only
     * be reserved once.
     */
    fun reserved_name(reserved_name: List<String>): Builder {
      checkElementsNotNull(reserved_name)
      this.reserved_name = reserved_name
      return this
    }

    override fun build(): EnumDescriptorProto = EnumDescriptorProto(
      name = name,
      value = value,
      options = options,
      reserved_range = reserved_range,
      reserved_name = reserved_name,
      unknownFields = buildUnknownFields()
    )
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<EnumDescriptorProto> = object : ProtoAdapter<EnumDescriptorProto>(
      FieldEncoding.LENGTH_DELIMITED, 
      EnumDescriptorProto::class, 
      "type.googleapis.com/google.protobuf.EnumDescriptorProto"
    ) {
      override fun encodedSize(value: EnumDescriptorProto): Int = 
        ProtoAdapter.STRING.encodedSizeWithTag(1, value.name) +
        EnumValueDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(2, value.value) +
        EnumOptions.ADAPTER.encodedSizeWithTag(3, value.options) +
        EnumReservedRange.ADAPTER.asRepeated().encodedSizeWithTag(4, value.reserved_range) +
        ProtoAdapter.STRING.asRepeated().encodedSizeWithTag(5, value.reserved_name) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: EnumDescriptorProto) {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
        EnumValueDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 2, value.value)
        EnumOptions.ADAPTER.encodeWithTag(writer, 3, value.options)
        EnumReservedRange.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.reserved_range)
        ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 5, value.reserved_name)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): EnumDescriptorProto {
        var name: String? = null
        val value = mutableListOf<EnumValueDescriptorProto>()
        var options: EnumOptions? = null
        val reserved_range = mutableListOf<EnumReservedRange>()
        val reserved_name = mutableListOf<String>()
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> name = ProtoAdapter.STRING.decode(reader)
            2 -> value.add(EnumValueDescriptorProto.ADAPTER.decode(reader))
            3 -> options = EnumOptions.ADAPTER.decode(reader)
            4 -> reserved_range.add(EnumReservedRange.ADAPTER.decode(reader))
            5 -> reserved_name.add(ProtoAdapter.STRING.decode(reader))
            else -> reader.readUnknownField(tag)
          }
        }
        return EnumDescriptorProto(
          name = name,
          value = value,
          options = options,
          reserved_range = reserved_range,
          reserved_name = reserved_name,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: EnumDescriptorProto): EnumDescriptorProto = value.copy(
        value = value.value.redactElements(EnumValueDescriptorProto.ADAPTER),
        options = value.options?.let(EnumOptions.ADAPTER::redact),
        reserved_range = value.reserved_range.redactElements(EnumReservedRange.ADAPTER),
        unknownFields = ByteString.EMPTY
      )
    }
  }

  /**
   * Range of reserved numeric values. Reserved values may not be used by
   * entries in the same enum. Reserved ranges may not overlap.
   *
   * Note that this is distinct from DescriptorProto.ReservedRange in that it
   * is inclusive such that it can appropriately represent the entire int32
   * domain.
   */
  class EnumReservedRange(
    /**
     * Inclusive.
     */
    @field:WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
    )
    @JvmField
    val start: Int? = null,
    /**
     * Inclusive.
     */
    @field:WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
    )
    @JvmField
    val end: Int? = null,
    unknownFields: ByteString = ByteString.EMPTY
  ) : Message<EnumReservedRange, EnumReservedRange.Builder>(ADAPTER, unknownFields) {
    override fun newBuilder(): Builder {
      val builder = Builder()
      builder.start = start
      builder.end = end
      builder.addUnknownFields(unknownFields)
      return builder
    }

    override fun equals(other: Any?): Boolean {
      if (other === this) return true
      if (other !is EnumReservedRange) return false
      return unknownFields == other.unknownFields
          && start == other.start
          && end == other.end
    }

    override fun hashCode(): Int {
      var result = super.hashCode
      if (result == 0) {
        result = unknownFields.hashCode()
        result = result * 37 + start.hashCode()
        result = result * 37 + end.hashCode()
        super.hashCode = result
      }
      return result
    }

    override fun toString(): String {
      val result = mutableListOf<String>()
      if (start != null) result += """start=$start"""
      if (end != null) result += """end=$end"""
      return result.joinToString(prefix = "EnumReservedRange{", separator = ", ", postfix = "}")
    }

    fun copy(
      start: Int? = this.start,
      end: Int? = this.end,
      unknownFields: ByteString = this.unknownFields
    ): EnumReservedRange = EnumReservedRange(start, end, unknownFields)

    class Builder : Message.Builder<EnumReservedRange, Builder>() {
      @JvmField
      var start: Int? = null

      @JvmField
      var end: Int? = null

      /**
       * Inclusive.
       */
      fun start(start: Int?): Builder {
        this.start = start
        return this
      }

      /**
       * Inclusive.
       */
      fun end(end: Int?): Builder {
        this.end = end
        return this
      }

      override fun build(): EnumReservedRange = EnumReservedRange(
        start = start,
        end = end,
        unknownFields = buildUnknownFields()
      )
    }

    companion object {
      @JvmField
      val ADAPTER: ProtoAdapter<EnumReservedRange> = object : ProtoAdapter<EnumReservedRange>(
        FieldEncoding.LENGTH_DELIMITED, 
        EnumReservedRange::class, 
        "type.googleapis.com/google.protobuf.EnumDescriptorProto.EnumReservedRange"
      ) {
        override fun encodedSize(value: EnumReservedRange): Int = 
          ProtoAdapter.INT32.encodedSizeWithTag(1, value.start) +
          ProtoAdapter.INT32.encodedSizeWithTag(2, value.end) +
          value.unknownFields.size

        override fun encode(writer: ProtoWriter, value: EnumReservedRange) {
          ProtoAdapter.INT32.encodeWithTag(writer, 1, value.start)
          ProtoAdapter.INT32.encodeWithTag(writer, 2, value.end)
          writer.writeBytes(value.unknownFields)
        }

        override fun decode(reader: ProtoReader): EnumReservedRange {
          var start: Int? = null
          var end: Int? = null
          val unknownFields = reader.forEachTag { tag ->
            when (tag) {
              1 -> start = ProtoAdapter.INT32.decode(reader)
              2 -> end = ProtoAdapter.INT32.decode(reader)
              else -> reader.readUnknownField(tag)
            }
          }
          return EnumReservedRange(
            start = start,
            end = end,
            unknownFields = unknownFields
          )
        }

        override fun redact(value: EnumReservedRange): EnumReservedRange = value.copy(
          unknownFields = ByteString.EMPTY
        )
      }
    }
  }
}
