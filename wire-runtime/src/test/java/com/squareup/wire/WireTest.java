// Copyright 2013 Square, Inc.
package com.squareup.wire;

import com.squareup.wire.protos.simple.Ext_simple_message;
import com.squareup.wire.protos.simple.ExternalMessage;
import com.squareup.wire.protos.simple.SimpleMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

import static com.squareup.wire.protos.simple.Ext_simple_message.barext;
import static com.squareup.wire.protos.simple.Ext_simple_message.bazext;
import static com.squareup.wire.protos.simple.Ext_simple_message.fooext;
import static com.squareup.wire.protos.simple.Ext_simple_message.nested_message_ext;

/**
 * Test Wire runtime.
 */
public class WireTest extends TestCase {

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
    assertEquals(99.9f, msg.optional_external_msg.f);
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
      // expected
    }

    // The message list is immutable
    try {
      msg.repeated_double.set(0, 0.0);
      fail();
    } catch (UnsupportedOperationException e) {
      // expected
    }

    Wire wire = new Wire();

    int msgSerializedSize = wire.getSerializedSize(msg);
    assertEquals(46, msgSerializedSize);
    byte[] result = new byte[msgSerializedSize];
    wire.writeTo(msg, result, 0, result.length);
    assertEquals(46, result.length);

    SimpleMessage newMsg = wire.parseFrom(SimpleMessage.class, result);
    assertEquals(new Integer(789), newMsg.optional_int32);
    assertEquals(new Integer(2), newMsg.optional_nested_msg.bb);
    assertEquals(99.9F, newMsg.optional_external_msg.f);
    assertEquals(SimpleMessage.NestedEnum.BAR, newMsg.default_nested_enum);
    assertEquals(new Integer(456), newMsg.required_int32);
    assertEquals(doubles, msg.repeated_double);
  }

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
    ProtoAdapter<SimpleMessage> adapter = wire.messageAdapter(SimpleMessage.class);
    int msgSerializedSize = adapter.getSerializedSize(msg);
    assertEquals(29, msgSerializedSize);
    byte[] result = new byte[msgSerializedSize];
    adapter.write(msg, WireOutput.newInstance(result));
    assertEquals(29, result.length);

    SimpleMessage newMsg = adapter.read(WireInput.newInstance(result));
    assertEquals(Arrays.asList(444, 555), newMsg.optional_external_msg.getExtension(fooext));
    assertEquals(new Integer(333), newMsg.optional_external_msg.getExtension(barext));
    assertEquals(new Integer(222), newMsg.optional_external_msg.getExtension(bazext));
  }

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
    ProtoAdapter<SimpleMessage> adapter = wire.messageAdapter(SimpleMessage.class);
    int msgSerializedSize = adapter.getSerializedSize(msg);
    assertEquals(21, msgSerializedSize);
    byte[] result = new byte[msgSerializedSize];
    adapter.write(msg, WireOutput.newInstance(result));
    assertEquals(21, result.length);

    SimpleMessage newMsg = adapter.read(WireInput.newInstance(result));
    assertNull(newMsg.optional_external_msg.getExtension(fooext));
    assertNull(newMsg.optional_external_msg.getExtension(barext));
    assertNull(newMsg.optional_external_msg.getExtension(bazext));
  }
}
