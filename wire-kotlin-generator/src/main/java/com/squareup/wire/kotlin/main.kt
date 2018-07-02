import android.os.Bundle
import android.os.Parcel
import com.squareup.wire.EnumAdapter
import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireEnum
import com.squareup.wire.internal.Internal.missingRequiredFields
import com.squareup.wire.kotlin.JavaPerson
import com.squareup.wire.kotlin.Person2
import com.squareup.wire.kotlin.UnkownFieldsBuilder
import com.squareup.wire.kotlin.decodeMessage
import com.squareup.wire.kotlin.getAlien
import com.squareup.wire.protos.person.PersonAndroid
import okio.ByteString
import android.content.Intent



data class Person(
    val name: String,
    val id: Int,
    val email: String? = null,
    val phone: List<PhoneNumber>,
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
            else -> UnkownFieldsBuilder.UNKNOWN_FIELD
          }
        }

        return PhoneNumber(number = number ?: throw missingRequiredFields(number, "number"),
            phoneType = phoneType ?: throw missingRequiredFields(phoneType, "phoneType"),
            unknownFields = unknownFields
        )
      }
    }
  }

  class Person_ADAPTER : ProtoAdapter<Person>(FieldEncoding.LENGTH_DELIMITED, Person::class.java) {

    override fun encodedSize(value: Person): Int {
      return (ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
          + ProtoAdapter.INT32.encodedSizeWithTag(2, value.id)
          + ProtoAdapter.STRING.encodedSizeWithTag(3, value.email)
          + PhoneNumber.ADAPTER.asRepeated().encodedSizeWithTag(4, value.phone)
          + value.unknownFields.size())
    }

    override fun encode(writer: ProtoWriter, value: Person) {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
      ProtoAdapter.INT32.encodeWithTag(writer, 2, value.id)
      ProtoAdapter.STRING.encodeWithTag(writer, 3, value.email)
      PhoneNumber.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.phone)
      writer.writeBytes(value.unknownFields)
    }

    override fun decode(reader: ProtoReader): Person {
      var name: String? = null
      var id: Int? = null
      var email: String? = null
      var phone = mutableListOf<Person.PhoneNumber>()


      val unknownFields = reader.decodeMessage { tag ->
        when (tag) {
          1 -> name = ProtoAdapter.STRING.decode(reader)
          2 -> id = ProtoAdapter.INT32.decode(reader)
          3 -> email = ProtoAdapter.STRING.decode(reader)
          4 -> phone.add(PhoneNumber.ADAPTER.decode(reader))
          else -> UnkownFieldsBuilder.UNKNOWN_FIELD
        }
      }

      return Person(name = name ?: throw missingRequiredFields(name, "name"),
          id = id ?: throw missingRequiredFields(id, "id"),
          email = email,
          phone = phone,
          unknownFields = unknownFields)
    }
  }
}

fun test() {
  val phoneNumber = Person.PhoneNumber(
      number = "PII",
      phoneType = Person.PhoneNumber.PhoneType.MOBILE)

  val person1 = Person(
      id = 1,
      name = "ashutosh",
      phone = mutableListOf(phoneNumber))


  val phoneNumberAdapter = ProtoAdapter.get(Person.PhoneNumber::class.java)
  val encoded = phoneNumberAdapter.encode(phoneNumber)
  val decoded = phoneNumberAdapter.decode(encoded)
  println(phoneNumber == decoded)

  val personAdapter = ProtoAdapter.get(Person::class.java)
  val encoded1 = personAdapter.encode(person1)
  val decoded1 = personAdapter.decode(encoded1)
  println(person1 == decoded1)

  // Test unknown fields
  val alien = getAlien()
  val alienAdapter = ProtoAdapter.get(Person2::class.java)
  val encoded2 = alienAdapter.encode(alien)
  val decoded2 = alienAdapter.decode(encoded2)
  println(alien == decoded2)


  val mutation = personAdapter.decode(encoded2)
  println(mutation)

  println(alien == alienAdapter.decode(personAdapter.encode(mutation)))

  val superman = mutation.copy(name = "clark kent")
  println(alienAdapter.decode(personAdapter.encode(superman)))

  // Need to able access the java Person class
  val javaPersonAdapter = ProtoAdapter.get(JavaPerson::class.java)
  println(javaPersonAdapter.decode(encoded1))


}

//fun testAndroid() {
//  val personAndroid: PersonAndroid = getPerson()
//  val creator = PersonAndroid.CREATOR
//
//  val intent = Intent()
//  intent.putExtra("PERSON", personAndroid)
//
//  intent.getBundleExtra("PERSON")
//}

fun getPerson(): PersonAndroid {
  val pp : PersonAndroid.PhoneNumber = PersonAndroid.PhoneNumber(number = "90992121",
      type = PersonAndroid.PhoneType.MOBILE)
  return PersonAndroid(name = "ashutosh", id = 1, phone = arrayListOf(pp))
}


fun main(args: Array<String>) {
 // test()
  testAndroid()
}
