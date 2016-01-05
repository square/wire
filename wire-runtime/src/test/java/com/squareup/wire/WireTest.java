/*
 * Copyright 2013 Square Inc.
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

import com.squareup.wire.protos.RepeatedAndPacked;
import com.squareup.wire.protos.edgecases.NoFields;
import com.squareup.wire.protos.person.Person;
import com.squareup.wire.protos.person.Person.PhoneNumber;
import com.squareup.wire.protos.person.Person.PhoneType;
import com.squareup.wire.protos.simple.ExternalMessage;
import com.squareup.wire.protos.simple.SimpleMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import okio.Buffer;
import okio.ByteString;
import org.junit.Test;
import squareup.protos.extension_collision.CollisionSubject;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Test Wire runtime.
 */
public class WireTest {

  @Test
  public void testSimpleMessage() throws Exception {
    SimpleMessage msg = new SimpleMessage.Builder().required_int32(456).build();
    assertThat(msg.optional_int32).isNull();
    assertThat(msg.optional_nested_msg).isNull();
    assertThat(msg.optional_external_msg).isNull();
    assertThat(msg.default_nested_enum).isNull();
    assertThat(msg.required_int32).isEqualTo(new Integer(456));
    assertThat(msg.repeated_double).isNotNull();
    assertThat(msg.repeated_double).hasSize(0);

    SimpleMessage.Builder builder = new SimpleMessage.Builder();
    builder.optional_int32(789);
    SimpleMessage.NestedMessage.Builder nested_msg_builder =
        new SimpleMessage.NestedMessage.Builder();
    nested_msg_builder.bb(2);
    builder.optional_nested_msg(nested_msg_builder.build());
    ExternalMessage.Builder external_msg_builder =
        new ExternalMessage.Builder();
    external_msg_builder.f(99.9f);
    builder.optional_external_msg(external_msg_builder.build());
    builder.default_nested_enum(SimpleMessage.NestedEnum.BAR);
    builder.required_int32(456);
    List<Double> doubles = Arrays.asList(1.0, 2.0, 3.0);
    builder.repeated_double(doubles);

    msg = builder.build();
    assertThat(msg.optional_int32).isEqualTo(new Integer(789));
    assertThat(msg.optional_nested_msg.bb).isEqualTo(new Integer(2));
    assertThat(msg.optional_external_msg.f).isEqualTo(new Float(99.9f));
    assertThat(msg.default_nested_enum).isEqualTo(SimpleMessage.NestedEnum.BAR);
    assertThat(msg.required_int32).isEqualTo(new Integer(456));
    assertThat(msg.repeated_double).isEqualTo(doubles);

    // Modifying the builder list does not affect an already-built message
    List<Double> savedData = new ArrayList<Double>(msg.repeated_double);
    doubles.set(1, 1.1);
    assertThat(msg.repeated_double).isNotSameAs(doubles);
    assertThat(msg.repeated_double).isEqualTo(savedData);

    // Rebuilding will use the new list
    msg = builder.build();
    assertThat(msg.repeated_double).isEqualTo(doubles);

    // Check for required fields
    builder.required_int32(null);
    try {
      builder.build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Required field not set:\n  required_int32");
    }

    // The message list is immutable
    try {
      msg.repeated_double.set(0, 0.0);
      fail();
    } catch (UnsupportedOperationException e) {
      // expected
    }

    ProtoAdapter<SimpleMessage> adapter = SimpleMessage.ADAPTER;

    byte[] result = adapter.encode(msg);
    assertThat(result.length).isEqualTo(46);

    SimpleMessage newMsg = adapter.decode(result);
    assertThat(newMsg.optional_int32).isEqualTo(new Integer(789));
    assertThat(newMsg.optional_nested_msg.bb).isEqualTo(new Integer(2));
    assertThat(newMsg.optional_external_msg.f).isEqualTo(new Float(99.9F));
    assertThat(newMsg.default_nested_enum).isEqualTo(SimpleMessage.NestedEnum.BAR);
    assertThat(newMsg.required_int32).isEqualTo(new Integer(456));
    assertThat(msg.repeated_double).isEqualTo(doubles);
  }

