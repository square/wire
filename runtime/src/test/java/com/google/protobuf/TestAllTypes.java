// Copyright 2013 Square, Inc.
package com.google.protobuf;

import com.squareup.omar.Omar;
import com.squareup.protos.alltypes.AllTypesContainer;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

import static com.squareup.protos.alltypes.AllTypesContainer.AllTypes;

public class TestAllTypes extends TestCase {

  // Return a two-element array with a given repeated value
  private <T> List<T> array(T x) {
    return Arrays.asList(x, x);
  }

  private final AllTypes allTypes = createAllTypes();
  private final Omar omar = new Omar(AllTypesContainer.class);

  private AllTypes createAllTypes() {
    byte[] bytes = { (byte) 125, (byte) 225 };
    AllTypesContainer.AllTypes.NestedMessage nestedMessage =
        new AllTypes.NestedMessage.Builder().a(999).build();
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
        .opt_nested_enum(AllTypes.NestedEnum.A)
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
        .req_nested_enum(AllTypes.NestedEnum.A)
        .req_nested_message(nestedMessage)
        .rep_int32(array(111))
        .rep_uint32(array(112))
        .rep_sint32(array(113))
        .rep_fixed32(array(114))
        .rep_sfixed32(array(115))
        .rep_int64(array(116L))
        .rep_uint64(array(117L))
        .rep_sint64(array(118L))
        .rep_fixed64(array(119L))
        .rep_sfixed64(array(120L))
        .rep_bool(array(true))
        .rep_float(array(122.0F))
        .rep_double(array(123.0))
        .rep_string(array("124"))
        .rep_bytes(array(bytes))
        .rep_nested_enum(array(AllTypes.NestedEnum.A))
        .rep_nested_message(array(nestedMessage))
        .pack_int32(array(111))
        .pack_uint32(array(112))
        .pack_sint32(array(113))
        .pack_fixed32(array(114))
        .pack_sfixed32(array(115))
        .pack_int64(array(116L))
        .pack_uint64(array(117L))
        .pack_sint64(array(118L))
        .pack_fixed64(array(119L))
        .pack_sfixed64(array(120L))
        .pack_bool(array(true))
        .pack_float(array(122.0F))
        .pack_double(array(123.0))
        .pack_string(array("124"))
        .pack_bytes(array(bytes))
        .pack_nested_enum(array(AllTypes.NestedEnum.A))
        .pack_nested_message(array(nestedMessage))
        .setExtension(AllTypesContainer.ext_pack_bool, array(true))
        .build();
  }

  public void testWrite() {
    int len = omar.getSerializedSize(allTypes);
    assertEquals(AllTypesData.expectedOutput.length, len);
    byte[] output = new byte[len];
    omar.writeTo(allTypes, output, 0, len);
    for (int i = 0; i < output.length; i++) {
      assertEquals("Byte " + i, AllTypesData.expectedOutput[i], output[i] & 0xff);
    }
  }

  public void testRead() throws IOException {
    byte[] data = new byte[AllTypesData.expectedOutput.length];
    omar.writeTo(allTypes, data, 0, data.length);
    AllTypes parsed = omar.parseFrom(AllTypes.class, data);
    assertEquals(allTypes, parsed);
  }

  public void testReadNoExtension() throws IOException {
    byte[] data = new byte[AllTypesData.expectedOutput.length];
    omar.writeTo(allTypes, data, 0, data.length);
    AllTypes parsed = new Omar().parseFrom(AllTypes.class, data);
    assertFalse(allTypes.equals(parsed));
  }

  public void testReadNonPacked() throws IOException {
    byte[] data = new byte[AllTypesData.nonPacked.length];
    for (int i = 0; i < AllTypesData.nonPacked.length; i++) {
      data[i] = (byte) AllTypesData.nonPacked[i];
    }
    AllTypes parsed = omar.parseFrom(AllTypes.class, data);
    assertEquals(allTypes, parsed);
  }

  public void testToString() throws IOException {
    byte[] data = new byte[AllTypesData.expectedOutput.length];
    omar.writeTo(allTypes, data, 0, data.length);
    AllTypes parsed = omar.parseFrom(AllTypes.class, data);
    assertEquals(AllTypesData.expectedToString, parsed.toString());
  }
}
