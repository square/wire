
package com.google.protobuf;

import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.squareup.omar.Omar;
import com.squareup.omar.ProtoAdapter;
import com.squareup.omar.UninitializedMessageException;
import com.squareup.protos.simple.ExternalMessageContainer;
import com.squareup.protos.simple.SimpleMessageContainer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

import static com.squareup.protos.simple.SimpleMessageContainer.SimpleMessage;

/**
 * Test puny runtime.
 */
public class OmarTest extends TestCase {

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
    SimpleMessage.NestedMessage.Builder nested_msg_builder = new SimpleMessage.NestedMessage.Builder();
    nested_msg_builder.bb(2);
    builder.optional_nested_msg(nested_msg_builder.build());
    ExternalMessageContainer.ExternalMessage.Builder external_msg_builder =
        new ExternalMessageContainer.ExternalMessage.Builder();
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

    // Rebulding will use the new list
    msg = builder.build();
    assertEquals(doubles, msg.repeated_double);

    // Check for required fields
    builder.required_int32(null);
    try {
      builder.build();
      fail();
    } catch (UninitializedMessageException e) {
      // expected
    }

    // The message list is immutable
    try {
      msg.repeated_double.set(0, 0.0);
      fail();
    } catch (UnsupportedOperationException e) {
      // expected
    }

    Omar omar = new Omar();

    int msgSerializedSize = Omar.getSerializedSize(msg);
    assertEquals(46, msgSerializedSize);
    byte[] result = new byte[msgSerializedSize];
    omar.writeTo(msg, result, 0, result.length);
    assertEquals(46, result.length);

    SimpleMessage newMsg = omar.parseFrom(SimpleMessage.class, result);
    assertEquals(new Integer(789), newMsg.optional_int32);
    assertEquals(new Integer(2), newMsg.optional_nested_msg.bb);
    assertEquals(new Float(99.9f), newMsg.optional_external_msg.f);
    assertEquals(SimpleMessage.NestedEnum.BAR, newMsg.default_nested_enum);
    assertEquals(new Integer(456), newMsg.required_int32);
    assertEquals(doubles, msg.repeated_double);
  }

  public void testExtensions() throws Exception {
    ExternalMessageContainer.ExternalMessage optional_external_msg =
        new ExternalMessageContainer.ExternalMessage.Builder()
        .setExtension(SimpleMessageContainer.fooext, Arrays.asList(444, 555))
        .setExtension(SimpleMessageContainer.barext, 333)
        .setExtension(SimpleMessageContainer.bazext, 222)
        .setExtension(SimpleMessageContainer.nested_message_ext, new SimpleMessage.NestedMessage.Builder().bb(77).build())
        .setExtension(SimpleMessageContainer.nested_enum_ext, SimpleMessage.NestedEnum.BAZ)
        .build();

    SimpleMessage msg = new SimpleMessage.Builder()
        .optional_external_msg(optional_external_msg)
        .required_int32(456)
        .build();

    assertEquals(Arrays.asList(444, 555), msg.optional_external_msg.getExtension(SimpleMessageContainer.fooext));
    assertEquals(new Integer(333), msg.optional_external_msg.getExtension(SimpleMessageContainer.barext));
    assertEquals(new Integer(222), msg.optional_external_msg.getExtension(SimpleMessageContainer.bazext));
    assertEquals(new Integer(77), msg.optional_external_msg.getExtension(SimpleMessageContainer.nested_message_ext).bb);
    assertEquals(SimpleMessage.NestedEnum.BAZ, msg.optional_external_msg.getExtension(SimpleMessageContainer.nested_enum_ext));

    Omar omar = new Omar(SimpleMessageContainer.class);
    ProtoAdapter<SimpleMessage> adapter = omar.messageAdapter(SimpleMessage.class);
    int msgSerializedSize = adapter.getSerializedSize(msg);
    //assertEquals(38, msgSerializedSize);
    byte[] result = new byte[msgSerializedSize];
    adapter.write(msg, CodedOutputByteBufferNano.newInstance(result));
    //assertEquals(38, result.length);

    SimpleMessage newMsg = adapter.read(CodedInputByteBufferNano.newInstance(result));
    assertEquals(Arrays.asList(444, 555), newMsg.optional_external_msg.getExtension(SimpleMessageContainer.fooext));
    assertEquals(new Integer(333), newMsg.optional_external_msg.getExtension(SimpleMessageContainer.barext));
    assertEquals(new Integer(222), newMsg.optional_external_msg.getExtension(SimpleMessageContainer.bazext));
  }

  public void testExtensionsNoRegistry() throws Exception {
    ExternalMessageContainer.ExternalMessage optional_external_msg =
        new ExternalMessageContainer.ExternalMessage.Builder()
            .setExtension(SimpleMessageContainer.fooext, Arrays.asList(444, 555))
            .setExtension(SimpleMessageContainer.barext, 333)
            .setExtension(SimpleMessageContainer.bazext, 222)
            .build();

    SimpleMessage msg = new SimpleMessage.Builder()
        .optional_external_msg(optional_external_msg)
        .required_int32(456)
        .build();

    assertEquals(Arrays.asList(444, 555), msg.optional_external_msg.getExtension(SimpleMessageContainer.fooext));
    assertEquals(new Integer(333), msg.optional_external_msg.getExtension(SimpleMessageContainer.barext));
    assertEquals(new Integer(222), msg.optional_external_msg.getExtension(SimpleMessageContainer.bazext));

    Omar omar = new Omar();
    ProtoAdapter<SimpleMessage> adapter = omar.messageAdapter(SimpleMessage.class);
    int msgSerializedSize = adapter.getSerializedSize(msg);
    //assertEquals(30, msgSerializedSize);
    byte[] result = new byte[msgSerializedSize];
    adapter.write(msg, CodedOutputByteBufferNano.newInstance(result));
    //assertEquals(30, result.length);

    SimpleMessage newMsg = adapter.read(CodedInputByteBufferNano.newInstance(result));
    assertNull(newMsg.optional_external_msg.getExtension(SimpleMessageContainer.fooext));
    assertNull(newMsg.optional_external_msg.getExtension(SimpleMessageContainer.barext));
    assertNull(newMsg.optional_external_msg.getExtension(SimpleMessageContainer.bazext));
  }
}
