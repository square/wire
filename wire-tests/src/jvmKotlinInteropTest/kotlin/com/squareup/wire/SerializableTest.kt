/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.message
import com.squareup.wire.protos.kotlin.NoFields
import com.squareup.wire.protos.kotlin.person.Person
import com.squareup.wire.protos.kotlin.simple.SimpleMessage
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import org.junit.Test

class SerializableTest {
  @Test fun simple() {
    val message = SimpleMessage.Builder().required_int32(42).build()
    assertThat(deserialize(serialize(message))).isEqualTo(message)
  }

  @Test fun nestedMessage() {
    val person = Person.Builder()
      .name("Omar")
      .id(1234)
      .email("omar@wire.com")
      .phone(
        listOf(
          Person.PhoneNumber.Builder()
            .number("410-555-0909")
            .type(Person.PhoneType.MOBILE)
            .build(),
        ),
      )
      .build()
    assertThat(deserialize(serialize(person))).isEqualTo(person)
  }

  @Test fun noFields() {
    val noFields = NoFields()
    assertThat(deserialize(serialize(noFields))).isEqualTo(noFields)
  }

  @Test fun decodeGolden() {
    val goldenValue = SimpleMessage.Builder()
      .required_int32(99)
      .result("tacos")
      .build()
    val goldenSerialized = (
      "rO0ABXNyACdjb20uc3F1YXJldXAud2lyZS5NZXNzYWdlU2VyaWFsaXplZEZvcm0AAAAA" +
        "AAAAAAIAAlsABWJ5dGVzdAACW0JMAAxtZXNzYWdlQ2xhc3N0ABFMamF2YS9sYW5nL0NsYXNzO3hwdXIAAltCrPMX" +
        "+AYIVOACAAB4cAAAAAkoY1IFdGFjb3N2cgA0Y29tLnNxdWFyZXVwLndpcmUucHJvdG9zLmtvdGxpbi5zaW1wbGUu" +
        "U2ltcGxlTWVzc2FnZQAAAAAAAAAAAgAMSQAOcmVxdWlyZWRfaW50MzJMABRkZWZhdWx0X2ZvcmVpZ25fZW51bXQA" +
        "NUxjb20vc3F1YXJldXAvd2lyZS9wcm90b3Mva290bGluL2ZvcmVpZ24vRm9yZWlnbkVudW07TAATZGVmYXVsdF9u" +
        "ZXN0ZWRfZW51bXQAQUxjb20vc3F1YXJldXAvd2lyZS9wcm90b3Mva290bGluL3NpbXBsZS9TaW1wbGVNZXNzYWdl" +
        "JE5lc3RlZEVudW07TAAXbm9fZGVmYXVsdF9mb3JlaWduX2VudW1xAH4AB0wAAW90ABJMamF2YS9sYW5nL1N0cmlu" +
        "ZztMABVvcHRpb25hbF9leHRlcm5hbF9tc2d0ADhMY29tL3NxdWFyZXVwL3dpcmUvcHJvdG9zL2tvdGxpbi9zaW1w" +
        "bGUvRXh0ZXJuYWxNZXNzYWdlO0wADm9wdGlvbmFsX2ludDMydAATTGphdmEvbGFuZy9JbnRlZ2VyO0wAE29wdGlv" +
        "bmFsX25lc3RlZF9tc2d0AERMY29tL3NxdWFyZXVwL3dpcmUvcHJvdG9zL2tvdGxpbi9zaW1wbGUvU2ltcGxlTWVz" +
        "c2FnZSROZXN0ZWRNZXNzYWdlO0wABW90aGVycQB+AAlMAAhwYWNrYWdlX3EAfgAJTAAPcmVwZWF0ZWRfZG91Ymxl" +
        "dAAQTGphdmEvdXRpbC9MaXN0O0wABnJlc3VsdHEAfgAJeHIAGWNvbS5zcXVhcmV1cC53aXJlLk1lc3NhZ2UAAAAA" +
        "AAAAAAIAAHhw"
      ).decodeBase64()
    assertThat(deserialize(goldenSerialized!!)).isEqualTo(goldenValue)
    assertThat(serialize(goldenValue)).isEqualTo(goldenSerialized)
  }

  companion object {
    private fun serialize(message: Message<*, *>): ByteString {
      val buffer = Buffer()
      val stream = ObjectOutputStream(buffer.outputStream())
      stream.writeObject(message)
      stream.flush()
      return buffer.readByteString()
    }

    private fun deserialize(data: ByteString): Any? {
      val buffer = Buffer().write(data)
      val stream = ObjectInputStream(buffer.inputStream())
      return stream.readObject()
    }
  }
}
