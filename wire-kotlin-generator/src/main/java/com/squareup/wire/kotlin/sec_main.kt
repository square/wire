package com.squareup.wire.kotlin

import com.squareup.wire.EnumAdapter
import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireEnum
import com.squareup.wire.internal.Internal
import okio.ByteString

data class Person2(
    val name: String,
    val id: Int,
    val email: String? = null,
    val phone: List<PhoneNumber>,
    val weight: Int,
    val height: Int,
    val unknownFields: ByteString = ByteString.EMPTY
) {
  companion object {
    @JvmField val ADAPTER = Person_ADAPTER()
  }

  data class PhoneNumber(
      val number: String,
      val phoneType: PhoneType,
      val unknownFields: ByteString = ByteString.EMPTY
  ) {

    enum class PhoneType(private val value: Int) : WireEnum {
      MOBILE(0),
      HOME(4),
      WORK(2);

      // Ideally we should be able to declare the above field as public and use the getter below.
      override fun getValue(): Int {
        return value
      }

      companion object {
        @JvmField val ADAPTER = PhoneType_Adapter()

        fun fromValue(value: Int): PhoneType? {
          return values().find { it.value == value }
        }
      }

      class PhoneType_Adapter : EnumAdapter<PhoneType>(PhoneType::class.java) {
        override fun fromValue(value: Int): PhoneType? {
          return PhoneType.fromValue(value)
        }
      }
    }

    companion object {
      @JvmField val ADAPTER = PhoneNumber_ADAPTER()
    }

    class PhoneNumber_ADAPTER : ProtoAdapter<PhoneNumber>(FieldEncoding.LENGTH_DELIMITED, PhoneNumber::class.java) {
      override fun encodedSize(value: PhoneNumber): Int {
        return (ProtoAdapter.STRING.encodedSizeWithTag(1, value.number)
            + PhoneType.ADAPTER.encodedSizeWithTag(2, value.phoneType)
            + value.unknownFields.size())
      }

      override fun encode(writer: ProtoWriter, value: PhoneNumber) {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.number)
        PhoneType.ADAPTER.encodeWithTag(writer, 2, value.phoneType)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): PhoneNumber {
        var number: String? = null
        var phoneType: PhoneType? = null

        val unknownFields = reader.decodeMessage { tag ->
          when (tag) {
            1 -> number = ProtoAdapter.STRING.decode(reader)
            2 -> phoneType = PhoneType.ADAPTER.decode(reader)
            else -> UNKNOWN_FIELD
          }
        }

        return PhoneNumber(number = number ?: throw Internal.missingRequiredFields(number, "number"),
            phoneType = phoneType ?: throw Internal.missingRequiredFields(phoneType, "phoneType"),
            unknownFields = unknownFields
        )
      }
    }
  }

  class Person_ADAPTER : ProtoAdapter<Person2>(FieldEncoding.LENGTH_DELIMITED, Person2::class.java) {

    override fun encodedSize(value: Person2): Int {
      return (ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
          + ProtoAdapter.INT32.encodedSizeWithTag(2, value.id)
          + ProtoAdapter.STRING.encodedSizeWithTag(3, value.email)
          + PhoneNumber.ADAPTER.asRepeated().encodedSizeWithTag(4, value.phone)
          + ProtoAdapter.INT32.encodedSizeWithTag(5, value.weight)
          + ProtoAdapter.INT32.encodedSizeWithTag(6, value.height)
          + value.unknownFields.size())
    }

    override fun encode(writer: ProtoWriter, value: Person2) {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
      ProtoAdapter.INT32.encodeWithTag(writer, 2, value.id)
      ProtoAdapter.STRING.encodeWithTag(writer, 3, value.email)
      PhoneNumber.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.phone)
      ProtoAdapter.INT32.encodeWithTag(writer, 5, value.weight)
      ProtoAdapter.INT32.encodeWithTag(writer, 6, value.height)
      writer.writeBytes(value.unknownFields)
    }

    override fun decode(reader: ProtoReader): Person2 {
      var name: String? = null
      var id: Int? = null
      var email: String? = null
      var phone = mutableListOf<Person2.PhoneNumber>()
      var weight: Int? = null
      var height: Int? = null


      val unknownFields = reader.decodeMessage { tag ->
        when (tag) {
          1 -> name = ProtoAdapter.STRING.decode(reader)
          2 -> id = ProtoAdapter.INT32.decode(reader)
          3 -> email = ProtoAdapter.STRING.decode(reader)
          4 -> phone.add(PhoneNumber.ADAPTER.decode(reader))
          5 -> weight = ProtoAdapter.INT32.decode(reader)
          6 -> height = ProtoAdapter.INT32.decode(reader)
          else -> UNKNOWN_FIELD
        }
      }

      return Person2(name = name ?: throw Internal.missingRequiredFields(name, "name"),
          id = id ?: throw Internal.missingRequiredFields(id, "id"),
          email = email,
          phone = phone,
          weight = weight ?: throw Internal.missingRequiredFields(weight, "weight"),
          height = height ?: throw Internal.missingRequiredFields(height, "height"),
          unknownFields = unknownFields)
    }
  }
}


fun getAlien() : Person2 {
  val phoneNumber = Person2.PhoneNumber(
      number = "PII",
      phoneType = Person2.PhoneNumber.PhoneType.MOBILE)

  return Person2(
      id = 2,
      name = "Singla",
      phone =  mutableListOf(phoneNumber),
      height = 12,
      weight = 15
  )
}
