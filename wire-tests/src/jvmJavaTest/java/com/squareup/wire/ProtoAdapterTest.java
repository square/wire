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

import com.squareup.wire.protos.person.Person;
import java.io.IOException;
import okio.ByteString;
import org.junit.Test;
import squareup.protos.packed_encoding.EmbeddedMessage;
import squareup.protos.packed_encoding.OuterMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public final class ProtoAdapterTest {
  @Test public void getFromClass() throws Exception {
    Person person = new Person.Builder()
        .id(99)
        .name("Omar Little")
        .build();
    ByteString encoded = ByteString.decodeHex("0a0b4f6d6172204c6974746c651063");

    ProtoAdapter<Person> personAdapter = ProtoAdapter.get(Person.class);
    assertThat(ByteString.of(personAdapter.encode(person))).isEqualTo(encoded);
    assertThat(personAdapter.decode(encoded)).isEqualTo(person);
  }

  @Test public void getFromInstanceSameAsFromClass() throws Exception {
    Person person = new Person.Builder()
        .id(99)
        .name("Omar Little")
        .build();

    ProtoAdapter<Person> instanceAdapter = ProtoAdapter.get(person);
    ProtoAdapter<Person> classAdapter = ProtoAdapter.get(Person.class);

    assertThat(instanceAdapter).isSameAs(classAdapter);
  }

  @Test public void repeatedHelpersCacheInstances() {
    ProtoAdapter<?> adapter = ProtoAdapter.UINT64;
    assertThat(adapter.asRepeated()).isSameAs(adapter.asRepeated());
    assertThat(adapter.asPacked()).isSameAs(adapter.asPacked());
  }

  /** https://github.com/square/wire/issues/541 */
  @Test public void embeddedEmptyPackedMessage() throws IOException {
    OuterMessage outerMessage = new OuterMessage.Builder()
        .outer_number_before(2)
        .embedded_message(new EmbeddedMessage.Builder()
            .inner_number_after(1)
            .build())
        .build();
    OuterMessage outerMessagesAfterSerialisation = OuterMessage.ADAPTER.decode(
        OuterMessage.ADAPTER.encode(outerMessage));
    assertEquals(outerMessagesAfterSerialisation, outerMessage);
  }
}
