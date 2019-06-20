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

import com.squareup.wire.protos.alltypes.AllTypes;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okio.Buffer;
import okio.ByteString;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;
import org.junit.Ignore;
import org.junit.Test;

import static com.squareup.wire.protos.alltypes.AllTypes.NestedEnum.A;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class TestAllTypes {

  // Return a two-element list with a given repeated value
  private static <T> List<T> list(T x) {
    return list(x, 2);
  }

  private static <T> List<T> list(T x, int numRepeated) {
    List<T> data = new ArrayList<>(numRepeated);
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
        .ext_opt_bool(true)
        .ext_rep_bool(list(true, numRepeated))
        .ext_pack_bool(list(true, numRepeated));
  }

  private final AllTypes allTypes = createAllTypes();
  private final ProtoAdapter<AllTypes> adapter = AllTypes.ADAPTER;

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
    assertThat(messageHashCode).isEqualTo(allTypes.hashCode());
  }

  @Test
  public void testBuilder() {
    AllTypes.Builder builder = getBuilder();
    AllTypes.NestedMessage nestedMessage = new AllTypes.NestedMessage.Builder().a(999).build();

    assertThat(builder.opt_int32).isEqualTo(new Integer(111));
    assertThat(builder.opt_uint32).isEqualTo(new Integer(112));
    assertThat(builder.opt_sint32).isEqualTo(new Integer(113));
    assertThat(builder.opt_fixed32).isEqualTo(new Integer(114));
    assertThat(builder.opt_sfixed32).isEqualTo(new Integer(115));
    assertThat(builder.opt_int64).isEqualTo(new Long(116L));
    assertThat(builder.opt_uint64).isEqualTo(new Long(117L));
    assertThat(builder.opt_sint64).isEqualTo(new Long(118L));
    assertThat(builder.opt_fixed64).isEqualTo(new Long(119L));
    assertThat(builder.opt_sfixed64).isEqualTo(new Long(120L));
    assertThat(builder.opt_bool).isEqualTo(Boolean.TRUE);
    assertThat(builder.opt_float).isEqualTo(new Float(122.0F));
    assertThat(builder.opt_double).isEqualTo(new Double(123.0));
    assertThat(builder.opt_string).isEqualTo("124");
    assertThat(builder.opt_bytes).isEqualTo(ByteString.of((byte) 125, (byte) 225));
    assertThat(builder.opt_nested_enum).isEqualTo(A);
    assertThat(builder.opt_nested_message).isEqualTo(nestedMessage);

    assertThat(builder.req_int32).isEqualTo(new Integer(111));
    assertThat(builder.req_uint32).isEqualTo(new Integer(112));
    assertThat(builder.req_sint32).isEqualTo(new Integer(113));
    assertThat(builder.req_fixed32).isEqualTo(new Integer(114));
    assertThat(builder.req_sfixed32).isEqualTo(new Integer(115));
    assertThat(builder.req_int64).isEqualTo(new Long(116L));
    assertThat(builder.req_uint64).isEqualTo(new Long(117L));
    assertThat(builder.req_sint64).isEqualTo(new Long(118L));
    assertThat(builder.req_fixed64).isEqualTo(new Long(119L));
    assertThat(builder.req_sfixed64).isEqualTo(new Long(120L));
    assertThat(builder.req_bool).isEqualTo(Boolean.TRUE);
    assertThat(builder.req_float).isEqualTo(new Float(122.0F));
    assertThat(builder.req_double).isEqualTo(new Double(123.0));
    assertThat(builder.req_string).isEqualTo("124");
    assertThat(builder.req_bytes).isEqualTo(ByteString.of((byte) 125, (byte) 225));
    assertThat(builder.req_nested_enum).isEqualTo(A);
    assertThat(builder.req_nested_message).isEqualTo(nestedMessage);

    assertThat(builder.rep_int32).hasSize(2);
    assertThat(builder.rep_int32.get(0)).isEqualTo(new Integer(111));
    assertThat(builder.rep_int32.get(1)).isEqualTo(new Integer(111));
    assertThat(builder.rep_uint32).hasSize(2);
    assertThat(builder.rep_uint32.get(0)).isEqualTo(new Integer(112));
    assertThat(builder.rep_uint32.get(1)).isEqualTo(new Integer(112));
    assertThat(builder.rep_sint32).hasSize(2);
    assertThat(builder.rep_sint32.get(0)).isEqualTo(new Integer(113));
    assertThat(builder.rep_sint32.get(1)).isEqualTo(new Integer(113));
    assertThat(builder.rep_fixed32).hasSize(2);
    assertThat(builder.rep_fixed32.get(0)).isEqualTo(new Integer(114));
    assertThat(builder.rep_fixed32.get(1)).isEqualTo(new Integer(114));
    assertThat(builder.rep_sfixed32).hasSize(2);
    assertThat(builder.rep_sfixed32.get(0)).isEqualTo(new Integer(115));
    assertThat(builder.rep_sfixed32.get(1)).isEqualTo(new Integer(115));
    assertThat(builder.rep_int64).hasSize(2);
    assertThat(builder.rep_int64.get(0)).isEqualTo(new Long(116L));
    assertThat(builder.rep_int64.get(1)).isEqualTo(new Long(116L));
    assertThat(builder.rep_uint64).hasSize(2);
    assertThat(builder.rep_uint64.get(0)).isEqualTo(new Long(117L));
    assertThat(builder.rep_uint64.get(1)).isEqualTo(new Long(117L));
    assertThat(builder.rep_sint64).hasSize(2);
    assertThat(builder.rep_sint64.get(0)).isEqualTo(new Long(118L));
    assertThat(builder.rep_sint64.get(1)).isEqualTo(new Long(118L));
    assertThat(builder.rep_fixed64).hasSize(2);
    assertThat(builder.rep_fixed64.get(0)).isEqualTo(new Long(119L));
    assertThat(builder.rep_fixed64.get(1)).isEqualTo(new Long(119L));
    assertThat(builder.rep_sfixed64).hasSize(2);
    assertThat(builder.rep_sfixed64.get(0)).isEqualTo(new Long(120L));
    assertThat(builder.rep_sfixed64.get(1)).isEqualTo(new Long(120L));
    assertThat(builder.rep_bool).hasSize(2);
    assertThat(builder.rep_bool.get(0)).isEqualTo(Boolean.TRUE);
    assertThat(builder.rep_bool.get(1)).isEqualTo(Boolean.TRUE);
    assertThat(builder.rep_float).hasSize(2);
    assertThat(builder.rep_float.get(0)).isEqualTo(new Float(122.0F));
    assertThat(builder.rep_float.get(1)).isEqualTo(new Float(122.0F));
    assertThat(builder.rep_double).hasSize(2);
    assertThat(builder.rep_double.get(0)).isEqualTo(new Double(123.0));
    assertThat(builder.rep_double.get(1)).isEqualTo(new Double(123.0));
    assertThat(builder.rep_string).hasSize(2);
    assertThat(builder.rep_string.get(0)).isEqualTo("124");
    assertThat(builder.rep_string.get(1)).isEqualTo("124");
    assertThat(builder.rep_bytes).hasSize(2);
    assertThat(builder.rep_bytes.get(0)).isEqualTo(ByteString.of((byte) 125, (byte) 225));
    assertThat(builder.rep_bytes.get(1)).isEqualTo(ByteString.of((byte) 125, (byte) 225));
    assertThat(builder.rep_nested_enum).hasSize(2);
    assertThat(builder.rep_nested_enum.get(0)).isEqualTo(A);
    assertThat(builder.rep_nested_enum.get(1)).isEqualTo(A);
    assertThat(builder.rep_nested_message).hasSize(2);
    assertThat(builder.rep_nested_message.get(0)).isEqualTo(nestedMessage);
    assertThat(builder.rep_nested_message.get(1)).isEqualTo(nestedMessage);

    assertThat(builder.pack_int32).hasSize(2);
    assertThat(builder.pack_int32.get(0)).isEqualTo(new Integer(111));
    assertThat(builder.pack_int32.get(1)).isEqualTo(new Integer(111));
    assertThat(builder.pack_uint32).hasSize(2);
    assertThat(builder.pack_uint32.get(0)).isEqualTo(new Integer(112));
    assertThat(builder.pack_uint32.get(1)).isEqualTo(new Integer(112));
    assertThat(builder.pack_sint32).hasSize(2);
    assertThat(builder.pack_sint32.get(0)).isEqualTo(new Integer(113));
    assertThat(builder.pack_sint32.get(1)).isEqualTo(new Integer(113));
    assertThat(builder.pack_fixed32).hasSize(2);
    assertThat(builder.pack_fixed32.get(0)).isEqualTo(new Integer(114));
    assertThat(builder.pack_fixed32.get(1)).isEqualTo(new Integer(114));
    assertThat(builder.pack_sfixed32).hasSize(2);
    assertThat(builder.pack_sfixed32.get(0)).isEqualTo(new Integer(115));
    assertThat(builder.pack_sfixed32.get(1)).isEqualTo(new Integer(115));
    assertThat(builder.pack_int64).hasSize(2);
    assertThat(builder.pack_int64.get(0)).isEqualTo(new Long(116L));
    assertThat(builder.pack_int64.get(1)).isEqualTo(new Long(116L));
    assertThat(builder.pack_uint64).hasSize(2);
    assertThat(builder.pack_uint64.get(0)).isEqualTo(new Long(117L));
    assertThat(builder.pack_uint64.get(1)).isEqualTo(new Long(117L));
    assertThat(builder.pack_sint64).hasSize(2);
    assertThat(builder.pack_sint64.get(0)).isEqualTo(new Long(118L));
    assertThat(builder.pack_sint64.get(1)).isEqualTo(new Long(118L));
    assertThat(builder.pack_fixed64).hasSize(2);
    assertThat(builder.pack_fixed64.get(0)).isEqualTo(new Long(119L));
    assertThat(builder.pack_fixed64.get(1)).isEqualTo(new Long(119L));
    assertThat(builder.pack_sfixed64).hasSize(2);
    assertThat(builder.pack_sfixed64.get(0)).isEqualTo(new Long(120L));
    assertThat(builder.pack_sfixed64.get(1)).isEqualTo(new Long(120L));
    assertThat(builder.pack_bool).hasSize(2);
    assertThat(builder.pack_bool.get(0)).isEqualTo(Boolean.TRUE);
    assertThat(builder.pack_bool.get(1)).isEqualTo(Boolean.TRUE);
    assertThat(builder.pack_float).hasSize(2);
    assertThat(builder.pack_float.get(0)).isEqualTo(new Float(122.0F));
    assertThat(builder.pack_float.get(1)).isEqualTo(new Float(122.0F));
    assertThat(builder.pack_double).hasSize(2);
    assertThat(builder.pack_double.get(0)).isEqualTo(new Double(123.0));
    assertThat(builder.pack_double.get(1)).isEqualTo(new Double(123.0));
    assertThat(builder.pack_nested_enum).hasSize(2);
    assertThat(builder.pack_nested_enum.get(0)).isEqualTo(A);
    assertThat(builder.pack_nested_enum.get(1)).isEqualTo(A);

    assertThat(builder.ext_opt_bool).isEqualTo(Boolean.TRUE);
    assertThat(builder.ext_rep_bool).isEqualTo(list(true));
    assertThat(builder.ext_pack_bool).isEqualTo(list(true));

    builder.ext_opt_bool(false);
    builder.ext_rep_bool(list(false));
    builder.ext_pack_bool(list(false));

    assertThat(builder.ext_opt_bool).isEqualTo(Boolean.FALSE);
    assertThat(builder.ext_rep_bool).isEqualTo(list(false));
    assertThat(builder.ext_pack_bool).isEqualTo(list(false));
  }

  @Test
  public void testInitBuilder() {
    AllTypes.Builder builder = allTypes.newBuilder();
    assertThat(builder.build()).isEqualTo(allTypes);
    builder.opt_bool = false;
    assertThat(builder.build()).isNotSameAs(allTypes);
  }

  @Test
  public void testWrite() {
    byte[] output = adapter.encode(allTypes);
    assertThat(output.length).isEqualTo(TestAllTypesData.expectedOutput.size());
    assertThat(ByteString.of(output)).isEqualTo(TestAllTypesData.expectedOutput);
  }

  @Test
  public void testWriteSource() throws IOException {
    Buffer sink = new Buffer();
    adapter.encode(sink, allTypes);
    assertThat(sink.readByteString()).isEqualTo(TestAllTypesData.expectedOutput);
  }

  @Test
  public void testWriteBytes() throws IOException {
    byte[] output = adapter.encode(allTypes);
    assertThat(output.length).isEqualTo(TestAllTypesData.expectedOutput.size());
    assertThat(ByteString.of(output)).isEqualTo(TestAllTypesData.expectedOutput);
  }

  @Test
  public void testWriteStream() throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    adapter.encode(stream, allTypes);
    byte[] output = stream.toByteArray();
    assertThat(output.length).isEqualTo(TestAllTypesData.expectedOutput.size());
    assertThat(ByteString.of(output)).isEqualTo(TestAllTypesData.expectedOutput);
  }

  @Test
  public void testReadSource() throws IOException {
    byte[] data = adapter.encode(allTypes);
    Buffer input = new Buffer().write(data);

    AllTypes parsed = adapter.decode(input);
    assertThat(parsed).isEqualTo(allTypes);

    assertThat(allTypes.ext_opt_bool).isEqualTo(Boolean.TRUE);
    assertThat(allTypes.ext_rep_bool).isEqualTo(list(true));
    assertThat(allTypes.ext_pack_bool).isEqualTo(list(true));
  }

  @Test
  public void testReadBytes() throws IOException {
    byte[] data = adapter.encode(allTypes);

    AllTypes parsed = adapter.decode(data);
    assertThat(parsed).isEqualTo(allTypes);

    assertThat(allTypes.ext_opt_bool).isEqualTo(Boolean.TRUE);
    assertThat(allTypes.ext_rep_bool).isEqualTo(list(true));
    assertThat(allTypes.ext_pack_bool).isEqualTo(list(true));
  }

  @Test
  public void testReadStream() throws IOException {
    byte[] data = adapter.encode(allTypes);
    InputStream stream = new ByteArrayInputStream(data);

    AllTypes parsed = adapter.decode(stream);
    assertThat(parsed).isEqualTo(allTypes);

    assertThat(allTypes.ext_opt_bool).isEqualTo(Boolean.TRUE);
    assertThat(allTypes.ext_rep_bool).isEqualTo(list(true));
    assertThat(allTypes.ext_pack_bool).isEqualTo(list(true));
  }

  @Test
  public void testReadLongMessages() throws IOException {
    AllTypes allTypes = createAllTypes(50);
    byte[] data = adapter.encode(allTypes);

    AllTypes parsed = adapter.decode(data);
    assertThat(parsed).isEqualTo(allTypes);

    assertThat(allTypes.ext_opt_bool).isEqualTo(Boolean.TRUE);
    assertThat(allTypes.ext_rep_bool).isEqualTo(list(true, 50));
    assertThat(allTypes.ext_pack_bool).isEqualTo(list(true, 50));
  }

  /** A source that returns 1, 2, 3, or 4 bytes at a time. */
  private static class SlowSource extends ForwardingSource {
    private long pos;

    SlowSource(Source delegate) {
      super(delegate);
    }

    @Override public long read(Buffer sink, long byteCount) throws IOException {
      long bytesToReturn = Math.min(byteCount, (pos % 4) + 1);
      pos += bytesToReturn;
      return super.read(sink, byteCount);
    }
  }

  @Test
  public void testReadFromSlowSource() throws IOException {
    byte[] data = adapter.encode(allTypes);

    Source input = new SlowSource(new Buffer().write(data));
    AllTypes parsed = adapter.decode(Okio.buffer(input));
    assertThat(parsed).isEqualTo(allTypes);

    assertThat(allTypes.ext_opt_bool).isEqualTo(Boolean.TRUE);
    assertThat(allTypes.ext_rep_bool).isEqualTo(list(true));
    assertThat(allTypes.ext_pack_bool).isEqualTo(list(true));
  }

  @Test
  public void testReadNoExtension() throws IOException {
    byte[] data = adapter.encode(allTypes);
    AllTypes parsed = AllTypes.ADAPTER.decode(data);
    assertThat(allTypes).isEqualTo(parsed);
  }

  @Test
  public void testReadNonPacked() throws IOException {
    AllTypes parsed = adapter.decode(
        new Buffer().write(TestAllTypesData.nonPacked));
    assertThat(parsed).isEqualTo(allTypes);
  }

  @Test
  public void testToString() throws IOException {
    byte[] data = adapter.encode(allTypes);
    AllTypes parsed = adapter.decode(data);
    assertThat(parsed.toString()).isEqualTo(TestAllTypesData.expectedToString);
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
      assertThat(e.getMessage()).isEqualTo("Required field not set:\n  req_bool");
    }

    builder.req_int32(null);
    builder.req_sfixed64(null);
    try {
      builder.build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage(
          "Required fields not set:\n  req_int32\n  req_sfixed64\n  req_bool");
    }
  }

  @Test
  public void testDefaults() throws Exception {
    assertThat(AllTypes.DEFAULT_DEFAULT_BOOL).isEqualTo((Object) true);
    // original: "<c-cedilla>ok\a\b\f\n\r\t\v\1\01\001\17\017\176\x1\x01\x11\X1\X01\X11g<u umlaut>zel"
    assertThat(AllTypes.DEFAULT_DEFAULT_STRING).isEqualTo( "çok\u0007\b\f\n\r\t\u000b\u0001\u0001"
        + "\u0001\u000f\u000f~\u0001\u0001\u0011\u0001\u0001\u0011güzel");
    assertThat(new String(AllTypes.DEFAULT_DEFAULT_BYTES.toByteArray(), "ISO-8859-1")).isEqualTo(
        "çok\u0007\b\f\n\r\t\u000b\u0001\u0001\u0001\u000f\u000f~\u0001\u0001\u0011\u0001\u0001"
            + "\u0011güzel");
  }

  @Test
  public void testEnums() {
    assertThat(AllTypes.NestedEnum.fromValue(1)).isEqualTo(A);
    assertThat(AllTypes.NestedEnum.fromValue(10)).isNull();
    assertThat(A.getValue()).isEqualTo(1);
  }

  @Test
  public void testSkipGroup() throws IOException {
    byte[] data =  new byte[TestAllTypesData.expectedOutput.size() + 27];
    System.arraycopy(TestAllTypesData.expectedOutput.toByteArray(), 0, data, 0, 17);
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

    System.arraycopy(TestAllTypesData.expectedOutput.toByteArray(), 17, data, index,
        TestAllTypesData.expectedOutput.size() - 17);

    AllTypes parsed = adapter.decode(data);
    assertThat(parsed).isEqualTo(allTypes);
  }

  @Test
  public void testUnknownFields() {
    AllTypes.Builder builder = getBuilder();
    builder.addUnknownField(10000, FieldEncoding.VARINT, 1L);
    AllTypes withUnknownField = builder.build();
    byte[] data = adapter.encode(withUnknownField);
    int count = TestAllTypesData.expectedOutput.size();
    assertThat(data.length).isEqualTo(count + 4);
    assertThat(data[count]).isEqualTo((byte) 0x80);
    assertThat(data[count + 1]).isEqualTo((byte) 0xf1);
    assertThat(data[count + 2]).isEqualTo((byte) 0x04);
    assertThat(data[count + 3]).isEqualTo((byte) 0x01);
  }

  @Test @Ignore("we no longer enforce this constraint")
  public void testUnknownFieldsTypeMismatch() {
    AllTypes.Builder builder = getBuilder();
    builder.addUnknownField(10000, FieldEncoding.VARINT, 1);
    try {
      // Don't allow heterogeneous types for the same tag
      builder.addUnknownField(10000, FieldEncoding.FIXED32, 2);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage(
          "Wire type FIXED32 differs from previous type VARINT for tag 10000");
    }
  }

  @Test
  public void testNullInRepeated() {
    try {
      // A null value for a repeated field is not allowed.
      getBuilder().rep_nested_enum(Arrays.asList(A, null, A));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("Element at index 1 is null");
    }
  }

  @Test
  public void testNullRepeated() {
    try {
      // A null value for a repeated field is not allowed.
      getBuilder().rep_nested_enum(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("list == null");
    }
  }
}
