// Copyright 2013 Square, Inc.
package com.google.protobuf;

import com.squareup.omar.Omar;
import com.squareup.protos.alltypes.AllTypesContainer;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

import static com.squareup.protos.alltypes.AllTypesContainer.AllTypes;

public class TestAllTypes extends TestCase {

  private static final int[] expectedOutput = {
    // optional

    0x8, // tag = 1, type = 0
    0x6f, // value = 111
    0x10, // tag = 2, type = 0
    0x70, // value = 112
    0x18, // tag = 3, type = 0
    0xe2, 0x01, // value = 226 (=113 zig-zag)
    0x25, // tag = 4, type = 5
    0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)
    0x2d, // tag = 5, type = 5
    0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)
    0x30, // tag = 6, type = 0
    0x74, // value = 116
    0x38, // tag = 7, type = 0
    0x75, // value = 117
    0x40, // tag = 8, type = 0
    0xec, 0x01, // value = 236 (=118 zigzag)
    0x49, // tag = 9, type = 1
    0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119
    0x51, // tag = 10, type = 1
    0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120
    0x58, // tag = 11, type = 0
    0x01, // value = 1 (true)
    0x65, // tag = 12, type = 5
    0x00, 0x00, 0xf4, 0x42, // value = 122.0F
    0x69, // tag = 13, type = 1
    0x00, 0x00, 0x00, 0x00, 0x00, 0xc0, 0x5e, 0x40, // value = 123.0
    0x72, // tag = 14, type = 2
    0x03, // length = 3
    0x31, 0x32, 0x34, // value = "124"
    0x7a, // tag = 15, type = 2
    0x02, // length = 2
    0x7d, 0xe1, // value = { 125, 225 }
    0x80, 0x01, // tag = 16, type = 0
    0x01, // value = 1
    0x8a, 0x01, // tag = 17, type = 2
    0x03, // length = 3
    0x08, // nested tag = 1, type = 0
    0xe7, 0x7, // value = 999

    // required

    0xa8, 0x06, // tag = 101, type = 0
    0x6f, // value = 111
    0xb0, 0x06, // tag = 102, type = 0
    0x70, // value = 112
    0xb8, 0x06, // tag = 103, type = 0
    0xe2, 0x01, // value = 226 (=113 zig-zag)
    0xc5, 0x06, // tag = 104, type = 5
    0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)
    0xcd, 0x06, // tag = 105, type = 5
    0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)
    0xd0, 0x06, // tag = 106, type = 0
    0x74, // value = 116
    0xd8, 0x06, // tag = 107, type = 0
    0x75, // value = 117
    0xe0, 0x06, // tag = 108, type = 0
    0xec, 0x01, // value = 236 (=118 zigzag)
    0xe9, 0x06, // tag = 109, type = 1
    0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119
    0xf1, 0x06, // tag = 110, type = 1
    0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120
    0xf8, 0x06, // tag = 111, type = 0
    0x01, // value = 1 (true)
    0x85, 0x07, // tag = 112, type = 5
    0x00, 0x00, 0xf4, 0x42, // value = 122.0F
    0x89, 0x07, // tag = 113, type = 1
    0x00, 0x00, 0x00, 0x00, 0x00, 0xc0, 0x5e, 0x40, // value = 123.0
    0x92, 0x07, // tag = 114, type = 2
    0x03, // length = 3
    0x31, 0x32, 0x34, // value = "124"
    0x9a, 0x07, // tag = 115, type = 2
    0x02, // length = 2
    0x7d, 0xe1, // value = { 125, 225 }
    0xa0, 0x07, // tag = 116, type = 0
    0x01, // value = 1
    0xaa, 0x07, // tag = 117, type = 2
    0x03, // length = 3
    0x08, // nested tag = 1, type = 0
    0xe7, 0x07, // value = 999

    // repeated

    0xc8, 0x0c, // tag = 201, type = 0
    0x6f, // value = 111
    0xc8, 0x0c, // tag = 201, type = 0
    0x6f, // value = 111

    0xd0, 0x0c, // tag = 202, type = 0
    0x70, // value = 112
    0xd0, 0x0c, // tag = 202, type = 0
    0x70, // value = 112

    0xd8, 0x0c, // tag = 203, type = 0
    0xe2, 0x01, // value = 226 (=113 zig-zag)
    0xd8, 0x0c, // tag = 203, type = 0
    0xe2, 0x01, // value = 226 (=113 zig-zag)

    0xe5, 0x0c, // tag = 204, type = 5
    0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)
    0xe5, 0x0c, // tag = 204, type = 5
    0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)

    0xed, 0x0c, // tag = 205, type = 5
    0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)
    0xed, 0x0c, // tag = 205, type = 5
    0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)

    0xf0, 0x0c, // tag = 206, type = 0
    0x74, // value = 116
    0xf0, 0x0c, // tag = 206, type = 0
    0x74, // value = 116

    0xf8, 0x0c, // tag = 207, type = 0
    0x75, // value = 117
    0xf8, 0x0c, // tag = 207, type = 0
    0x75, // value = 117

    0x80, 0x0d, // tag = 208, type = 0
    0xec, 0x01, // value = 236 (=118 zigzag)
    0x80, 0x0d, // tag = 208, type = 0
    0xec, 0x01, // value = 236 (=118 zigzag)

    0x89, 0x0d, // tag = 209, type = 1
    0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119
    0x89, 0x0d, // tag = 209, type = 1
    0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119

    0x91, 0x0d, // tag = 210, type = 1
    0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120
    0x91, 0x0d, // tag = 210, type = 1
    0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120

    0x98, 0x0d, // tag = 211, type = 0
    0x01, // value = 1 (true)
    0x98, 0x0d, // tag = 211, type = 0
    0x01, // value = 1 (true)

    0xa5, 0x0d, // tag = 212, type = 5
    0x00, 0x00, 0xf4, 0x42, // value = 122.0F
    0xa5, 0x0d, // tag = 212, type = 5
    0x00, 0x00, 0xf4, 0x42, // value = 122.0F

    0xa9, 0x0d, // tag = 213, type = 1
    0x00, 0x00, 0x00, 0x00, 0x00, 0xc0, 0x5e, 0x40, // value = 123.0
    0xa9, 0x0d, // tag = 213, type = 1
    0x00, 0x00, 0x00, 0x00, 0x00, 0xc0, 0x5e, 0x40, // value = 123.0

    0xb2, 0x0d, // tag = 214, type = 2
    0x03, // length = 3
    0x31, 0x32, 0x34, // value = "124"
    0xb2, 0x0d, // tag = 214, type = 2
    0x03, // length = 3
    0x31, 0x32, 0x34, // value = "124"

    0xba, 0x0d, // tag = 215, type = 2
    0x02, // length = 2
    0x7d, 0xe1, // value = { 125, 225 }
    0xba, 0x0d, // tag = 215, type = 2
    0x02, // length = 2
    0x7d, 0xe1, // value = { 125, 225 }

    0xc0, 0x0d, // tag = 216, type = 0
    0x01, // value = 1
    0xc0, 0x0d, // tag = 216, type = 0
    0x01, // value = 1

    0xca, 0x0d, // tag = 217, type = 2
    0x03, // length = 3
    0x08, // nested tag = 1, type = 0
    0xe7, 0x07, // value = 999
    0xca, 0x0d, // tag = 217, type = 2
    0x03, // length = 3
    0x08, // nested tag = 1, type = 0
    0xe7, 0x07, // value = 999

    // packed

    0xea, 0x12, // tag = 301, type = 2
    0x02, // length = 2
    0x6f, // value = 111
    0x6f, // value = 111

    0xf2, 0x12, // tag = 302, type = 2
    0x02, // length = 2
    0x70, // value = 112
    0x70, // value = 112

    0xfa, 0x12, // tag = 303, type = 2
    0x04, // length = 4
    0xe2, 0x01, // value = 226 (=113 zig-zag)
    0xe2, 0x01, // value = 226 (=113 zig-zag)

    0x82, 0x13, // tag = 304, type = 2
    0x08, // length = 8
    0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)
    0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)

    0x8a, 0x13, // tag = 305, type = 2
    0x08, // length = 8
    0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)
    0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)

    0x92, 0x13, // tag = 306, type = 2
    0x02, // length = 2
    0x74, // value = 116
    0x74, // value = 116

    0x9a, 0x13, // tag = 307, type = 2
    0x02, // length = 2
    0x75, // value = 117
    0x75, // value = 117

    0xa2, 0x13, // tag = 308, type = 2
    0x04, // length = 4
    0xec, 0x01, // value = 236 (=118 zigzag)
    0xec, 0x01, // value = 236 (=118 zigzag)

    0xaa, 0x13, // tag = 309, type = 2
    0x10, // length = 16
    0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119
    0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119

    0xb2, 0x13, // tag = 310, type = 2
    0x10, // length = 16
    0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120
    0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120

    0xba, 0x13, // tag = 311, type = 2
    0x02, // length = 2
    0x01, // value = 1 (true)
    0x01, // value = 1 (true)

    0xc2, 0x13, // tag = 312, type = 2
    0x08, // length = 8
    0x00, 0x00, 0xf4, 0x42, // value = 122.0F
    0x00, 0x00, 0xf4, 0x42, // value = 122.0F

    0xca, 0x13, // tag = 313, type = 2
    0x10, // length = 16
    0x00, 0x00, 0x00, 0x00, 0x00, 0xc0, 0x5e, 0x40, // value = 123.0
    0x00, 0x00, 0x00, 0x00, 0x00, 0xc0, 0x5e, 0x40, // value = 123.0

    // string cannot be packed
    0xd2, 0x13, // tag = 314, type = 2
    0x03, // length = 3
    0x31, 0x32, 0x34, // value = "124"
    0xd2, 0x13, // tag = 314, type = 2
    0x03, // length = 3
    0x31, 0x32, 0x34, // value = "124"

    // bytes cannot be packed
    0xda, 0x13, // tag = 315, type = 2
    0x02, // length = 2
    0x7d, 0xe1, // value = { 125, 225 }
    0xda, 0x13, // tag = 315, type = 2
    0x02, // length = 2
    0x7d, 0xe1, // value = { 125, 225 }

    0xe2, 0x13, // tag = 316, type = 2
    0x2, // length = 2
    0x01, // value = 1
    0x01, // value = 1

    // messages cannot be packed
    0xea, 0x13, // tag = 317, type = 2
    0x03, // length = 3
    0x08, // nested tag = 1, type = 0
    0xe7, 0x07, // value = 999
    0xea, 0x13, // tag = 317, type = 2
    0x03, // length = 3
    0x08, // nested tag = 1, type = 0
    0xe7, 0x07 // value = 999
  };

  private String hex(int x) {
    return "0x" + Integer.toHexString(x);
  }

  private int varint(byte[] data, int off) {
    byte tmp = data[off++];
    if (tmp >= 0) {
      return tmp;
    }
    int result = tmp & 0x7f;
    if (off < data.length && (tmp = data[off++]) >= 0) {
      result |= tmp << 7;
    } else {
      result |= (tmp & 0x7f) << 7;
      if (off < data.length && (tmp = data[off++]) >= 0) {
        result |= tmp << 14;
      } else {
        result |= (tmp & 0x7f) << 14;
        if (off < data.length && (tmp = data[off++]) >= 0) {
          result |= tmp << 21;
        } else {
          result |= (tmp & 0x7f) << 21;
          if (off < data.length) {
            result |= (tmp = data[off++]) << 28;
          }
          if (tmp < 0) {
            // Discard upper 32 bits.
            for (int i = 0; i < 5; i++) {
              if (off < data.length && (data[off++]) >= 0) {
                return result;
              }
            }
            return -999;
          }
        }
      }
    }
    return result;
  }

  private int vlen(byte[] data, int off) {
    int len = 1;
    while (off < data.length && data[off++] < 0) {
      len++;
    }
    return len;
  }

  private <T> List<T> array(T x) {
    return Arrays.asList(x, x);
  }

  public void testAll() {
    byte[] bytes = { (byte) 125, (byte) 225 };

    AllTypesContainer.AllTypes.NestedMessage nestedMessage =
        new AllTypes.NestedMessage.Builder().a(999).build();

    AllTypes allTypes = new AllTypes.Builder()
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

        .build();

    int len = Omar.getSerializedSize(allTypes);
    // assertEquals(expectedOutput.length, len);
    byte[] output = new byte[len];
    Omar.writeTo(allTypes, output, 0, len);

    for (int i = 0; i < output.length; i++) {
      if ((output[i] & 0xff) != expectedOutput[i]) {
        System.out.print(" * ");
      }
      int varint = varint(output, i);
      System.out.println("[" + i + "] = " + (output[i] & 0xff) + " " + hex(output[i] & 0xff) +
          " / varint " + "(" + vlen(output, i) + "): " + varint + " (= " + (varint / 8) + ", " + (varint % 8) + ")" +
          " - expected " + expectedOutput[i] + " " + hex(expectedOutput[i]));
    }

    for (int i = 0; i < output.length; i++) {
      assertEquals("Byte " + i, expectedOutput[i], output[i] & 0xff);
    }
  }
}
