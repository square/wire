import human.Person

fun main() {
  val phoneNumber = Person.PhoneNumber.Builder()
    .area("519")
    .number("5550202")
    .build()
  val person = Person(name = "Jacques")
  println("Hello, Kotlin/Native! Here is ${person.name} and their number: ${person.phone_number}")
}
