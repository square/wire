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

import com.squareup.wire.protos.person.Person;
import com.squareup.wire.protos.person.Person.PhoneNumber;
import com.squareup.wire.protos.person.Person.PhoneType;
import com.squareup.wire.protos.simple.Ext_simple_message;
import com.squareup.wire.protos.simple.ExternalMessage;
import com.squareup.wire.protos.simple.SimpleMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static com.squareup.wire.protos.simple.Ext_simple_message.barext;
import static com.squareup.wire.protos.simple.Ext_simple_message.bazext;
import static com.squareup.wire.protos.simple.Ext_simple_message.fooext;
import static com.squareup.wire.protos.simple.Ext_simple_message.nested_message_ext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test Wire runtime.
 */
public class WireTest {

  @Test
  public void testSimpleMessage() throws Exception {
    SimpleMessage msg = new SimpleMessage.Builder().required_int32(456).build();
    assertEquals(null, msg.optional_int32);
    assertEquals(null, msg.optional_nested_msg);
    assertEquals(null, msg.optional_external_msg);
    assertEquals(null, msg.default_nested_enum);
    assertEquals(new Integer(456), msg.required_int32);
    assertEquals(null, msg.repeated_double);

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
    assertEquals(new Integer(789), msg.optional_int32);
    assertEquals(new Integer(2), msg.optional_nested_msg.bb);
    assertEquals(new Float(99.9f), msg.optional_external_msg.f);
    assertEquals(SimpleMessage.NestedEnum.BAR, msg.default_nested_enum);
    assertEquals(new Integer(456), msg.required_int32);
    assertEquals(doubles, msg.repeated_double);

    // Modifying the builder list does not affect an already-built message
    List<Double> savedData = new ArrayList<Double>(msg.repeated_double);
    doubles.set(1, 1.1);
    assertNotSame(doubles, msg.repeated_double);
    assertEquals(savedData, msg.repeated_double);

    // Rebuilding will use the new list
    msg = builder.build();
    assertEquals(doubles, msg.repeated_double);

    // Check for required fields
    builder.required_int32(null);
    try {
      builder.build();
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Required field not set:\n  required_int32", e.getMessage());
    }

    // The message list is immutable
    try {
      msg.repeated_double.set(0, 0.0);
      fail();
    } catch (UnsupportedOperationException e) {
      // expected
    }

    Wire wire = new Wire();

    assertEquals(46, msg.getSerializedSize());
    byte[] result = new byte[msg.getSerializedSize()];
    msg.writeTo(result, 0, result.length);
    assertEquals(46, result.length);

    SimpleMessage newMsg = wire.parseFrom(result, SimpleMessage.class);
    assertEquals(new Integer(789), newMsg.optional_int32);
    assertEquals(new Integer(2), newMsg.optional_nested_msg.bb);
    assertEquals(new Float(99.9F), newMsg.optional_external_msg.f);
    assertEquals(SimpleMessage.NestedEnum.BAR, newMsg.default_nested_enum);
    assertEquals(new Integer(456), newMsg.required_int32);
    assertEquals(doubles, msg.repeated_double);
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

    byte[] data = person.toByteArray();
    Wire wire = new Wire();
    wire.parseFrom(data, Person.class);
  }

  @Test
  public void testExtensions() throws Exception {
    ExternalMessage optional_external_msg =
        new ExternalMessage.Builder()
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

    assertEquals(Arrays.asList(444, 555), msg.optional_external_msg.getExtension(
        fooext));
    assertEquals(new Integer(333), msg.optional_external_msg.getExtension(barext));
    assertEquals(new Integer(222), msg.optional_external_msg.getExtension(bazext));
    assertEquals(new Integer(77), msg.optional_external_msg.getExtension(nested_message_ext).bb);
    assertEquals(SimpleMessage.NestedEnum.BAZ, msg.optional_external_msg.getExtension(
        Ext_simple_message.nested_enum_ext));

    Wire wire = new Wire(Ext_simple_message.class);
    int msgSerializedSize = msg.getSerializedSize();
    assertEquals(29, msgSerializedSize);
    byte[] result = new byte[msgSerializedSize];
    msg.writeTo(result);
    assertEquals(29, result.length);

    SimpleMessage newMsg = wire.parseFrom(result, SimpleMessage.class);
    assertEquals(Arrays.asList(444, 555), newMsg.optional_external_msg.getExtension(fooext));
    assertEquals(new Integer(333), newMsg.optional_external_msg.getExtension(barext));
    assertEquals(new Integer(222), newMsg.optional_external_msg.getExtension(bazext));
  }

  @Test
  public void extensionToString() {
    assertEquals("[REPEATED INT32 squareup.protos.simple.fooext = 125]",
        Ext_simple_message.fooext.toString());
    assertEquals("[OPTIONAL INT32 squareup.protos.simple.barext = 126]",
        Ext_simple_message.barext.toString());
    assertEquals("[REQUIRED INT32 squareup.protos.simple.bazext = 127]",
        Ext_simple_message.bazext.toString());
    assertEquals("[OPTIONAL MESSAGE squareup.protos.simple.nested_message_ext = 128]",
        Ext_simple_message.nested_message_ext.toString());
    assertEquals("[OPTIONAL ENUM squareup.protos.simple.nested_enum_ext = 129]",
        Ext_simple_message.nested_enum_ext.toString());
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

    assertEquals(Arrays.asList(444, 555), msg.optional_external_msg.getExtension(fooext));
    assertEquals(new Integer(333), msg.optional_external_msg.getExtension(barext));
    assertEquals(new Integer(222), msg.optional_external_msg.getExtension(bazext));

    Wire wire = new Wire();
    int msgSerializedSize = msg.getSerializedSize();
    assertEquals(21, msgSerializedSize);
    byte[] result = new byte[msgSerializedSize];
    msg.writeTo(result);
    assertEquals(21, result.length);

    SimpleMessage newMsg = wire.parseFrom(result, SimpleMessage.class);
    assertNull(newMsg.optional_external_msg.getExtension(fooext));
    assertNull(newMsg.optional_external_msg.getExtension(barext));
    assertNull(newMsg.optional_external_msg.getExtension(bazext));
  }
}
