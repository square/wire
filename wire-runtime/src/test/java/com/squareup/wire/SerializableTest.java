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

import static org.junit.Assert.assertEquals;

public class SerializableTest {

  @Test public void testSimpleSerializable() throws Exception {
    SimpleMessage message = new SimpleMessage.Builder().required_int32(42).build();
    assertEquals(message, serializeDeserialize(message));
  }

  @Test public void testNestedMessageSerializable() throws Exception {
    Person person = new Person.Builder()
        .name("Omar")
        .id(1234)
        .email("omar@wire.com")
        .phone(Arrays.asList(new Person.PhoneNumber.Builder()
            .number("410-555-0909")
            .type(Person.PhoneType.MOBILE)
            .build()))
        .build();
    assertEquals(person, serializeDeserialize(person));
  }

  @Test public void testNoFieldsSerializable() throws Exception {
    NoFields noFields = new NoFields();
    assertEquals(noFields, serializeDeserialize(noFields));
  }

  @Test public void decodeGolden() throws Exception {
    SimpleMessage goldenValue = new SimpleMessage.Builder()
        .required_int32(99)
        .result("tacos")
        .build();
    ByteString goldenSerialized = ByteString.decodeBase64("rO0ABXNyACdjb20uc3F1YXJldXAud2lyZS5NZXNz"
        + "YWdlU2VyaWFsaXplZEZvcm0AAAAAAAAAAAIAAlsABWJ5dGVzdAACW0JMAAxtZXNzYWdlQ2xhc3N0ABFMamF2YS9s"
        + "YW5nL0NsYXNzO3hwdXIAAltCrPMX+AYIVOACAAB4cAAAAAkoY1IFdGFjb3N2cgAtY29tLnNxdWFyZXVwLndpcmUu"
        + "cHJvdG9zLnNpbXBsZS5TaW1wbGVNZXNzYWdlAAAAAAAAAAACAAxMAAhfcGFja2FnZXQAEkxqYXZhL2xhbmcvU3Ry"
        + "aW5nO0wAFGRlZmF1bHRfZm9yZWlnbl9lbnVtdAAuTGNvbS9zcXVhcmV1cC93aXJlL3Byb3Rvcy9mb3JlaWduL0Zv"
        + "cmVpZ25FbnVtO0wAE2RlZmF1bHRfbmVzdGVkX2VudW10ADpMY29tL3NxdWFyZXVwL3dpcmUvcHJvdG9zL3NpbXBs"
        + "ZS9TaW1wbGVNZXNzYWdlJE5lc3RlZEVudW07TAAXbm9fZGVmYXVsdF9mb3JlaWduX2VudW1xAH4ACEwAAW9xAH4A"
        + "B0wAFW9wdGlvbmFsX2V4dGVybmFsX21zZ3QAMUxjb20vc3F1YXJldXAvd2lyZS9wcm90b3Mvc2ltcGxlL0V4dGVy"
        + "bmFsTWVzc2FnZTtMAA5vcHRpb25hbF9pbnQzMnQAE0xqYXZhL2xhbmcvSW50ZWdlcjtMABNvcHRpb25hbF9uZXN0"
        + "ZWRfbXNndAA9TGNvbS9zcXVhcmV1cC93aXJlL3Byb3Rvcy9zaW1wbGUvU2ltcGxlTWVzc2FnZSROZXN0ZWRNZXNz"
        + "YWdlO0wABW90aGVycQB+AAdMAA9yZXBlYXRlZF9kb3VibGV0ABBMamF2YS91dGlsL0xpc3Q7TAAOcmVxdWlyZWRf"
        + "aW50MzJxAH4AC0wABnJlc3VsdHEAfgAHeHIAGWNvbS5zcXVhcmV1cC53aXJlLk1lc3NhZ2UAAAAAAAAAAAIAAHhw"
    );
    Buffer buffer = new Buffer();
    buffer.write(goldenSerialized);
    assertEquals(goldenValue, new ObjectInputStream(buffer.inputStream()).readObject());
  }

  private static Object serializeDeserialize(Message message) throws Exception {
    Buffer buffer = new Buffer();
    new ObjectOutputStream(buffer.outputStream()).writeObject(message);
    return new ObjectInputStream(buffer.inputStream()).readObject();
  }
}