  @Test public void mutateBuilder() throws Exception {
    SimpleMessage message = new SimpleMessage.Builder()
        .required_int32(10)
        .build();

    SimpleMessage.Builder builder = message.newBuilder();
    builder.required_int32 = 20;
    builder.repeated_double.add(30.5);
    builder.optional_int32 = 40;

    assertThat(builder.build()).isEqualTo(new SimpleMessage.Builder()
        .required_int32(20)
        .repeated_double(Arrays.asList(30.5))
        .optional_int32(40)
        .build());
  }

  @Test
  public void testPerson() throws IOException {
    Person person = new Person.Builder()
        .name("Omar")
        .id(1234)
        .email("omar@wire.com")
        .phone(Arrays.asList(new PhoneNumber.Builder()
            .number("410-555-0909")
            .type(PhoneType.MOBILE)
            .build()))
        .build();

    ProtoAdapter<Person> adapter = Person.ADAPTER;

    byte[] data = adapter.encode(person);
    adapter.decode(data);
  }

  @Test
  public void testExtensions() throws Exception {
    ExternalMessage optional_external_msg = new ExternalMessage.Builder()
        .fooext(Arrays.asList(444, 555))
        .barext(333)
        .bazext(222)
        .nested_message_ext(new SimpleMessage.NestedMessage.Builder().bb(77).build())
        .nested_enum_ext(SimpleMessage.NestedEnum.BAZ)
        .build();

    SimpleMessage msg = new SimpleMessage.Builder()
        .optional_external_msg(optional_external_msg)
        .required_int32(456)
        .build();

    assertThat(msg.optional_external_msg.fooext).containsExactly(444, 555);
    assertThat(msg.optional_external_msg.barext).isEqualTo(new Integer(333));
    assertThat(msg.optional_external_msg.bazext).isEqualTo(new Integer(222));
    assertThat(msg.optional_external_msg.nested_message_ext.bb)
        .isEqualTo(new Integer(77));
    assertThat(msg.optional_external_msg.nested_enum_ext).isEqualTo(SimpleMessage.NestedEnum.BAZ);

    ProtoAdapter<SimpleMessage> adapter = SimpleMessage.ADAPTER;

    byte[] result = adapter.encode(msg);
    assertThat(result.length).isEqualTo(29);

    SimpleMessage newMsg = adapter.decode(result);
    assertThat(newMsg.optional_external_msg.fooext).containsExactly(444, 555);
    assertThat(newMsg.optional_external_msg.barext).isEqualTo(new Integer(333));
    assertThat(newMsg.optional_external_msg.bazext).isEqualTo(new Integer(222));
  }

  @Test
  public void testExtensionEnum() throws Exception {
    ExternalMessage optional_external_msg = new ExternalMessage.Builder()
        .nested_enum_ext(SimpleMessage.NestedEnum.BAZ)
        .build();

    SimpleMessage msg = new SimpleMessage.Builder()
        .optional_external_msg(optional_external_msg)
        .required_int32(456)
        .build();

    ProtoAdapter<SimpleMessage> adapter = SimpleMessage.ADAPTER;

    byte[] data = adapter.encode(msg);

    // Change BAZ enum to a value not known by this client.
    data[4] = 17;

    // Parse the altered message.
    SimpleMessage newMsg = adapter.decode(data);

    // Original value shows up as an extension.
    assertThat(msg.toString()).contains("nested_enum_ext=BAZ");
    // New value is unknown in the tag map.
    ProtoReader reader = new ProtoReader(new Buffer().write(
        newMsg.optional_external_msg.unknownFields()));
    reader.beginMessage();
    assertThat(reader.nextTag()).isEqualTo(129);
    assertThat(reader.peekFieldEncoding().rawProtoAdapter().decode(reader)).isEqualTo(17L);

    // Serialized outputs are the same.
    byte[] newData = adapter.encode(newMsg);
    assertThat(data).isEqualTo(newData);
  }

