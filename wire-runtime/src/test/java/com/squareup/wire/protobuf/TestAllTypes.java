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
package com.squareup.wire.protobuf;

import com.squareup.wire.Extension;
import com.squareup.wire.Message;
import com.squareup.wire.Wire;
import com.squareup.wire.protos.alltypes.AllTypes;
import com.squareup.wire.protos.alltypes.Ext_all_types;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okio.Buffer;
import okio.ByteString;
import org.junit.Test;

import static com.squareup.wire.protos.alltypes.AllTypes.NestedEnum.A;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestAllTypes {

  // Return a two-element list with a given repeated value
  private static <T> List<T> list(T x) {
    return list(x, 2);
  }

  private static <T> List<T> list(T x, int numRepeated) {
    List<T> data = new ArrayList<T>(numRepeated);
    for (int i = 0; i < numRepeated; i++) {
      data.add(x);
    }
    return data;
  }

  private static AllTypes.Builder getBuilder() {
    return getBuilder(2);
  }

  private static AllTypes.Builder getBuilder(int numRepeated) {
    ByteString bytes = ByteString.of((byte) 125, (byte) 225);
    AllTypes.NestedMessage nestedMessage = new AllTypes.NestedMessage.Builder().a(999).build();
    return new AllTypes.Builder()
        .opt_int32(111)
        .opt_uint32(112)
        .opt_sint32(113)
        .opt_fixed32(114)
        .opt_sfixed32(115)
        .opt_int64(116L)
        .opt_uint64(117L)
        .opt_sint64(118L)
        .opt_fixed64(119L)
        .opt_sfixed64(120L)
        .opt_bool(true)
        .opt_float(122.0F)
        .opt_double(123.0)
        .opt_string("124")
        .opt_bytes(bytes)
        .opt_nested_enum(A)
        .opt_nested_message(nestedMessage)
        .req_int32(111)
        .req_uint32(112)
        .req_sint32(113)
        .req_fixed32(114)
        .req_sfixed32(115)
        .req_int64(116L)
        .req_uint64(117L)
        .req_sint64(118L)
        .req_fixed64(119L)
        .req_sfixed64(120L)
        .req_bool(true)
        .req_float(122.0F)
        .req_double(123.0)
        .req_string("124")
        .req_bytes(bytes)
        .req_nested_enum(A)
        .req_nested_message(nestedMessage)
        .rep_int32(list(111, numRepeated))
        .rep_uint32(list(112, numRepeated))
        .rep_sint32(list(113, numRepeated))
        .rep_fixed32(list(114, numRepeated))
        .rep_sfixed32(list(115, numRepeated))
        .rep_int64(list(116L, numRepeated))
        .rep_uint64(list(117L, numRepeated))
        .rep_sint64(list(118L, numRepeated))
        .rep_fixed64(list(119L, numRepeated))
        .rep_sfixed64(list(120L, numRepeated))
        .rep_bool(list(true, numRepeated))
        .rep_float(list(122.0F, numRepeated))
        .rep_double(list(123.0, numRepeated))
        .rep_string(list("124", numRepeated))
        .rep_bytes(list(bytes, numRepeated))
        .rep_nested_enum(list(A, numRepeated))
        .rep_nested_message(list(nestedMessage, numRepeated))
        .pack_int32(list(111, numRepeated))
        .pack_uint32(list(112, numRepeated))
        .pack_sint32(list(113, numRepeated))
        .pack_fixed32(list(114, numRepeated))
        .pack_sfixed32(list(115, numRepeated))
        .pack_int64(list(116L, numRepeated))
        .pack_uint64(list(117L, numRepeated))
        .pack_sint64(list(118L, numRepeated))
        .pack_fixed64(list(119L, numRepeated))
        .pack_sfixed64(list(120L, numRepeated))
        .pack_bool(list(true, numRepeated))
        .pack_float(list(122.0F, numRepeated))
        .pack_double(list(123.0, numRepeated))
        .pack_nested_enum(list(A, numRepeated))
        .setExtension(Ext_all_types.ext_opt_bool, true)
        .setExtension(Ext_all_types.ext_rep_bool, list(true, numRepeated))
        .setExtension(Ext_all_types.ext_pack_bool, list(true, numRepeated));
  }

  private final AllTypes allTypes = createAllTypes();
  private final Wire wire = new Wire(Ext_all_types.class);

  private AllTypes createAllTypes(int numRepeated) {
    return getBuilder(numRepeated).build();
  }

  private AllTypes createAllTypes() {
    return getBuilder().build();
  }

  @Test
  public void testHashCodes() {
    AllTypes.Builder builder = getBuilder();
    AllTypes message = builder.build();
    int messageHashCode = message.hashCode();
    assertEquals(allTypes.hashCode(), messageHashCode);
  }

  @Test
  public void testBuilder() {
    AllTypes.Builder builder = getBuilder();
    AllTypes.NestedMessage nestedMessage = new AllTypes.NestedMessage.Builder().a(999).build();

    assertEquals(new Integer(111), builder.opt_int32);
    assertEquals(new Integer(112), builder.opt_uint32);
    assertEquals(new Integer(113), builder.opt_sint32);
    assertEquals(new Integer(114), builder.opt_fixed32);
    assertEquals(new Integer(115), builder.opt_sfixed32);
    assertEquals(new Long(116L), builder.opt_int64);
    assertEquals(new Long(117L), builder.opt_uint64);
    assertEquals(new Long(118L), builder.opt_sint64);
    assertEquals(new Long(119L), builder.opt_fixed64);
    assertEquals(new Long(120L), builder.opt_sfixed64);
    assertEquals(Boolean.TRUE, builder.opt_bool);
    assertEquals(new Float(122.0F), builder.opt_float);
    assertEquals(new Double(123.0), builder.opt_double);
    assertEquals("124", builder.opt_string);
    assertEquals(2, builder.opt_bytes.size());
    assertEquals((byte) 125, builder.opt_bytes.getByte(0));
    assertEquals((byte) 225, builder.opt_bytes.getByte(1));
    assertEquals(A, builder.opt_nested_enum);
    assertEquals(nestedMessage, builder.opt_nested_message);

    assertEquals(new Integer(111), builder.req_int32);
    assertEquals(new Integer(112), builder.req_uint32);
    assertEquals(new Integer(113), builder.req_sint32);
    assertEquals(new Integer(114), builder.req_fixed32);
    assertEquals(new Integer(115), builder.req_sfixed32);
    assertEquals(new Long(116L), builder.req_int64);
    assertEquals(new Long(117L), builder.req_uint64);
    assertEquals(new Long(118L), builder.req_sint64);
    assertEquals(new Long(119L), builder.req_fixed64);
    assertEquals(new Long(120L), builder.req_sfixed64);
    assertEquals(Boolean.TRUE, builder.req_bool);
    assertEquals(new Float(122.0F), builder.req_float);
    assertEquals(new Double(123.0), builder.req_double);
    assertEquals("124", builder.req_string);
    assertEquals(2, builder.req_bytes.size());
    assertEquals((byte) 125, builder.req_bytes.getByte(0));
    assertEquals((byte) 225, builder.req_bytes.getByte(1));
    assertEquals(A, builder.req_nested_enum);
    assertEquals(nestedMessage, builder.req_nested_message);

    assertEquals(2, builder.rep_int32.size());
    assertEquals(new Integer(111), builder.rep_int32.get(0));
    assertEquals(new Integer(111), builder.rep_int32.get(1));
    assertEquals(2, builder.rep_uint32.size());
    assertEquals(new Integer(112), builder.rep_uint32.get(0));
    assertEquals(new Integer(112), builder.rep_uint32.get(1));
    assertEquals(2, builder.rep_sint32.size());
    assertEquals(new Integer(113), builder.rep_sint32.get(0));
    assertEquals(new Integer(113), builder.rep_sint32.get(1));
    assertEquals(2, builder.rep_fixed32.size());
    assertEquals(new Integer(114), builder.rep_fixed32.get(0));
    assertEquals(new Integer(114), builder.rep_fixed32.get(1));
    assertEquals(2, builder.rep_sfixed32.size());
    assertEquals(new Integer(115), builder.rep_sfixed32.get(0));
    assertEquals(new Integer(115), builder.rep_sfixed32.get(1));
    assertEquals(2, builder.rep_int64.size());
    assertEquals(new Long(116L), builder.rep_int64.get(0));
    assertEquals(new Long(116L), builder.rep_int64.get(1));
    assertEquals(2, builder.rep_uint64.size());
    assertEquals(new Long(117L), builder.rep_uint64.get(0));
    assertEquals(new Long(117L), builder.rep_uint64.get(1));
    assertEquals(2, builder.rep_sint64.size());
    assertEquals(new Long(118L), builder.rep_sint64.get(0));
    assertEquals(new Long(118L), builder.rep_sint64.get(1));
    assertEquals(2, builder.rep_fixed64.size());
    assertEquals(new Long(119L), builder.rep_fixed64.get(0));
    assertEquals(new Long(119L), builder.rep_fixed64.get(1));
    assertEquals(2, builder.rep_sfixed64.size());
    assertEquals(new Long(120L), builder.rep_sfixed64.get(0));
    assertEquals(new Long(120L), builder.rep_sfixed64.get(1));
    assertEquals(2, builder.rep_bool.size());
    assertEquals(Boolean.TRUE, builder.rep_bool.get(0));
    assertEquals(Boolean.TRUE, builder.rep_bool.get(1));
    assertEquals(2, builder.rep_float.size());
    assertEquals(new Float(122.0F), builder.rep_float.get(0));
    assertEquals(new Float(122.0F), builder.rep_float.get(1));
    assertEquals(2, builder.rep_double.size());
    assertEquals(new Double(123.0), builder.rep_double.get(0));
    assertEquals(new Double(123.0), builder.rep_double.get(1));
    assertEquals(2, builder.rep_string.size());
    assertEquals("124", builder.rep_string.get(0));
    assertEquals("124", builder.rep_string.get(1));
    assertEquals(2, builder.rep_bytes.size());
    assertEquals(2, builder.rep_bytes.get(0).size());
    assertEquals((byte) 125, builder.rep_bytes.get(0).getByte(0));
    assertEquals((byte) 225, builder.rep_bytes.get(0).getByte(1));
    assertEquals(2, builder.rep_bytes.get(1).size());
    assertEquals((byte) 125, builder.rep_bytes.get(1).getByte(0));
    assertEquals((byte) 225, builder.rep_bytes.get(1).getByte(1));
    assertEquals(2, builder.rep_nested_enum.size());
    assertEquals(A, builder.rep_nested_enum.get(0));
    assertEquals(A, builder.rep_nested_enum.get(1));
    assertEquals(2, builder.rep_nested_message.size());
    assertEquals(nestedMessage, builder.rep_nested_message.get(0));
    assertEquals(nestedMessage, builder.rep_nested_message.get(1));

    assertEquals(2, builder.pack_int32.size());
    assertEquals(new Integer(111), builder.pack_int32.get(0));
    assertEquals(new Integer(111), builder.pack_int32.get(1));
    assertEquals(2, builder.pack_uint32.size());
    assertEquals(new Integer(112), builder.pack_uint32.get(0));
    assertEquals(new Integer(112), builder.pack_uint32.get(1));
    assertEquals(2, builder.pack_sint32.size());
    assertEquals(new Integer(113), builder.pack_sint32.get(0));
    assertEquals(new Integer(113), builder.pack_sint32.get(1));
    assertEquals(2, builder.pack_fixed32.size());
    assertEquals(new Integer(114), builder.pack_fixed32.get(0));
    assertEquals(new Integer(114), builder.pack_fixed32.get(1));
    assertEquals(2, builder.pack_sfixed32.size());
    assertEquals(new Integer(115), builder.pack_sfixed32.get(0));
    assertEquals(new Integer(115), builder.pack_sfixed32.get(1));
    assertEquals(2, builder.pack_int64.size());
    assertEquals(new Long(116L), builder.pack_int64.get(0));
    assertEquals(new Long(116L), builder.pack_int64.get(1));
    assertEquals(2, builder.pack_uint64.size());
    assertEquals(new Long(117L), builder.pack_uint64.get(0));
    assertEquals(new Long(117L), builder.pack_uint64.get(1));
    assertEquals(2, builder.pack_sint64.size());
    assertEquals(new Long(118L), builder.pack_sint64.get(0));
    assertEquals(new Long(118L), builder.pack_sint64.get(1));
    assertEquals(2, builder.pack_fixed64.size());
    assertEquals(new Long(119L), builder.pack_fixed64.get(0));
    assertEquals(new Long(119L), builder.pack_fixed64.get(1));
    assertEquals(2, builder.pack_sfixed64.size());
    assertEquals(new Long(120L), builder.pack_sfixed64.get(0));
    assertEquals(new Long(120L), builder.pack_sfixed64.get(1));
    assertEquals(2, builder.pack_bool.size());
    assertEquals(Boolean.TRUE, builder.pack_bool.get(0));
    assertEquals(Boolean.TRUE, builder.pack_bool.get(1));
    assertEquals(2, builder.pack_float.size());
    assertEquals(new Float(122.0F), builder.pack_float.get(0));
    assertEquals(new Float(122.0F), builder.pack_float.get(1));
    assertEquals(2, builder.pack_double.size());
    assertEquals(new Double(123.0), builder.pack_double.get(0));
    assertEquals(new Double(123.0), builder.pack_double.get(1));
    assertEquals(2, builder.pack_nested_enum.size());
    assertEquals(A, builder.pack_nested_enum.get(0));
    assertEquals(A, builder.pack_nested_enum.get(1));

    assertEquals(Boolean.TRUE, builder.getExtension(Ext_all_types.ext_opt_bool));
    assertEquals(list(true), builder.getExtension(Ext_all_types.ext_rep_bool));
    assertEquals(list(true), builder.getExtension(Ext_all_types.ext_pack_bool));

    builder.setExtension(Ext_all_types.ext_opt_bool, false);
    builder.setExtension(Ext_all_types.ext_rep_bool, list(false));
    builder.setExtension(Ext_all_types.ext_pack_bool, list(false));

    assertEquals(Boolean.FALSE, builder.getExtension(Ext_all_types.ext_opt_bool));
    assertEquals(list(false), builder.getExtension(Ext_all_types.ext_rep_bool));
    assertEquals(list(false), builder.getExtension(Ext_all_types.ext_pack_bool));
  }

  @Test
  public void testInitBuilder() {
    AllTypes.Builder builder = new AllTypes.Builder(allTypes);
    assertEquals(allTypes, builder.build());
    builder.opt_bool = false;
    assertNotSame(allTypes, builder.build());
  }

  @Test
  public void testWrite() {
    int count = allTypes.getSerializedSize();
    assertEquals(TestAllTypesData.expectedOutput.length, count);
    byte[] output = new byte[count];
    allTypes.writeTo(output, 0, count);
    assertEquals(ByteString.of(TestAllTypesData.expectedOutput), ByteString.of(output));

    output = new byte[count];
    allTypes.writeTo(output);
    assertEquals(ByteString.of(TestAllTypesData.expectedOutput), ByteString.of(output));

    output = allTypes.toByteArray();
    assertEquals(TestAllTypesData.expectedOutput.length, output.length);
    assertEquals(ByteString.of(TestAllTypesData.expectedOutput), ByteString.of(output));
  }

  @Test
  public void testRead() throws IOException {
    byte[] data = new byte[TestAllTypesData.expectedOutput.length];
    allTypes.writeTo(data, 0, data.length);
    AllTypes parsed = wire.parseFrom(data, AllTypes.class);
    assertEquals(allTypes, parsed);

    assertEquals(Boolean.TRUE, allTypes.getExtension(Ext_all_types.ext_opt_bool));
    assertEquals(list(true), allTypes.getExtension(Ext_all_types.ext_rep_bool));
    assertEquals(list(true), allTypes.getExtension(Ext_all_types.ext_pack_bool));

    List<Extension<AllTypes, ?>> extensions = parsed.getExtensions();
    assertEquals(3, extensions.size());
    assertTrue(extensions.contains(Ext_all_types.ext_opt_bool));
    assertTrue(extensions.contains(Ext_all_types.ext_rep_bool));
    assertTrue(extensions.contains(Ext_all_types.ext_pack_bool));
  }

  @Test
  public void testReadWithOffset() throws IOException {
    byte[] data = new byte[TestAllTypesData.expectedOutput.length + 100];
    allTypes.writeTo(data, 50, TestAllTypesData.expectedOutput.length);
    AllTypes parsed = wire.parseFrom(data, 50, TestAllTypesData.expectedOutput.length, AllTypes.class);
    assertEquals(allTypes, parsed);

    assertEquals(Boolean.TRUE, allTypes.getExtension(Ext_all_types.ext_opt_bool));
    assertEquals(list(true), allTypes.getExtension(Ext_all_types.ext_rep_bool));
    assertEquals(list(true), allTypes.getExtension(Ext_all_types.ext_pack_bool));

    List<Extension<AllTypes, ?>> extensions = parsed.getExtensions();
    assertEquals(3, extensions.size());
    assertTrue(extensions.contains(Ext_all_types.ext_opt_bool));
    assertTrue(extensions.contains(Ext_all_types.ext_rep_bool));
    assertTrue(extensions.contains(Ext_all_types.ext_pack_bool));
  }

  @Test
  public void testReadFromInputStream() throws IOException {
    byte[] data = new byte[TestAllTypesData.expectedOutput.length];
    allTypes.writeTo(data, 0, data.length);

    InputStream input = new ByteArrayInputStream(data);
    AllTypes parsed = wire.parseFrom(input, AllTypes.class);
    assertEquals(allTypes, parsed);

    assertEquals(Boolean.TRUE, allTypes.getExtension(Ext_all_types.ext_opt_bool));
    assertEquals(list(true), allTypes.getExtension(Ext_all_types.ext_rep_bool));
    assertEquals(list(true), allTypes.getExtension(Ext_all_types.ext_pack_bool));

    List<Extension<AllTypes, ?>> extensions = parsed.getExtensions();
    assertEquals(3, extensions.size());
    assertTrue(extensions.contains(Ext_all_types.ext_opt_bool));
    assertTrue(extensions.contains(Ext_all_types.ext_rep_bool));
    assertTrue(extensions.contains(Ext_all_types.ext_pack_bool));
  }

  @Test
  public void testReadFromOkioBuffer() throws IOException {
    byte[] data = new byte[TestAllTypesData.expectedOutput.length];
    allTypes.writeTo(data, 0, data.length);

    Buffer input = new Buffer().write(data);
    AllTypes parsed = wire.parseFrom(input, AllTypes.class);
    assertEquals(allTypes, parsed);

    assertEquals(Boolean.TRUE, allTypes.getExtension(Ext_all_types.ext_opt_bool));
    assertEquals(list(true), allTypes.getExtension(Ext_all_types.ext_rep_bool));
    assertEquals(list(true), allTypes.getExtension(Ext_all_types.ext_pack_bool));

    List<Extension<AllTypes, ?>> extensions = parsed.getExtensions();
    assertEquals(3, extensions.size());
    assertTrue(extensions.contains(Ext_all_types.ext_opt_bool));
    assertTrue(extensions.contains(Ext_all_types.ext_rep_bool));
    assertTrue(extensions.contains(Ext_all_types.ext_pack_bool));
  }

  @Test
  public void testReadLongMessagesFromInputStream() throws IOException {
    AllTypes allTypes = createAllTypes(50);
    byte[] data = new byte[allTypes.getSerializedSize()];
    allTypes.writeTo(data, 0, data.length);

    InputStream input = new ByteArrayInputStream(data);
    AllTypes parsed = wire.parseFrom(input, AllTypes.class);
    assertEquals(allTypes, parsed);

    assertEquals(Boolean.TRUE, allTypes.getExtension(Ext_all_types.ext_opt_bool));
    assertEquals(list(true, 50), allTypes.getExtension(Ext_all_types.ext_rep_bool));
    assertEquals(list(true, 50), allTypes.getExtension(Ext_all_types.ext_pack_bool));

    List<Extension<AllTypes, ?>> extensions = parsed.getExtensions();
    assertEquals(3, extensions.size());
    assertTrue(extensions.contains(Ext_all_types.ext_opt_bool));
    assertTrue(extensions.contains(Ext_all_types.ext_rep_bool));
    assertTrue(extensions.contains(Ext_all_types.ext_pack_bool));
  }

  // An input stream that returns 1, 2, 3, or 4 bytes at a time
  private static class SlowInputStream extends InputStream {
    private final byte[] data;
    private int pos;

    public SlowInputStream(byte[] data) {
      this.data = data;
    }

    @Override public int read(byte[] output, int offset, int count) {
      if (pos == data.length) {
        return -1;
      }
      int bytesToReturn = Math.min(data.length - pos, (pos % 4) + 1);
      for (int i = 0; i < bytesToReturn; i++) {
        output[offset++] = data[pos++];
      }
      return bytesToReturn;
    }

    @Override public int read() {
      if (pos == data.length) {
        return -1;
      }
      return (int) data[pos++];
    }
  }

  @Test
  public void testReadFromSlowInputStream() throws IOException {
    byte[] data = new byte[TestAllTypesData.expectedOutput.length];
    allTypes.writeTo(data, 0, data.length);

    InputStream input = new SlowInputStream(data);
    AllTypes parsed = wire.parseFrom(input, AllTypes.class);
    assertEquals(allTypes, parsed);

    assertEquals(Boolean.TRUE, allTypes.getExtension(Ext_all_types.ext_opt_bool));
    assertEquals(list(true), allTypes.getExtension(Ext_all_types.ext_rep_bool));
    assertEquals(list(true), allTypes.getExtension(Ext_all_types.ext_pack_bool));

    List<Extension<AllTypes, ?>> extensions = parsed.getExtensions();
    assertEquals(3, extensions.size());
    assertTrue(extensions.contains(Ext_all_types.ext_opt_bool));
    assertTrue(extensions.contains(Ext_all_types.ext_rep_bool));
    assertTrue(extensions.contains(Ext_all_types.ext_pack_bool));
  }

  @Test
  public void testReadNoExtension() throws IOException {
    byte[] data = new byte[TestAllTypesData.expectedOutput.length];
    allTypes.writeTo(data, 0, data.length);
    AllTypes parsed = new Wire().parseFrom(data, AllTypes.class);
    assertFalse(allTypes.equals(parsed));
  }

  @Test
  public void testReadNonPacked() throws IOException {
    AllTypes parsed = wire.parseFrom(TestAllTypesData.nonPacked, AllTypes.class);
    assertEquals(allTypes, parsed);
  }

  @Test
  public void testToString() throws IOException {
    byte[] data = new byte[TestAllTypesData.expectedOutput.length];
    allTypes.writeTo(data, 0, data.length);
    AllTypes parsed = wire.parseFrom(data, AllTypes.class);
    assertEquals(TestAllTypesData.expectedToString, parsed.toString());
  }

  @Test
  public void testRequiredFields() {
    AllTypes.Builder builder = getBuilder();
    builder.build();

    builder.req_bool(null);
    try {
      builder.build();
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Required field not set:\n  req_bool", e.getMessage());
    }

    builder.req_int32(null);
    builder.req_sfixed64(null);
    try {
      builder.build();
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Required fields not set:\n  req_bool\n  req_int32\n  req_sfixed64",
        e.getMessage());
    }
  }

  @Test
  public void testDefaults() throws Exception {
    assertEquals(true, AllTypes.DEFAULT_DEFAULT_BOOL);
    // original: "<c-cedilla>ok\a\b\f\n\r\t\v\1\01\001\17\017\176\x1\x01\x11\X1\X01\X11g<u umlaut>zel"
    assertEquals(
        "çok\u0007\b\f\n\r\t\u000b\u0001\u0001\u0001\u000f\u000f~\u0001\u0001\u0011\u0001\u0001\u0011güzel",
        AllTypes.DEFAULT_DEFAULT_STRING);
    assertEquals("çok\u0007\b\f\n\r\t\u000b\u0001\u0001\u0001\u000f\u000f~\u0001\u0001\u0011\u0001\u0001\u0011güzel",
        new String(AllTypes.DEFAULT_DEFAULT_BYTES.toByteArray(), "ISO-8859-1"));
  }

  @Test
  public void testEnums() {
    assertEquals(A, Message.enumFromInt(AllTypes.NestedEnum.class, 1));
    assertEquals(1, Message.intFromEnum(A));
  }

  @Test
  public void testSkipGroup() throws IOException {
    byte[] data =  new byte[TestAllTypesData.expectedOutput.length + 27];
    System.arraycopy(TestAllTypesData.expectedOutput, 0, data, 0, 17);
    int index = 17;
    data[index++] = (byte) 0xa3; // start group, tag = 20, type = 3
    data[index++] = (byte) 0x01;
    data[index++] = (byte) 0x08; // tag = 1, type = 0 (varint)
    data[index++] = (byte) 0x81;
    data[index++] = (byte) 0x82;
    data[index++] = (byte) 0x6f;
    data[index++] = (byte) 0x21; // tag = 2, type = 1 (fixed64)
    data[index++] = (byte) 0x01;
    data[index++] = (byte) 0x02;
    data[index++] = (byte) 0x03;
    data[index++] = (byte) 0x04;
    data[index++] = (byte) 0x05;
    data[index++] = (byte) 0x06;
    data[index++] = (byte) 0x07;
    data[index++] = (byte) 0x08;
    data[index++] = (byte) 0x1a; // tag = 3, type = 2 (length-delimited)
    data[index++] = (byte) 0x03; // length = 3
    data[index++] = (byte) 0x01;
    data[index++] = (byte) 0x02;
    data[index++] = (byte) 0x03;
    data[index++] = (byte) 0x25; // tag = 4, type = 5 (fixed32)
    data[index++] = (byte) 0x01;
    data[index++] = (byte) 0x02;
    data[index++] = (byte) 0x03;
    data[index++] = (byte) 0x04;
    data[index++] = (byte) 0xa4; // end group, tag = 20, type = 4
    data[index++] = (byte) 0x01;

    System.arraycopy(TestAllTypesData.expectedOutput, 17, data, index,
        TestAllTypesData.expectedOutput.length - 17);

    AllTypes parsed = wire.parseFrom(data, AllTypes.class);
    assertEquals(allTypes, parsed);
  }

  @Test
  public void testUnknownFields() {
    AllTypes.Builder builder = getBuilder();
    builder.addVarint(10000, 1);
    AllTypes withUnknownField = builder.build();
    byte[] data = withUnknownField.toByteArray();
    int count = TestAllTypesData.expectedOutput.length;
    assertEquals(count + 4, data.length);
    assertEquals((byte) 0x80, data[count]);
    assertEquals((byte) 0xf1, data[count + 1]);
    assertEquals((byte) 0x04, data[count + 2]);
    assertEquals((byte) 0x01, data[count + 3]);

    // Don't allow heterogeneous types for the same tag
    try {
      builder = getBuilder();
      builder.addVarint(10000, 1);
      builder.addFixed32(10000, 2);
      fail();
    } catch (IllegalArgumentException expected) {
      assertEquals("Wire type FIXED32 differs from previous type VARINT for tag 10000",
          expected.getMessage());
    }
  }

  @Test
  public void testNullEnum() {
    try {
      // A null value for a repeated field is not allowed.
      getBuilder().rep_nested_enum(Arrays.asList(A, null, A));
      fail();
    } catch (NullPointerException e) {
    }

    // Should not fail - a null list means the field is cleared.
    getBuilder().rep_nested_enum(null);
  }
}
