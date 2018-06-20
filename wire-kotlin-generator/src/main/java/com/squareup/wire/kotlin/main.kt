import com.squareup.wire.ProtoAdapter
import com.squareup.wire.internal.Internal.missingRequiredFields
import com.squareup.wire.kotlin.UNKNOWN_FIELD
import com.squareup.wire.kotlin.decodeMessage

data class Person(
    val name: kotlin.String,
    val id: kotlin.Int,
    val email: kotlin.String? = null,
    val phone: kotlin.collections.List<Person.PhoneNumber>,
    val unknownFields: okio.ByteString = okio.ByteString.EMPTY
) {
  class Person_ADAPTER : com.squareup.wire.ProtoAdapter<Person>(com.squareup.wire.FieldEncoding.LENGTH_DELIMITED, Person::class.java) {
    override fun encodedSize(value: Person): kotlin.Int =
      ProtoAdapter.STRING.encodedSizeWithTag(1, value.name) +
          ProtoAdapter.INT32.encodedSizeWithTag(2, value.id) +
          ProtoAdapter.STRING.encodedSizeWithTag(3, value.email) +
          PhoneNumber.ADAPTER.asRepeated().encodedSizeWithTag(4, value.phone) +
          value.unknownFields.size()

    override fun encode(writer: com.squareup.wire.ProtoWriter, value: Person) {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
      ProtoAdapter.INT32.encodeWithTag(writer, 2, value.id)
      ProtoAdapter.STRING.encodeWithTag(writer, 3, value.email)
      PhoneNumber.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.phone)
      writer.writeBytes(value.unknownFields)
    }

    override fun decode(reader: com.squareup.wire.ProtoReader): Person {
      var name: kotlin.String? = null
      var id: kotlin.Int? = null
      var email: kotlin.String? = null
      var phone = mutableListOf<Person.PhoneNumber>()
      val unknownFields = reader.decodeMessage {
        tag->
        when(tag) {
          1 -> name = ProtoAdapter.STRING.decode(reader)
          2 -> id = ProtoAdapter.INT32.decode(reader)
          3 -> email = ProtoAdapter.STRING.decode(reader)
          4 -> phone.add(PhoneNumber.ADAPTER.decode(reader))
          else -> UNKNOWN_FIELD
        }
      }
      return Person(
            name = name ?: throw missingRequiredFields(name, "name"),
            id = id ?: throw missingRequiredFields(id, "id"),
            email = email,
            phone = phone,
            unknownFields = unknownFields)
    }
  }

  enum class PhoneType(override val value: kotlin.Int) : com.squareup.wire.WireEnum {
    MOBILE,

    HOME,

    WORK;
  }

  data class PhoneNumber(
      val number: kotlin.String,
      val type: PhoneType? = null,
      val unknownFields: okio.ByteString = okio.ByteString.EMPTY
  ) {
    companion object {
      val ADAPTER = object: com.squareup.wire.ProtoAdapter<PhoneNumber>(com.squareup.wire.FieldEncoding.LENGTH_DELIMITED, PhoneNumber::class.java) {
        override fun encodedSize(value: PhoneNumber): kotlin.Int =
            ProtoAdapter.STRING.encodedSizeWithTag(1, value.number) +
                PhoneType.ADAPTER.encodedSizeWithTag(2, value.type) +
                value.unknownFields.size()

        override fun encode(writer: com.squareup.wire.ProtoWriter, value: PhoneNumber) {
          ProtoAdapter.STRING.encodeWithTag(writer, 1, value.number)
          PhoneType.ADAPTER.encodeWithTag(writer, 2, value.type)
          writer.writeBytes(value.unknownFields)
        }

        override fun decode(reader: com.squareup.wire.ProtoReader): PhoneNumber {
          var number: kotlin.String? = null
          var type: Person.PhoneType? = null
          val unknownFields = reader.decodeMessage {
            tag->
            when(tag) {
              1 -> number = ProtoAdapter.STRING.decode(reader)
              2 -> type = PhoneType.ADAPTER.decode(reader)
              else -> UNKNOWN_FIELD
            }
          }
          return PhoneNumber(
              number = number ?: throw missingRequiredFields(number, "number"),
              type = type,
              unknownFields = unknownFields)
        }
      }
    }
  }
  companion object {
    val ADAPTER: Person_ADAPTER = Person.Person_ADAPTER()
  }
}