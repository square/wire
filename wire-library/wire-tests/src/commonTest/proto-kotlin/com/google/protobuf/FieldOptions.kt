// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: google/protobuf/descriptor.proto
package com.google.protobuf

import com.squareup.wire.EnumAdapter
import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireEnum
import com.squareup.wire.WireField
import com.squareup.wire.internal.redactElements
import com.squareup.wire.protos.custom_options.FooBar
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Float
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.collections.List
import kotlin.hashCode
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import okio.ByteString

class FieldOptions(
  /**
   * The ctype option instructs the C++ code generator to use a different
   * representation of the field than it normally would.  See the specific
   * options below.  This option is not yet implemented in the open source
   * release -- sorry, we'll try to include it in a future version!
   */
  @field:WireField(
    tag = 1,
    adapter = "com.google.protobuf.FieldOptions${'$'}CType#ADAPTER"
  )
  val ctype: CType? = null,
  /**
   * The packed option can be enabled for repeated primitive fields to enable
   * a more efficient representation on the wire. Rather than repeatedly
   * writing the tag and type for each element, the entire array is encoded as
   * a single length-delimited blob. In proto3, only explicit setting it to
   * false will avoid using packed encoding.
   */
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#BOOL"
  )
  val packed: Boolean? = null,
  /**
   * The jstype option determines the JavaScript type used for values of the
   * field.  The option is permitted only for 64 bit integral and fixed types
   * (int64, uint64, sint64, fixed64, sfixed64).  A field with jstype JS_STRING
   * is represented as JavaScript string, which avoids loss of precision that
   * can happen when a large value is converted to a floating point JavaScript.
   * Specifying JS_NUMBER for the jstype causes the generated JavaScript code to
   * use the JavaScript "number" type.  The behavior of the default option
   * JS_NORMAL is implementation dependent.
   *
   * This option is an enum to permit additional types to be added, e.g.
   * goog.math.Integer.
   */
  @field:WireField(
    tag = 6,
    adapter = "com.google.protobuf.FieldOptions${'$'}JSType#ADAPTER"
  )
  val jstype: JSType? = null,
  /**
   * Should this field be parsed lazily?  Lazy applies only to message-type
   * fields.  It means that when the outer message is initially parsed, the
   * inner message's contents will not be parsed but instead stored in encoded
   * form.  The inner message will actually be parsed when it is first accessed.
   *
   * This is only a hint.  Implementations are free to choose whether to use
   * eager or lazy parsing regardless of the value of this option.  However,
   * setting this option true suggests that the protocol author believes that
   * using lazy parsing on this field is worth the additional bookkeeping
   * overhead typically needed to implement it.
   *
   * This option does not affect the public interface of any generated code;
   * all method signatures remain the same.  Furthermore, thread-safety of the
   * interface is not affected by this option; const methods remain safe to
   * call from multiple threads concurrently, while non-const methods continue
   * to require exclusive access.
   *
   *
   * Note that implementations may choose not to check required fields within
   * a lazy sub-message.  That is, calling IsInitialized() on the outer message
   * may return true even if the inner message has missing required fields.
   * This is necessary because otherwise the inner message would have to be
   * parsed in order to perform the check, defeating the purpose of lazy
   * parsing.  An implementation which chooses not to check required fields
   * must be consistent about it.  That is, for any particular sub-message, the
   * implementation must either *always* check its required fields, or *never*
   * check its required fields, regardless of whether or not the message has
   * been parsed.
   */
  @field:WireField(
    tag = 5,
    adapter = "com.squareup.wire.ProtoAdapter#BOOL"
  )
  val lazy: Boolean? = null,
  /**
   * Is this field deprecated?
   * Depending on the target platform, this can emit Deprecated annotations
   * for accessors, or it will be completely ignored; in the very least, this
   * is a formalization for deprecating fields.
   */
  @field:WireField(
    tag = 3,
    adapter = "com.squareup.wire.ProtoAdapter#BOOL"
  )
  val deprecated: Boolean? = null,
  /**
   * For Google-internal migration only. Do not use.
   */
  @field:WireField(
    tag = 10,
    adapter = "com.squareup.wire.ProtoAdapter#BOOL"
  )
  val weak: Boolean? = null,
  /**
   * The parser stores options it doesn't recognize here. See above.
   */
  @field:WireField(
    tag = 999,
    adapter = "com.google.protobuf.UninterpretedOption#ADAPTER",
    label = WireField.Label.REPEATED
  )
  val uninterpreted_option: List<UninterpretedOption> = emptyList(),
  /**
   * Extension source: custom_options.proto
   */
  @field:WireField(
    tag = 60001,
    adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  val my_field_option_one: Int? = null,
  /**
   * Extension source: custom_options.proto
   */
  @field:WireField(
    tag = 60002,
    adapter = "com.squareup.wire.ProtoAdapter#FLOAT"
  )
  val my_field_option_two: Float? = null,
  /**
   * Extension source: custom_options.proto
   */
  @field:WireField(
    tag = 60003,
    adapter = "com.squareup.wire.protos.custom_options.FooBar${'$'}FooBarBazEnum#ADAPTER"
  )
  val my_field_option_three: FooBar.FooBarBazEnum? = null,
  /**
   * Extension source: custom_options.proto
   */
  @field:WireField(
    tag = 60004,
    adapter = "com.squareup.wire.protos.custom_options.FooBar#ADAPTER"
  )
  val my_field_option_four: FooBar? = null,
  /**
   * Fields marked with redacted are not to be logged, generally for PCI or PII.
   * Extension source: option_redacted.proto
   */
  @field:WireField(
    tag = 22300,
    adapter = "com.squareup.wire.ProtoAdapter#BOOL"
  )
  val redacted: Boolean? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<FieldOptions, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing = throw AssertionError()

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is FieldOptions) return false
    if (unknownFields != other.unknownFields) return false
    if (ctype != other.ctype) return false
    if (packed != other.packed) return false
    if (jstype != other.jstype) return false
    if (lazy != other.lazy) return false
    if (deprecated != other.deprecated) return false
    if (weak != other.weak) return false
    if (uninterpreted_option != other.uninterpreted_option) return false
    if (my_field_option_one != other.my_field_option_one) return false
    if (my_field_option_two != other.my_field_option_two) return false
    if (my_field_option_three != other.my_field_option_three) return false
    if (my_field_option_four != other.my_field_option_four) return false
    if (redacted != other.redacted) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + ctype.hashCode()
      result = result * 37 + packed.hashCode()
      result = result * 37 + jstype.hashCode()
      result = result * 37 + lazy.hashCode()
      result = result * 37 + deprecated.hashCode()
      result = result * 37 + weak.hashCode()
      result = result * 37 + uninterpreted_option.hashCode()
      result = result * 37 + my_field_option_one.hashCode()
      result = result * 37 + my_field_option_two.hashCode()
      result = result * 37 + my_field_option_three.hashCode()
      result = result * 37 + my_field_option_four.hashCode()
      result = result * 37 + redacted.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (ctype != null) result += """ctype=$ctype"""
    if (packed != null) result += """packed=$packed"""
    if (jstype != null) result += """jstype=$jstype"""
    if (lazy != null) result += """lazy=$lazy"""
    if (deprecated != null) result += """deprecated=$deprecated"""
    if (weak != null) result += """weak=$weak"""
    if (uninterpreted_option.isNotEmpty()) result +=
        """uninterpreted_option=$uninterpreted_option"""
    if (my_field_option_one != null) result += """my_field_option_one=$my_field_option_one"""
    if (my_field_option_two != null) result += """my_field_option_two=$my_field_option_two"""
    if (my_field_option_three != null) result += """my_field_option_three=$my_field_option_three"""
    if (my_field_option_four != null) result += """my_field_option_four=$my_field_option_four"""
    if (redacted != null) result += """redacted=$redacted"""
    return result.joinToString(prefix = "FieldOptions{", separator = ", ", postfix = "}")
  }

  fun copy(
    ctype: CType? = this.ctype,
    packed: Boolean? = this.packed,
    jstype: JSType? = this.jstype,
    lazy: Boolean? = this.lazy,
    deprecated: Boolean? = this.deprecated,
    weak: Boolean? = this.weak,
    uninterpreted_option: List<UninterpretedOption> = this.uninterpreted_option,
    my_field_option_one: Int? = this.my_field_option_one,
    my_field_option_two: Float? = this.my_field_option_two,
    my_field_option_three: FooBar.FooBarBazEnum? = this.my_field_option_three,
    my_field_option_four: FooBar? = this.my_field_option_four,
    redacted: Boolean? = this.redacted,
    unknownFields: ByteString = this.unknownFields
  ): FieldOptions = FieldOptions(ctype, packed, jstype, lazy, deprecated, weak,
      uninterpreted_option, my_field_option_one, my_field_option_two, my_field_option_three,
      my_field_option_four, redacted, unknownFields)

  companion object {
    @JvmField
    val DEFAULT_CTYPE: CType = CType.STRING

    @JvmField
    val DEFAULT_JSTYPE: JSType = JSType.JS_NORMAL

    const val DEFAULT_LAZY: Boolean = false

    const val DEFAULT_DEPRECATED: Boolean = false

    const val DEFAULT_WEAK: Boolean = false

    @JvmField
    val ADAPTER: ProtoAdapter<FieldOptions> = object : ProtoAdapter<FieldOptions>(
      FieldEncoding.LENGTH_DELIMITED, 
      FieldOptions::class, 
      "type.googleapis.com/google.protobuf.FieldOptions"
    ) {
      override fun encodedSize(value: FieldOptions): Int {
        var size = value.unknownFields.size
        size += CType.ADAPTER.encodedSizeWithTag(1, value.ctype)
        size += ProtoAdapter.BOOL.encodedSizeWithTag(2, value.packed)
        size += JSType.ADAPTER.encodedSizeWithTag(6, value.jstype)
        size += ProtoAdapter.BOOL.encodedSizeWithTag(5, value.lazy)
        size += ProtoAdapter.BOOL.encodedSizeWithTag(3, value.deprecated)
        size += ProtoAdapter.BOOL.encodedSizeWithTag(10, value.weak)
        size += UninterpretedOption.ADAPTER.asRepeated().encodedSizeWithTag(999,
            value.uninterpreted_option)
        size += ProtoAdapter.INT32.encodedSizeWithTag(60001, value.my_field_option_one)
        size += ProtoAdapter.FLOAT.encodedSizeWithTag(60002, value.my_field_option_two)
        size += FooBar.FooBarBazEnum.ADAPTER.encodedSizeWithTag(60003, value.my_field_option_three)
        size += FooBar.ADAPTER.encodedSizeWithTag(60004, value.my_field_option_four)
        size += ProtoAdapter.BOOL.encodedSizeWithTag(22300, value.redacted)
        return size
      }

      override fun encode(writer: ProtoWriter, value: FieldOptions) {
        CType.ADAPTER.encodeWithTag(writer, 1, value.ctype)
        ProtoAdapter.BOOL.encodeWithTag(writer, 2, value.packed)
        JSType.ADAPTER.encodeWithTag(writer, 6, value.jstype)
        ProtoAdapter.BOOL.encodeWithTag(writer, 5, value.lazy)
        ProtoAdapter.BOOL.encodeWithTag(writer, 3, value.deprecated)
        ProtoAdapter.BOOL.encodeWithTag(writer, 10, value.weak)
        UninterpretedOption.ADAPTER.asRepeated().encodeWithTag(writer, 999,
            value.uninterpreted_option)
        ProtoAdapter.INT32.encodeWithTag(writer, 60001, value.my_field_option_one)
        ProtoAdapter.FLOAT.encodeWithTag(writer, 60002, value.my_field_option_two)
        FooBar.FooBarBazEnum.ADAPTER.encodeWithTag(writer, 60003, value.my_field_option_three)
        FooBar.ADAPTER.encodeWithTag(writer, 60004, value.my_field_option_four)
        ProtoAdapter.BOOL.encodeWithTag(writer, 22300, value.redacted)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): FieldOptions {
        var ctype: CType? = null
        var packed: Boolean? = null
        var jstype: JSType? = null
        var lazy: Boolean? = null
        var deprecated: Boolean? = null
        var weak: Boolean? = null
        val uninterpreted_option = mutableListOf<UninterpretedOption>()
        var my_field_option_one: Int? = null
        var my_field_option_two: Float? = null
        var my_field_option_three: FooBar.FooBarBazEnum? = null
        var my_field_option_four: FooBar? = null
        var redacted: Boolean? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> try {
              ctype = CType.ADAPTER.decode(reader)
            } catch (e: ProtoAdapter.EnumConstantNotFoundException) {
              reader.addUnknownField(tag, FieldEncoding.VARINT, e.value.toLong())
            }
            2 -> packed = ProtoAdapter.BOOL.decode(reader)
            6 -> try {
              jstype = JSType.ADAPTER.decode(reader)
            } catch (e: ProtoAdapter.EnumConstantNotFoundException) {
              reader.addUnknownField(tag, FieldEncoding.VARINT, e.value.toLong())
            }
            5 -> lazy = ProtoAdapter.BOOL.decode(reader)
            3 -> deprecated = ProtoAdapter.BOOL.decode(reader)
            10 -> weak = ProtoAdapter.BOOL.decode(reader)
            999 -> uninterpreted_option.add(UninterpretedOption.ADAPTER.decode(reader))
            60001 -> my_field_option_one = ProtoAdapter.INT32.decode(reader)
            60002 -> my_field_option_two = ProtoAdapter.FLOAT.decode(reader)
            60003 -> try {
              my_field_option_three = FooBar.FooBarBazEnum.ADAPTER.decode(reader)
            } catch (e: ProtoAdapter.EnumConstantNotFoundException) {
              reader.addUnknownField(tag, FieldEncoding.VARINT, e.value.toLong())
            }
            60004 -> my_field_option_four = FooBar.ADAPTER.decode(reader)
            22300 -> redacted = ProtoAdapter.BOOL.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return FieldOptions(
          ctype = ctype,
          packed = packed,
          jstype = jstype,
          lazy = lazy,
          deprecated = deprecated,
          weak = weak,
          uninterpreted_option = uninterpreted_option,
          my_field_option_one = my_field_option_one,
          my_field_option_two = my_field_option_two,
          my_field_option_three = my_field_option_three,
          my_field_option_four = my_field_option_four,
          redacted = redacted,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: FieldOptions): FieldOptions = value.copy(
        uninterpreted_option =
            value.uninterpreted_option.redactElements(UninterpretedOption.ADAPTER),
        my_field_option_four = value.my_field_option_four?.let(FooBar.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }
  }

  enum class CType(
    override val value: Int
  ) : WireEnum {
    /**
     * Default mode.
     */
    STRING(0),

    CORD(1),

    STRING_PIECE(2);

    companion object {
      @JvmField
      val ADAPTER: ProtoAdapter<CType> = object : EnumAdapter<CType>(
        CType::class
      ) {
        override fun fromValue(value: Int): CType? = CType.fromValue(value)
      }

      @JvmStatic
      fun fromValue(value: Int): CType? = when (value) {
        0 -> STRING
        1 -> CORD
        2 -> STRING_PIECE
        else -> null
      }
    }
  }

  enum class JSType(
    override val value: Int
  ) : WireEnum {
    /**
     * Use the default type.
     */
    JS_NORMAL(0),

    /**
     * Use JavaScript strings.
     */
    JS_STRING(1),

    /**
     * Use JavaScript numbers.
     */
    JS_NUMBER(2);

    companion object {
      @JvmField
      val ADAPTER: ProtoAdapter<JSType> = object : EnumAdapter<JSType>(
        JSType::class
      ) {
        override fun fromValue(value: Int): JSType? = JSType.fromValue(value)
      }

      @JvmStatic
      fun fromValue(value: Int): JSType? = when (value) {
        0 -> JS_NORMAL
        1 -> JS_STRING
        2 -> JS_NUMBER
        else -> null
      }
    }
  }
}
