// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.proto2.person.kotlin.Person in person_kotlin.proto
package com.squareup.wire.proto2.person.kotlin

import com.squareup.wire.EnumAdapter
import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.Syntax
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.WireEnum
import com.squareup.wire.WireField
import com.squareup.wire.`internal`.immutableCopyOf
import com.squareup.wire.`internal`.missingRequiredFields
import com.squareup.wire.`internal`.redactElements
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
import kotlin.collections.List
import kotlin.hashCode
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import okio.ByteString

/**
 * Message representing a person, includes their name, unique ID number, email and phone number.
 */
public class Person(
  /**
   * The customer's full name.
   */
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    label = WireField.Label.REQUIRED
  )
  public val name: String,
  /**
   * The customer's ID number.
   */
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#INT32",
    label = WireField.Label.REQUIRED
  )
  public val id: Int,
  /**
   * Email address for the customer.
   */
  @field:WireField(
    tag = 3,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public val email: String? = null,
  phone: List<PhoneNumber> = emptyList(),
  unknownFields: ByteString = ByteString.EMPTY
) : Message<Person, Nothing>(ADAPTER, unknownFields) {
  /**
   * A list of the customer's phone numbers.
   */
  @field:WireField(
    tag = 4,
    adapter = "com.squareup.wire.proto2.person.kotlin.Person${'$'}PhoneNumber#ADAPTER",
    label = WireField.Label.REPEATED
  )
  public val phone: List<PhoneNumber> = immutableCopyOf("phone", phone)

  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  public override fun newBuilder(): Nothing = throw AssertionError()

  public override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is Person) return false
    if (unknownFields != other.unknownFields) return false
    if (name != other.name) return false
    if (id != other.id) return false
    if (email != other.email) return false
    if (phone != other.phone) return false
    return true
  }

  public override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + name.hashCode()
      result = result * 37 + id.hashCode()
      result = result * 37 + email.hashCode()
      result = result * 37 + phone.hashCode()
      super.hashCode = result
    }
    return result
  }

  public override fun toString(): String {
    val result = mutableListOf<String>()
    result += """name=${sanitize(name)}"""
    result += """id=$id"""
    if (email != null) result += """email=${sanitize(email)}"""
    if (phone.isNotEmpty()) result += """phone=$phone"""
    return result.joinToString(prefix = "Person{", separator = ", ", postfix = "}")
  }

  public fun copy(
    name: String = this.name,
    id: Int = this.id,
    email: String? = this.email,
    phone: List<PhoneNumber> = this.phone,
    unknownFields: ByteString = this.unknownFields
  ): Person = Person(name, id, email, phone, unknownFields)

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<Person> = object : ProtoAdapter<Person>(
      FieldEncoding.LENGTH_DELIMITED, 
      Person::class, 
      "type.googleapis.com/squareup.proto2.person.kotlin.Person", 
      PROTO_2, 
      null
    ) {
      public override fun encodedSize(value: Person): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
        size += ProtoAdapter.INT32.encodedSizeWithTag(2, value.id)
        size += ProtoAdapter.STRING.encodedSizeWithTag(3, value.email)
        size += PhoneNumber.ADAPTER.asRepeated().encodedSizeWithTag(4, value.phone)
        return size
      }

      public override fun encode(writer: ProtoWriter, value: Person): Unit {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
        ProtoAdapter.INT32.encodeWithTag(writer, 2, value.id)
        ProtoAdapter.STRING.encodeWithTag(writer, 3, value.email)
        PhoneNumber.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.phone)
        writer.writeBytes(value.unknownFields)
      }

      public override fun decode(reader: ProtoReader): Person {
        var name: String? = null
        var id: Int? = null
        var email: String? = null
        val phone = mutableListOf<PhoneNumber>()
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> name = ProtoAdapter.STRING.decode(reader)
            2 -> id = ProtoAdapter.INT32.decode(reader)
            3 -> email = ProtoAdapter.STRING.decode(reader)
            4 -> phone.add(PhoneNumber.ADAPTER.decode(reader))
            else -> reader.readUnknownField(tag)
          }
        }
        return Person(
          name = name ?: throw missingRequiredFields(name, "name"),
          id = id ?: throw missingRequiredFields(id, "id"),
          email = email,
          phone = phone,
          unknownFields = unknownFields
        )
      }

      public override fun redact(value: Person): Person = value.copy(
        phone = value.phone.redactElements(PhoneNumber.ADAPTER),
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }

  /**
   * Represents the type of the phone number: mobile, home or work.
   */
  public enum class PhoneType(
    public override val value: Int
  ) : WireEnum {
    MOBILE(0),
    HOME(1),
    /**
     * Could be phone or fax.
     */
    WORK(2),
    ;

    public companion object {
      @JvmField
      public val ADAPTER: ProtoAdapter<PhoneType> = object : EnumAdapter<PhoneType>(
        PhoneType::class, 
        PROTO_2, 
        PhoneType.MOBILE
      ) {
        public override fun fromValue(value: Int): PhoneType? = PhoneType.fromValue(value)
      }

      @JvmStatic
      public fun fromValue(value: Int): PhoneType? = when (value) {
        0 -> MOBILE
        1 -> HOME
        2 -> WORK
        else -> null
      }
    }
  }

  public class PhoneNumber(
    /**
     * The customer's phone number.
     */
    @field:WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING",
      label = WireField.Label.REQUIRED
    )
    public val number: String,
    /**
     * The type of phone stored here.
     */
    @field:WireField(
      tag = 2,
      adapter = "com.squareup.wire.proto2.person.kotlin.Person${'$'}PhoneType#ADAPTER"
    )
    public val type: PhoneType? = null,
    unknownFields: ByteString = ByteString.EMPTY
  ) : Message<PhoneNumber, Nothing>(ADAPTER, unknownFields) {
    @Deprecated(
      message = "Shouldn't be used in Kotlin",
      level = DeprecationLevel.HIDDEN
    )
    public override fun newBuilder(): Nothing = throw AssertionError()

    public override fun equals(other: Any?): Boolean {
      if (other === this) return true
      if (other !is PhoneNumber) return false
      if (unknownFields != other.unknownFields) return false
      if (number != other.number) return false
      if (type != other.type) return false
      return true
    }

    public override fun hashCode(): Int {
      var result = super.hashCode
      if (result == 0) {
        result = unknownFields.hashCode()
        result = result * 37 + number.hashCode()
        result = result * 37 + type.hashCode()
        super.hashCode = result
      }
      return result
    }

    public override fun toString(): String {
      val result = mutableListOf<String>()
      result += """number=${sanitize(number)}"""
      if (type != null) result += """type=$type"""
      return result.joinToString(prefix = "PhoneNumber{", separator = ", ", postfix = "}")
    }

    public fun copy(
      number: String = this.number,
      type: PhoneType? = this.type,
      unknownFields: ByteString = this.unknownFields
    ): PhoneNumber = PhoneNumber(number, type, unknownFields)

    public companion object {
      @JvmField
      public val DEFAULT_TYPE: PhoneType = PhoneType.HOME

      @JvmField
      public val ADAPTER: ProtoAdapter<PhoneNumber> = object : ProtoAdapter<PhoneNumber>(
        FieldEncoding.LENGTH_DELIMITED, 
        PhoneNumber::class, 
        "type.googleapis.com/squareup.proto2.person.kotlin.Person.PhoneNumber", 
        PROTO_2, 
        null
      ) {
        public override fun encodedSize(value: PhoneNumber): Int {
          var size = value.unknownFields.size
          size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.number)
          size += PhoneType.ADAPTER.encodedSizeWithTag(2, value.type)
          return size
        }

        public override fun encode(writer: ProtoWriter, value: PhoneNumber): Unit {
          ProtoAdapter.STRING.encodeWithTag(writer, 1, value.number)
          PhoneType.ADAPTER.encodeWithTag(writer, 2, value.type)
          writer.writeBytes(value.unknownFields)
        }

        public override fun decode(reader: ProtoReader): PhoneNumber {
          var number: String? = null
          var type: PhoneType? = null
          val unknownFields = reader.forEachTag { tag ->
            when (tag) {
              1 -> number = ProtoAdapter.STRING.decode(reader)
              2 -> try {
                type = PhoneType.ADAPTER.decode(reader)
              } catch (e: ProtoAdapter.EnumConstantNotFoundException) {
                reader.addUnknownField(tag, FieldEncoding.VARINT, e.value.toLong())
              }
              else -> reader.readUnknownField(tag)
            }
          }
          return PhoneNumber(
            number = number ?: throw missingRequiredFields(number, "number"),
            type = type,
            unknownFields = unknownFields
          )
        }

        public override fun redact(value: PhoneNumber): PhoneNumber = value.copy(
          unknownFields = ByteString.EMPTY
        )
      }

      private const val serialVersionUID: Long = 0L
    }
  }
}
