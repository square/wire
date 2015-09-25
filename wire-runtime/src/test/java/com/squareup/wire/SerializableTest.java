/*
 * Copyright 2015 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire;

import com.squareup.wire.protos.edgecases.NoFields;
import com.squareup.wire.protos.person.Person;
import com.squareup.wire.protos.simple.SimpleMessage;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import okio.Buffer;
import okio.ByteString;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SerializableTest {

  @Test public void simple() throws Exception {
    SimpleMessage message = new SimpleMessage.Builder().required_int32(42).build();
    assertThat(deserialize(serialize(message))).isEqualTo(message);
  }

  @Test public void nestedMessage() throws Exception {
    Person person = new Person.Builder()
        .name("Omar")
        .id(1234)
        .email("omar@wire.com")
        .phone(Arrays.asList(new Person.PhoneNumber.Builder()
            .number("410-555-0909")
            .type(Person.PhoneType.MOBILE)
            .build()))
        .build();
    assertThat(deserialize(serialize(person))).isEqualTo(person);
  }

  @Test public void noFields() throws Exception {
    NoFields noFields = new NoFields();
    assertThat(deserialize(serialize(noFields))).isEqualTo(noFields);
  }

  @Test public void decodeGolden() throws Exception {
    SimpleMessage goldenValue = new SimpleMessage.Builder()
        .required_int32(99)
        .result("tacos")
        .build();
    ByteString goldenSerialized = ByteString.decodeBase64("rO0ABXNyACdjb20uc3F1YXJldXAud2lyZS5NZXNz"
        + "YWdlU2VyaWFsaXplZEZvcm0AAAAAAAAAAAIAAlsABWJ5dGVzdAACW0JMAAxtZXNzYWdlQ2xhc3N0ABFMamF2YS9s"
        + "YW5nL0NsYXNzO3hwdXIAAltCrPMX+AYIVOACAAB4cAAAAAkoY1IFdGFjb3N2cgAtY29tLnNxdWFyZXVwLndpcmUu"
        + "cHJvdG9zLnNpbXBsZS5TaW1wbGVNZXNzYWdlAAAAAAAAAAACAAxMABRkZWZhdWx0X2ZvcmVpZ25fZW51bXQALkxj"
        + "b20vc3F1YXJldXAvd2lyZS9wcm90b3MvZm9yZWlnbi9Gb3JlaWduRW51bTtMABNkZWZhdWx0X25lc3RlZF9lbnVt"
        + "dAA6TGNvbS9zcXVhcmV1cC93aXJlL3Byb3Rvcy9zaW1wbGUvU2ltcGxlTWVzc2FnZSROZXN0ZWRFbnVtO0wAF25v"
        + "X2RlZmF1bHRfZm9yZWlnbl9lbnVtcQB+AAdMAAFvdAASTGphdmEvbGFuZy9TdHJpbmc7TAAVb3B0aW9uYWxfZXh0"
        + "ZXJuYWxfbXNndAAxTGNvbS9zcXVhcmV1cC93aXJlL3Byb3Rvcy9zaW1wbGUvRXh0ZXJuYWxNZXNzYWdlO0wADm9w"
        + "dGlvbmFsX2ludDMydAATTGphdmEvbGFuZy9JbnRlZ2VyO0wAE29wdGlvbmFsX25lc3RlZF9tc2d0AD1MY29tL3Nx"
        + "dWFyZXVwL3dpcmUvcHJvdG9zL3NpbXBsZS9TaW1wbGVNZXNzYWdlJE5lc3RlZE1lc3NhZ2U7TAAFb3RoZXJxAH4A"
        + "CUwACHBhY2thZ2VfcQB+AAlMAA9yZXBlYXRlZF9kb3VibGV0ABBMamF2YS91dGlsL0xpc3Q7TAAOcmVxdWlyZWRf"
        + "aW50MzJxAH4AC0wABnJlc3VsdHEAfgAJeHIAGWNvbS5zcXVhcmV1cC53aXJlLk1lc3NhZ2UAAAAAAAAAAAIAAHhw"
    );
    assertThat(deserialize(goldenSerialized)).isEqualTo(goldenValue);
    assertThat(serialize(goldenValue)).isEqualTo(goldenSerialized);
  }

  private static ByteString serialize(Message message) throws Exception {
    Buffer buffer = new Buffer();
    ObjectOutputStream stream = new ObjectOutputStream(buffer.outputStream());
    stream.writeObject(message);
    stream.flush();
    return buffer.readByteString();
  }

  public static Object deserialize(ByteString data) throws Exception {
    Buffer buffer = new Buffer().write(data);
    ObjectInputStream stream = new ObjectInputStream(buffer.inputStream());
    return stream.readObject();
  }
}
