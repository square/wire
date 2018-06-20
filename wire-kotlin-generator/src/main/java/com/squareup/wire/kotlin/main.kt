import com.squareup.wire.kotlin.decodeMessage

data class Person(
    val name: kotlin.String,
    val id: kotlin.Int,
    val email: kotlin.String? = null,
    val phone: kotlin.collections.List<PhoneNumber>,
    val unknownFields: okio.ByteString = okio.ByteString.EMPTY
) {
  class Person_ADAPTER : com.squareup.wire.ProtoAdapter<Person>(com.squareup.wire.FieldEncoding.LENGTH_DELIMITED, Person::class.java) {
    override fun encodedSize(value: Person): kotlin.Int = com.squareup.wire.ProtoAdapter.STRING.encodedSizeWithTag(1, value.name) +
        com.squareup.wire.ProtoAdapter.INT32.encodedSizeWithTag(2, value.id) +
        com.squareup.wire.ProtoAdapter.STRING.encodedSizeWithTag(3, value.email) +
        PhoneNumber.ADAPTER.asRepeated().encodedSizeWithTag(4, value.phone) +
        value.unknownFields.size()
    override fun encode(writer: com.squareup.wire.ProtoWriter, value: Person) {
      com.squareup.wire.ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
      com.squareup.wire.ProtoAdapter.INT32.encodeWithTag(writer, 2, value.id)
      com.squareup.wire.ProtoAdapter.STRING.encodeWithTag(writer, 3, value.email)
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
          1 -> name = com.squareup.wire.ProtoAdapter.STRING.decode(reader)
          2 -> id = com.squareup.wire.ProtoAdapter.INT32.decode(reader)
          3 -> email = com.squareup.wire.ProtoAdapter.STRING.decode(reader)
          4 -> phone.add(PhoneNumber.ADAPTER.decode(reader))
          else -> com.squareup.wire.kotlin.UnkownFieldsBuilder.Companion.UNKNOWN_FIELD
        }
      }
      return Person(
          name = name ?: throw com.squareup.wire.internal.Internal.missingRequiredFields(name, "name"),
          id = id ?: throw com.squareup.wire.internal.Internal.missingRequiredFields(id, "id"),
          email = email,
          phone = phone,
          unknownFields = unknownFields)
    }
  }

  companion object {
    @kotlin.jvm.JvmField
    val ADAPTER: Person_ADAPTER = Person.Person_ADAPTER()
  }

  enum class PhoneType(private val value: kotlin.Int) : com.squareup.wire.WireEnum {
    MOBILE(0),

    HOME(1),

    WORK(2);

    override fun getValue(): kotlin.Int = value

    class PhoneType_ADAPTER : com.squareup.wire.EnumAdapter<PhoneType>(Person.PhoneType::class.java) {
      override fun fromValue(value: kotlin.Int): PhoneType? = Person.PhoneType.fromValue(value)
    }

    companion object {
      @kotlin.jvm.JvmField
      val ADAPTER: PhoneType.PhoneType_ADAPTER = PhoneType.PhoneType_ADAPTER()

      fun fromValue(value: kotlin.Int): PhoneType? = values().find { it.value == value }
    }
  }

  data class PhoneNumber(
      val number: kotlin.String,
      val type: PhoneType? = null,
      val unknownFields: okio.ByteString = okio.ByteString.EMPTY
  ) {
    class PhoneNumber_ADAPTER : com.squareup.wire.ProtoAdapter<PhoneNumber>(com.squareup.wire.FieldEncoding.LENGTH_DELIMITED, Person.PhoneNumber::class.java) {
      override fun encodedSize(value: PhoneNumber): kotlin.Int = com.squareup.wire.ProtoAdapter.STRING.encodedSizeWithTag(1, value.number) +
          PhoneType.ADAPTER.encodedSizeWithTag(2, value.type) +
          value.unknownFields.size()
      override fun encode(writer: com.squareup.wire.ProtoWriter, value: PhoneNumber) {
        com.squareup.wire.ProtoAdapter.STRING.encodeWithTag(writer, 1, value.number)
        PhoneType.ADAPTER.encodeWithTag(writer, 2, value.type)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: com.squareup.wire.ProtoReader): PhoneNumber {
        var number: kotlin.String? = null
        var type: Person.PhoneType? = null
        val unknownFields = reader.decodeMessage {
          tag->
          when(tag) {
            1 -> number = com.squareup.wire.ProtoAdapter.STRING.decode(reader)
            2 -> type = PhoneType.ADAPTER.decode(reader)
            else -> com.squareup.wire.kotlin.UnkownFieldsBuilder.Companion.UNKNOWN_FIELD
          }
        }
        return PhoneNumber(
            number = number ?: throw com.squareup.wire.internal.Internal.missingRequiredFields(number, "number"),
            type = type,
            unknownFields = unknownFields)
      }
    }

    companion object {
      @kotlin.jvm.JvmField
      val ADAPTER: PhoneNumber.PhoneNumber_ADAPTER = PhoneNumber.PhoneNumber_ADAPTER()
    }
  }
}
