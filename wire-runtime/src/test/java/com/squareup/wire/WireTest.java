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

import com.squareup.wire.UnknownFieldMap.VarintValue;
import com.squareup.wire.protos.RepeatedAndPacked;
import com.squareup.wire.protos.person.Person;
import com.squareup.wire.protos.person.Person.PhoneNumber;
import com.squareup.wire.protos.person.Person.PhoneType;
import com.squareup.wire.protos.simple.Ext_simple_message;
import com.squareup.wire.protos.simple.ExternalMessage;
import com.squareup.wire.protos.simple.SimpleMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static com.squareup.wire.protos.simple.Ext_simple_message.barext;
import static com.squareup.wire.protos.simple.Ext_simple_message.bazext;
import static com.squareup.wire.protos.simple.Ext_simple_message.fooext;
import static com.squareup.wire.protos.simple.Ext_simple_message.nested_message_ext;
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

    Wire wire = new Wire();
    MessageAdapter<SimpleMessage> adapter = wire.adapter(SimpleMessage.class);

    byte[] result = adapter.writeBytes(msg);
    assertThat(result.length).isEqualTo(46);

    SimpleMessage newMsg = adapter.readBytes(result);
    assertThat(newMsg.optional_int32).isEqualTo(new Integer(789));
    assertThat(newMsg.optional_nested_msg.bb).isEqualTo(new Integer(2));
    assertThat(newMsg.optional_external_msg.f).isEqualTo(new Float(99.9F));
    assertThat(newMsg.default_nested_enum).isEqualTo(SimpleMessage.NestedEnum.BAR);
    assertThat(newMsg.required_int32).isEqualTo(new Integer(456));
    assertThat(msg.repeated_double).isEqualTo(doubles);
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

    Wire wire = new Wire();
    MessageAdapter<Person> adapter = wire.adapter(Person.class);

    byte[] data = adapter.writeBytes(person);
    adapter.readBytes(data);
  }

  @Test
  public void testExtensions() throws Exception {
    ExternalMessage optional_external_msg = new ExternalMessage.Builder()
        .setExtension(fooext, Arrays.asList(444, 555))
        .setExtension(barext, 333)
        .setExtension(bazext, 222)
        .setExtension(nested_message_ext, new SimpleMessage.NestedMessage.Builder().bb(77).build())
        .setExtension(Ext_simple_message.nested_enum_ext, SimpleMessage.NestedEnum.BAZ)
        .build();

    SimpleMessage msg = new SimpleMessage.Builder()
        .optional_external_msg(optional_external_msg)
        .required_int32(456)
        .build();

    assertThat(msg.optional_external_msg.getExtension(fooext)).containsExactly(444, 555);
    assertThat(msg.optional_external_msg.getExtension(barext)).isEqualTo(new Integer(333));
    assertThat(msg.optional_external_msg.getExtension(bazext)).isEqualTo(new Integer(222));
    assertThat(msg.optional_external_msg.getExtension(nested_message_ext).bb)
        .isEqualTo(new Integer(77));
    assertThat(msg.optional_external_msg.getExtension(
        Ext_simple_message.nested_enum_ext)).isEqualTo(SimpleMessage.NestedEnum.BAZ);

    Wire wire = new Wire(Ext_simple_message.class);
    MessageAdapter<SimpleMessage> adapter = wire.adapter(SimpleMessage.class);

    byte[] result = adapter.writeBytes(msg);
    assertThat(result.length).isEqualTo(29);

    SimpleMessage newMsg = adapter.readBytes(result);
    assertThat(newMsg.optional_external_msg.getExtension(fooext)).containsExactly(444, 555);
    assertThat(newMsg.optional_external_msg.getExtension(barext)).isEqualTo(new Integer(333));
    assertThat(newMsg.optional_external_msg.getExtension(bazext)).isEqualTo(new Integer(222));
  }

  @Test
  public void testExtensionEnum() throws Exception {
    ExternalMessage optional_external_msg = new ExternalMessage.Builder()
        .setExtension(Ext_simple_message.nested_enum_ext, SimpleMessage.NestedEnum.BAZ)
        .build();

    SimpleMessage msg = new SimpleMessage.Builder()
        .optional_external_msg(optional_external_msg)
        .required_int32(456)
        .build();

    Wire wireNoExt = new Wire();
    MessageAdapter<SimpleMessage> adapterNoExt = wireNoExt.adapter(SimpleMessage.class);
    Wire wireExt = new Wire(Ext_simple_message.class);
    MessageAdapter<SimpleMessage> adapterExt = wireExt.adapter(SimpleMessage.class);

    byte[] data = adapterNoExt.writeBytes(msg);

    // Change BAZ enum to a value not known by this client.
    data[4] = 17;

    // Parse the altered message.
    SimpleMessage newMsg = adapterExt.readBytes(data);

    // Original value shows up as an extension.
    assertThat(msg.toString()).contains("extensions={129=BAZ}");
    // New value is placed into the unknown field map.
    assertThat(newMsg.toString()).contains("extensions={}");

    // Serialized outputs are the same.
    byte[] newData = adapterExt.writeBytes(newMsg);
    assertThat(data).isEqualTo(newData);
  }

  @Test
  public void extensionToString() {
    assertThat(Ext_simple_message.fooext.toString()).isEqualTo(
        "[REPEATED INT32 squareup.protos.simple.fooext = 125]");
    assertThat(Ext_simple_message.barext.toString()).isEqualTo(
        "[OPTIONAL INT32 squareup.protos.simple.barext = 126]");
    assertThat(Ext_simple_message.bazext.toString()).isEqualTo(
        "[REQUIRED INT32 squareup.protos.simple.bazext = 127]");
    assertThat(Ext_simple_message.nested_message_ext.toString()).isEqualTo(
        "[OPTIONAL MESSAGE squareup.protos.simple.nested_message_ext = 128]");
    assertThat(Ext_simple_message.nested_enum_ext.toString()).isEqualTo(
        "[OPTIONAL ENUM squareup.protos.simple.nested_enum_ext = 129]");
  }

  @Test
  public void testExtensionsNoRegistry() throws Exception {
    ExternalMessage optional_external_msg =
        new ExternalMessage.Builder()
            .setExtension(fooext, Arrays.asList(444, 555))
            .setExtension(barext, 333)
            .setExtension(bazext, 222)
            .build();

    SimpleMessage msg = new SimpleMessage.Builder()
        .optional_external_msg(optional_external_msg)
        .required_int32(456)
        .build();

    assertThat(msg.optional_external_msg.getExtension(fooext)).containsExactly(444, 555);
    assertThat(msg.optional_external_msg.getExtension(barext)).isEqualTo(new Integer(333));
    assertThat(msg.optional_external_msg.getExtension(bazext)).isEqualTo(new Integer(222));

    Wire wire = new Wire();
    MessageAdapter<SimpleMessage> adapter = wire.adapter(SimpleMessage.class);

    byte[] result = adapter.writeBytes(msg);
    assertThat(result.length).isEqualTo(21);

    SimpleMessage newMsg = adapter.readBytes(result);
    assertThat(newMsg.optional_external_msg.getExtension(fooext)).isNull();
    assertThat(newMsg.optional_external_msg.getExtension(barext)).isNull();
    assertThat(newMsg.optional_external_msg.getExtension(bazext)).isNull();
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

    Wire wire = new Wire();
    MessageAdapter<SimpleMessage> adapter = wire.adapter(SimpleMessage.class);

    // Empty lists and null lists occupy 0 bytes in the wire encoding
    byte[] noListBytes = adapter.writeBytes(noListMessage);
    byte[] emptyListBytes = adapter.writeBytes(emptyListMessage);

    assertThat(noListBytes.length).isEqualTo(2);
    assertThat(emptyListBytes.length).isEqualTo(2);

    assertThat(emptyListBytes[0]).isEqualTo(noListBytes[0]);
    assertThat(emptyListBytes[1]).isEqualTo(noListBytes[1]);

    // Parsed results have a null list
    SimpleMessage noListParsed = adapter.readBytes(noListBytes);
    SimpleMessage emptyListParsed = adapter.readBytes(emptyListBytes);

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

    Wire wire = new Wire();
    MessageAdapter<Person> adapter = wire.adapter(Person.class);

    byte[] data = adapter.writeBytes(person);
    assertThat(data[27]).isEqualTo((byte) PhoneType.WORK.getValue());

    // Corrupt the PhoneNumber type field with an undefined value
    data[27] = 17;
    // Parsed PhoneNumber has no value set
    Person result = adapter.readBytes(data);
    assertThat(result.phone.get(0).type).isNull();

    // The value 17 will be stored as an unknown varint with tag number 2
    Collection<List<UnknownFieldMap.Value>> unknownFields = result.phone.get(0).unknownFields();
    assertThat(unknownFields).hasSize(1);
    List<UnknownFieldMap.Value> values = unknownFields.iterator().next();
    assertThat(values).hasSize(1);
    VarintValue value = (VarintValue) values.get(0);
    assertThat(value.tag).isEqualTo(2);
    assertThat(value.value).isEqualTo(Long.valueOf(17L));

    // Serialize again, value is preserved
    byte[] newData = adapter.writeBytes(result);
    assertThat(data).isEqualTo(newData);
  }

  @Test public void missingRepeatedAndPackedFieldsBecomesEmptyList() throws IOException {
    byte[] bytes = new byte[0];
    RepeatedAndPacked data = new Wire().adapter(RepeatedAndPacked.class).readBytes(bytes);
    assertThat(data.rep_int32).isEqualTo(Collections.emptyList());
    assertThat(data.pack_int32).isEqualTo(Collections.emptyList());
  }
}