  @Test
  public void testExtensionsNoRegistry() throws Exception {
    ExternalMessage optional_external_msg =
        new ExternalMessage.Builder()
            .fooext(Arrays.asList(444, 555))
            .barext(333)
            .bazext(222)
            .build();

    SimpleMessage msg = new SimpleMessage.Builder()
        .optional_external_msg(optional_external_msg)
        .required_int32(456)
        .build();

    assertThat(msg.optional_external_msg.fooext).containsExactly(444, 555);
    assertThat(msg.optional_external_msg.barext).isEqualTo(new Integer(333));
    assertThat(msg.optional_external_msg.bazext).isEqualTo(new Integer(222));

    ProtoAdapter<SimpleMessage> adapter = SimpleMessage.ADAPTER;

    byte[] result = adapter.encode(msg);
    assertThat(result.length).isEqualTo(21);

    SimpleMessage newMsg = adapter.decode(result);
    assertThat(newMsg.optional_external_msg.fooext).isEqualTo(Arrays.asList(444, 555));
    assertThat(newMsg.optional_external_msg.barext).isEqualTo(333);
    assertThat(newMsg.optional_external_msg.bazext).isEqualTo(222);
  }

  @Test
  public void testEmptyList() throws IOException {
    SimpleMessage noListMessage = new SimpleMessage.Builder()
        .required_int32(1)
        .build();
    SimpleMessage emptyListMessage = new SimpleMessage.Builder()
        .required_int32(1)
        .repeated_double(new ArrayList<Double>())
        .build();

    assertThat(noListMessage.repeated_double).isNotNull();
    assertThat(noListMessage.repeated_double).hasSize(0);
    assertThat(emptyListMessage.repeated_double).isNotNull();
    assertThat(emptyListMessage.repeated_double).hasSize(0);

    // Empty lists and null lists compare as equals()
    assertThat(noListMessage).isEqualTo(emptyListMessage);

    // Empty lists and null lists have the same hashCode()
    int noListHashCode = noListMessage.hashCode();
    int emptyListHashCode = emptyListMessage.hashCode();
    assertThat(noListHashCode).isEqualTo(emptyListHashCode);

    ProtoAdapter<SimpleMessage> adapter = SimpleMessage.ADAPTER;

    // Empty lists and null lists occupy 0 bytes in the wire encoding
    byte[] noListBytes = adapter.encode(noListMessage);
    byte[] emptyListBytes = adapter.encode(emptyListMessage);

    assertThat(noListBytes.length).isEqualTo(2);
    assertThat(emptyListBytes.length).isEqualTo(2);

    assertThat(emptyListBytes[0]).isEqualTo(noListBytes[0]);
    assertThat(emptyListBytes[1]).isEqualTo(noListBytes[1]);

    // Parsed results have a null list
    SimpleMessage noListParsed = adapter.decode(noListBytes);
    SimpleMessage emptyListParsed = adapter.decode(emptyListBytes);

    assertThat(noListParsed.repeated_double).isNotNull();
    assertThat(noListParsed.repeated_double).hasSize(0);
    assertThat(emptyListParsed.repeated_double).isNotNull();
    assertThat(emptyListParsed.repeated_double).hasSize(0);
  }

  @Test
  public void testBadEnum() throws IOException {
    Person person = new Person.Builder()
        .id(1)
        .name("Joe Schmoe")
        .phone(Arrays.asList(
            new PhoneNumber.Builder().number("555-1212").type(PhoneType.WORK).build()))
        .build();

    assertThat(person.phone.get(0).type).isEqualTo(PhoneType.WORK);

    ProtoAdapter<Person> adapter = Person.ADAPTER;

    byte[] data = adapter.encode(person);
    assertThat(data[27]).isEqualTo((byte) PhoneType.WORK.getValue());

    // Corrupt the PhoneNumber type field with an undefined value
    data[27] = 17;
    // Parsed PhoneNumber has no value set
    Person result = adapter.decode(data);
    assertThat(result.phone.get(0).type).isNull();

    // The value 17 will be stored as an unknown varint with tag number 2
    ByteString unknownFields = result.phone.get(0).unknownFields();
    ProtoReader reader = new ProtoReader(new Buffer().write(unknownFields));
    long token = reader.beginMessage();
    assertThat(reader.nextTag()).isEqualTo(2);
    assertThat(reader.peekFieldEncoding()).isEqualTo(FieldEncoding.VARINT);
    assertThat(FieldEncoding.VARINT.rawProtoAdapter().decode(reader)).isEqualTo(17L);
    assertThat(reader.nextTag()).isEqualTo(-1);
    reader.endMessage(token);

    // Serialize again, value is preserved
    byte[] newData = adapter.encode(result);
    assertThat(data).isEqualTo(newData);
  }

  @Test public void missingRepeatedAndPackedFieldsBecomesEmptyList() throws IOException {
    byte[] bytes = new byte[0];
    RepeatedAndPacked data = RepeatedAndPacked.ADAPTER.decode(bytes);
    assertThat(data.rep_int32).isEqualTo(Collections.emptyList());
    assertThat(data.pack_int32).isEqualTo(Collections.emptyList());
  }

  @Test public void unmodifiedMutableListReusesImmutableInstance() {
    PhoneNumber phone = new PhoneNumber.Builder().number("555-1212").type(PhoneType.WORK).build();
    Person personWithPhone = new Person.Builder()
        .id(1)
        .name("Joe Schmoe")
        .phone(singletonList(phone))
        .build();
    Person personNoPhone = new Person.Builder()
        .id(1)
        .name("Joe Schmoe")
        .build();
    try {
      personWithPhone.phone.set(0, null);
      fail();
    } catch (UnsupportedOperationException expected) {
    }
    assertThat(personNoPhone.phone).isSameAs(Collections.emptyList());

    // Round-trip these instances through the builder and ensure the lists are the same instances.
    assertThat(personWithPhone.newBuilder().build().phone).isSameAs(personWithPhone.phone);
    assertThat(personNoPhone.newBuilder().build().phone).isSameAs(personNoPhone.phone);
  }

  @Test public void listMustBeNonNull() {
    Person.Builder builder = new Person.Builder();
    builder.id = 1;
    builder.name = "Joe Schmoe";
    builder.phone = null;
    try {
      builder.build();
      fail();
    } catch (NullPointerException expected) {
      assertThat(expected).hasMessage("phone == null");
    }
  }

  @Test public void listElementsMustBeNonNull() {
    Person.Builder builder = new Person.Builder();
    builder.id = 1;
    builder.name = "Joe Schmoe";
    builder.phone.add(null);
    try {
      builder.build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("phone.contains(null)");
    }
  }

  @Test public void builderListsAreAlwaysMutable() {
    PhoneNumber phone = new PhoneNumber.Builder().number("555-1212").type(PhoneType.WORK).build();

    Person.Builder newBuilder = new Person.Builder();
    newBuilder.phone.add(phone);

    Person person = new Person.Builder()
        .id(1)
        .name("Joe Schmoe")
        .phone(singletonList(phone))
        .build();
    Person.Builder copyBuilder = person.newBuilder();
    copyBuilder.phone.add(phone);
  }

  @Test public void emptyMessageToString() {
    NoFields empty = new NoFields();
    assertThat(empty.toString()).isEqualTo("NoFields{}");
  }

  @Test
  public void extensionNameCollisions() throws Exception {
    assertThat(CollisionSubject.FIELD_OPTIONS_F.squareup_protos_extension_collision_1_a)
        .isEqualTo("1a");
    assertThat(CollisionSubject.FIELD_OPTIONS_F.b)
        .isEqualTo("1b");
    assertThat(CollisionSubject.FIELD_OPTIONS_F.squareup_protos_extension_collision_2_a)
        .isEqualTo("2a");
    assertThat(CollisionSubject.FIELD_OPTIONS_F.c)
        .isEqualTo("2c");
  }
}
