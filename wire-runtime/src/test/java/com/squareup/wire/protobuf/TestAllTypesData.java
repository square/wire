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

class TestAllTypesData {

  public static final String expectedToString =
      "AllTypes{opt_int32=111, opt_uint32=112, opt_sint32=113, opt_fixed32=114, opt_sfixed32=115, " +
      "opt_int64=116, opt_uint64=117, opt_sint64=118, opt_fixed64=119, opt_sfixed64=120, opt_bool=true, " +
      "opt_float=122.0, opt_double=123.0, opt_string=124, opt_bytes=ByteString[size=2 data=7de1], " +
      "opt_nested_enum=A, opt_nested_message=NestedMessage{a=999}, " +
      "req_int32=111, req_uint32=112, req_sint32=113, req_fixed32=114, req_sfixed32=115, req_int64=116, " +
      "req_uint64=117, req_sint64=118, req_fixed64=119, req_sfixed64=120, req_bool=true, req_float=122.0, " +
      "req_double=123.0, req_string=124, req_bytes=ByteString[size=2 data=7de1], req_nested_enum=A, " +
      "req_nested_message=NestedMessage{a=999}, rep_int32=[111, 111], rep_uint32=[112, 112], " +
      "rep_sint32=[113, 113], rep_fixed32=[114, 114], rep_sfixed32=[115, 115], rep_int64=[116, 116], " +
      "rep_uint64=[117, 117], rep_sint64=[118, 118], rep_fixed64=[119, 119], rep_sfixed64=[120, 120], " +
      "rep_bool=[true, true], rep_float=[122.0, 122.0], rep_double=[123.0, 123.0], " +
      "rep_string=[124, 124], rep_bytes=[ByteString[size=2 data=7de1], ByteString[size=2 data=7de1]], " +
      "rep_nested_enum=[A, A], rep_nested_message=[NestedMessage{a=999}, NestedMessage{a=999}], " +
      "pack_int32=[111, 111], pack_uint32=[112, 112], pack_sint32=[113, 113], " +
      "pack_fixed32=[114, 114], pack_sfixed32=[115, 115], pack_int64=[116, 116], " +
      "pack_uint64=[117, 117], pack_sint64=[118, 118], pack_fixed64=[119, 119], " +
      "pack_sfixed64=[120, 120], pack_bool=[true, true], pack_float=[122.0, 122.0], " +
      "pack_double=[123.0, 123.0], pack_nested_enum=[A, A], " +
      "{extensions={1011=true, 1111=[true, true], 1211=[true, true]}}}";

  public static final byte[] expectedOutput = {
      // optional

      0x08, // tag = 1, type = 0
      0x6f, // value = 111
      0x10, // tag = 2, type = 0
      0x70, // value = 112
      0x18, // tag = 3, type = 0
      (byte) 0xe2, 0x01, // value = 226 (=113 zig-zag)
      0x25, // tag = 4, type = 5
      0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)
      0x2d, // tag = 5, type = 5
      0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)
      0x30, // tag = 6, type = 0
      0x74, // value = 116
      0x38, // tag = 7, type = 0
      0x75, // value = 117
      0x40, // tag = 8, type = 0
      (byte) 0xec, 0x01, // value = 236 (=118 zigzag)
      0x49, // tag = 9, type = 1
      0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119
      0x51, // tag = 10, type = 1
      0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120
      0x58, // tag = 11, type = 0
      0x01, // value = 1 (true)
      0x65, // tag = 12, type = 5
      0x00, 0x00, (byte) 0xf4, 0x42, // value = 122.0F
      0x69, // tag = 13, type = 1
      0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x5e, 0x40, // value = 123.0
      0x72, // tag = 14, type = 2
      0x03, // length = 3
      0x31, 0x32, 0x34, // value = "124"
      0x7a, // tag = 15, type = 2
      0x02, // length = 2
      0x7d, (byte) 0xe1, // value = { 125, 225 }
      (byte) 0x80, 0x01, // tag = 16, type = 0
      0x01, // value = 1
      (byte) 0x8a, 0x01, // tag = 17, type = 2
      0x03, // length = 3
      0x08, // nested tag = 1, type = 0
      (byte) 0xe7, 0x7, // value = 999

      // required

      (byte) 0xa8, 0x06, // tag = 101, type = 0
      0x6f, // value = 111
      (byte) 0xb0, 0x06, // tag = 102, type = 0
      0x70, // value = 112
      (byte) 0xb8, 0x06, // tag = 103, type = 0
      (byte) 0xe2, 0x01, // value = 226 (=113 zig-zag)
      (byte) 0xc5, 0x06, // tag = 104, type = 5
      0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)
      (byte) 0xcd, 0x06, // tag = 105, type = 5
      0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)
      (byte) 0xd0, 0x06, // tag = 106, type = 0
      0x74, // value = 116
      (byte) 0xd8, 0x06, // tag = 107, type = 0
      0x75, // value = 117
      (byte) 0xe0, 0x06, // tag = 108, type = 0
      (byte) 0xec, 0x01, // value = 236 (=118 zigzag)
      (byte) 0xe9, 0x06, // tag = 109, type = 1
      0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119
      (byte) 0xf1, 0x06, // tag = 110, type = 1
      0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120
      (byte) 0xf8, 0x06, // tag = 111, type = 0
      0x01, // value = 1 (true)
      (byte) 0x85, 0x07, // tag = 112, type = 5
      0x00, 0x00, (byte) 0xf4, 0x42, // value = 122.0F
      (byte) 0x89, 0x07, // tag = 113, type = 1
      0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x5e, 0x40, // value = 123.0
      (byte) 0x92, 0x07, // tag = 114, type = 2
      0x03, // length = 3
      0x31, 0x32, 0x34, // value = "124"
      (byte) 0x9a, 0x07, // tag = 115, type = 2
      0x02, // length = 2
      0x7d, (byte) 0xe1, // value = { 125, 225 }
      (byte) 0xa0, 0x07, // tag = 116, type = 0
      0x01, // value = 1
      (byte) 0xaa, 0x07, // tag = 117, type = 2
      0x03, // length = 3
      0x08, // nested tag = 1, type = 0
      (byte) 0xe7, 0x07, // value = 999

      // repeated

      (byte) 0xc8, 0x0c, // tag = 201, type = 0
      0x6f, // value = 111
      (byte) 0xc8, 0x0c, // tag = 201, type = 0
      0x6f, // value = 111

      (byte) 0xd0, 0x0c, // tag = 202, type = 0
      0x70, // value = 112
      (byte) 0xd0, 0x0c, // tag = 202, type = 0
      0x70, // value = 112

      (byte) 0xd8, 0x0c, // tag = 203, type = 0
      (byte) 0xe2, 0x01, // value = 226 (=113 zig-zag)
      (byte) 0xd8, 0x0c, // tag = 203, type = 0
      (byte) 0xe2, 0x01, // value = 226 (=113 zig-zag)

      (byte) 0xe5, 0x0c, // tag = 204, type = 5
      0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)
      (byte) 0xe5, 0x0c, // tag = 204, type = 5
      0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)

      (byte) 0xed, 0x0c, // tag = 205, type = 5
      0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)
      (byte) 0xed, 0x0c, // tag = 205, type = 5
      0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)

      (byte) 0xf0, 0x0c, // tag = 206, type = 0
      0x74, // value = 116
      (byte) 0xf0, 0x0c, // tag = 206, type = 0
      0x74, // value = 116

      (byte) 0xf8, 0x0c, // tag = 207, type = 0
      0x75, // value = 117
      (byte) 0xf8, 0x0c, // tag = 207, type = 0
      0x75, // value = 117

      (byte) 0x80, 0x0d, // tag = 208, type = 0
      (byte) 0xec, 0x01, // value = 236 (=118 zigzag)
      (byte) 0x80, 0x0d, // tag = 208, type = 0
      (byte) 0xec, 0x01, // value = 236 (=118 zigzag)

      (byte) 0x89, 0x0d, // tag = 209, type = 1
      0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119
      (byte) 0x89, 0x0d, // tag = 209, type = 1
      0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119

      (byte) 0x91, 0x0d, // tag = 210, type = 1
      0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120
      (byte) 0x91, 0x0d, // tag = 210, type = 1
      0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120

      (byte) 0x98, 0x0d, // tag = 211, type = 0
      0x01, // value = 1 (true)
      (byte) 0x98, 0x0d, // tag = 211, type = 0
      0x01, // value = 1 (true)

      (byte) 0xa5, 0x0d, // tag = 212, type = 5
      0x00, 0x00, (byte) 0xf4, 0x42, // value = 122.0F
      (byte) 0xa5, 0x0d, // tag = 212, type = 5
      0x00, 0x00, (byte) 0xf4, 0x42, // value = 122.0F

      (byte) 0xa9, 0x0d, // tag = 213, type = 1
      0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x5e, 0x40, // value = 123.0
      (byte) 0xa9, 0x0d, // tag = 213, type = 1
      0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x5e, 0x40, // value = 123.0

      (byte) 0xb2, 0x0d, // tag = 214, type = 2
      0x03, // length = 3
      0x31, 0x32, 0x34, // value = "124"
      (byte) 0xb2, 0x0d, // tag = 214, type = 2
      0x03, // length = 3
      0x31, 0x32, 0x34, // value = "124"

      (byte) 0xba, 0x0d, // tag = 215, type = 2
      0x02, // length = 2
      0x7d, (byte) 0xe1, // value = { 125, 225 }
      (byte) 0xba, 0x0d, // tag = 215, type = 2
      0x02, // length = 2
      0x7d, (byte) 0xe1, // value = { 125, 225 }

      (byte) 0xc0, 0x0d, // tag = 216, type = 0
      0x01, // value = 1
      (byte) 0xc0, 0x0d, // tag = 216, type = 0
      0x01, // value = 1

      (byte) 0xca, 0x0d, // tag = 217, type = 2
      0x03, // length = 3
      0x08, // nested tag = 1, type = 0
      (byte) 0xe7, 0x07, // value = 999
      (byte) 0xca, 0x0d, // tag = 217, type = 2
      0x03, // length = 3
      0x08, // nested tag = 1, type = 0
      (byte) 0xe7, 0x07, // value = 999

      // packed

      (byte) 0xea, 0x12, // tag = 301, type = 2
      0x02, // length = 2
      0x6f, // value = 111
      0x6f, // value = 111

      (byte) 0xf2, 0x12, // tag = 302, type = 2
      0x02, // length = 2
      0x70, // value = 112
      0x70, // value = 112

      (byte) 0xfa, 0x12, // tag = 303, type = 2
      0x04, // length = 4
      (byte) 0xe2, 0x01, // value = 226 (=113 zig-zag)
      (byte) 0xe2, 0x01, // value = 226 (=113 zig-zag)

      (byte) 0x82, 0x13, // tag = 304, type = 2
      0x08, // length = 8
      0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)
      0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)

      (byte) 0x8a, 0x13, // tag = 305, type = 2
      0x08, // length = 8
      0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)
      0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)

      (byte) 0x92, 0x13, // tag = 306, type = 2
      0x02, // length = 2
      0x74, // value = 116
      0x74, // value = 116

      (byte) 0x9a, 0x13, // tag = 307, type = 2
      0x02, // length = 2
      0x75, // value = 117
      0x75, // value = 117

      (byte) 0xa2, 0x13, // tag = 308, type = 2
      0x04, // length = 4
      (byte) 0xec, 0x01, // value = 236 (=118 zigzag)
      (byte) 0xec, 0x01, // value = 236 (=118 zigzag)

      (byte) 0xaa, 0x13, // tag = 309, type = 2
      0x10, // length = 16
      0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119
      0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119

      (byte) 0xb2, 0x13, // tag = 310, type = 2
      0x10, // length = 16
      0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120
      0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120

      (byte) 0xba, 0x13, // tag = 311, type = 2
      0x02, // length = 2
      0x01, // value = 1 (true)
      0x01, // value = 1 (true)

      (byte) 0xc2, 0x13, // tag = 312, type = 2
      0x08, // length = 8
      0x00, 0x00, (byte) 0xf4, 0x42, // value = 122.0F
      0x00, 0x00, (byte) 0xf4, 0x42, // value = 122.0F

      (byte) 0xca, 0x13, // tag = 313, type = 2
      0x10, // length = 16
      0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x5e, 0x40, // value = 123.0
      0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x5e, 0x40, // value = 123.0

      (byte) 0xe2, 0x13, // tag = 316, type = 2
      0x2, // length = 2
      0x01, // value = 1
      0x01, // value = 1

      // extensions

      (byte) 0x98, 0x3f, // tag = 1011, type = 0
      0x01, // value = 1 (true)

      (byte) 0xb8, 0x45, // tag = 1111, type = 0
      0x01, // value = 1 (true)
      (byte) 0xb8, 0x45, // tag = 1111, type = 0
      0x01, // value = 1 (true)

      (byte) 0xda, 0x4b, // tag = 1211, type = 2
      0x02, // length = 2
      0x01, // value = 1 (true)
      0x01, // value = 1 (true)
  };

  // message with 'packed' fields stored non-packed, must still be readable
  public static final byte[] nonPacked = {
      // optional

      0x8, // tag = 1, type = 0
      0x6f, // value = 111
      0x10, // tag = 2, type = 0
      0x70, // value = 112
      0x18, // tag = 3, type = 0
      (byte) 0xe2, 0x01, // value = 226 (=113 zig-zag)
      0x25, // tag = 4, type = 5
      0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)
      0x2d, // tag = 5, type = 5
      0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)
      0x30, // tag = 6, type = 0
      0x74, // value = 116
      0x38, // tag = 7, type = 0
      0x75, // value = 117
      0x40, // tag = 8, type = 0
      (byte) 0xec, 0x01, // value = 236 (=118 zigzag)
      0x49, // tag = 9, type = 1
      0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119
      0x51, // tag = 10, type = 1
      0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120
      0x58, // tag = 11, type = 0
      0x01, // value = 1 (true)
      0x65, // tag = 12, type = 5
      0x00, 0x00, (byte) 0xf4, 0x42, // value = 122.0F
      0x69, // tag = 13, type = 1
      0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x5e, 0x40, // value = 123.0
      0x72, // tag = 14, type = 2
      0x03, // length = 3
      0x31, 0x32, 0x34, // value = "124"
      0x7a, // tag = 15, type = 2
      0x02, // length = 2
      0x7d, (byte) 0xe1, // value = { 125, 225 }
      (byte) 0x80, 0x01, // tag = 16, type = 0
      0x01, // value = 1
      (byte) 0x8a, 0x01, // tag = 17, type = 2
      0x03, // length = 3
      0x08, // nested tag = 1, type = 0
      (byte)  0xe7, 0x7, // value = 999

      // required

      (byte) 0xa8, 0x06, // tag = 101, type = 0
      0x6f, // value = 111
      (byte) 0xb0, 0x06, // tag = 102, type = 0
      0x70, // value = 112
      (byte) 0xb8, 0x06, // tag = 103, type = 0
      (byte) 0xe2, 0x01, // value = 226 (=113 zig-zag)
      (byte) 0xc5, 0x06, // tag = 104, type = 5
      0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)
      (byte) 0xcd, 0x06, // tag = 105, type = 5
      0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)
      (byte) 0xd0, 0x06, // tag = 106, type = 0
      0x74, // value = 116
      (byte) 0xd8, 0x06, // tag = 107, type = 0
      0x75, // value = 117
      (byte) 0xe0, 0x06, // tag = 108, type = 0
      (byte) 0xec, 0x01, // value = 236 (=118 zigzag)
      (byte) 0xe9, 0x06, // tag = 109, type = 1
      0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119
      (byte) 0xf1, 0x06, // tag = 110, type = 1
      0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120
      (byte) 0xf8, 0x06, // tag = 111, type = 0
      0x01, // value = 1 (true)
      (byte) 0x85, 0x07, // tag = 112, type = 5
      0x00, 0x00, (byte) 0xf4, 0x42, // value = 122.0F
      (byte) 0x89, 0x07, // tag = 113, type = 1
      0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x5e, 0x40, // value = 123.0
      (byte) 0x92, 0x07, // tag = 114, type = 2
      0x03, // length = 3
      0x31, 0x32, 0x34, // value = "124"
      (byte) 0x9a, 0x07, // tag = 115, type = 2
      0x02, // length = 2
      0x7d, (byte) 0xe1, // value = { 125, 225 }
      (byte) 0xa0, 0x07, // tag = 116, type = 0
      0x01, // value = 1
      (byte) 0xaa, 0x07, // tag = 117, type = 2
      0x03, // length = 3
      0x08, // nested tag = 1, type = 0
      (byte) 0xe7, 0x07, // value = 999

      // repeated

      (byte) 0xc8, 0x0c, // tag = 201, type = 0
      0x6f, // value = 111
      (byte) 0xc8, 0x0c, // tag = 201, type = 0
      0x6f, // value = 111

      (byte) 0xd0, 0x0c, // tag = 202, type = 0
      0x70, // value = 112
      (byte) 0xd0, 0x0c, // tag = 202, type = 0
      0x70, // value = 112

      (byte) 0xd8, 0x0c, // tag = 203, type = 0
      (byte) 0xe2, 0x01, // value = 226 (=113 zig-zag)
      (byte) 0xd8, 0x0c, // tag = 203, type = 0
      (byte) 0xe2, 0x01, // value = 226 (=113 zig-zag)

      (byte) 0xe5, 0x0c, // tag = 204, type = 5
      0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)
      (byte) 0xe5, 0x0c, // tag = 204, type = 5
      0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)

      (byte) 0xed, 0x0c, // tag = 205, type = 5
      0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)
      (byte) 0xed, 0x0c, // tag = 205, type = 5
      0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)

      (byte) 0xf0, 0x0c, // tag = 206, type = 0
      0x74, // value = 116
      (byte) 0xf0, 0x0c, // tag = 206, type = 0
      0x74, // value = 116

      (byte) 0xf8, 0x0c, // tag = 207, type = 0
      0x75, // value = 117
      (byte) 0xf8, 0x0c, // tag = 207, type = 0
      0x75, // value = 117

      (byte) 0x80, 0x0d, // tag = 208, type = 0
      (byte) 0xec, 0x01, // value = 236 (=118 zigzag)
      (byte) 0x80, 0x0d, // tag = 208, type = 0
      (byte) 0xec, 0x01, // value = 236 (=118 zigzag)

      (byte) 0x89, 0x0d, // tag = 209, type = 1
      0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119
      (byte) 0x89, 0x0d, // tag = 209, type = 1
      0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119

      (byte) 0x91, 0x0d, // tag = 210, type = 1
      0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120
      (byte) 0x91, 0x0d, // tag = 210, type = 1
      0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120

      (byte) 0x98, 0x0d, // tag = 211, type = 0
      0x01, // value = 1 (true)
      (byte) 0x98, 0x0d, // tag = 211, type = 0
      0x01, // value = 1 (true)

      (byte) 0xa5, 0x0d, // tag = 212, type = 5
      0x00, 0x00, (byte) 0xf4, 0x42, // value = 122.0F
      (byte) 0xa5, 0x0d, // tag = 212, type = 5
      0x00, 0x00, (byte) 0xf4, 0x42, // value = 122.0F

      (byte)  0xa9, 0x0d, // tag = 213, type = 1
      0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x5e, 0x40, // value = 123.0
      (byte) 0xa9, 0x0d, // tag = 213, type = 1
      0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x5e, 0x40, // value = 123.0

      (byte) 0xb2, 0x0d, // tag = 214, type = 2
      0x03, // length = 3
      0x31, 0x32, 0x34, // value = "124"
      (byte) 0xb2, 0x0d, // tag = 214, type = 2
      0x03, // length = 3
      0x31, 0x32, 0x34, // value = "124"

      (byte) 0xba, 0x0d, // tag = 215, type = 2
      0x02, // length = 2
      0x7d, (byte) 0xe1, // value = { 125, 225 }
      (byte) 0xba, 0x0d, // tag = 215, type = 2
      0x02, // length = 2
      0x7d, (byte) 0xe1, // value = { 125, 225 }

      (byte) 0xc0, 0x0d, // tag = 216, type = 0
      0x01, // value = 1
      (byte) 0xc0, 0x0d, // tag = 216, type = 0
      0x01, // value = 1

      (byte) 0xca, 0x0d, // tag = 217, type = 2
      0x03, // length = 3
      0x08, // nested tag = 1, type = 0
      (byte) 0xe7, 0x07, // value = 999
      (byte) 0xca, 0x0d, // tag = 217, type = 2
      0x03, // length = 3
      0x08, // nested tag = 1, type = 0
      (byte) 0xe7, 0x07, // value = 999

      // packed

      (byte) 0xe8, 0x12, // tag = 301, type = 0
      0x6f, // value = 111
      (byte) 0xe8, 0x12, // tag = 301, type = 0
      0x6f, // value = 111

      (byte) 0xf0, 0x12, // tag = 302, type = 0
      0x70, // value = 112
      (byte) 0xf0, 0x12, // tag = 302, type = 0
      0x70, // value = 112

      (byte) 0xf8, 0x12, // tag = 303, type = 0
      (byte) 0xe2, 0x01, // value = 226 (=113 zig-zag)
      (byte) 0xf8, 0x12, // tag = 303, type = -
      (byte) 0xe2, 0x01, // value = 226 (=113 zig-zag)

      (byte) 0x80, 0x13, // tag = 304, type = 0
      0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)
      (byte) 0x80, 0x13, // tag = 304, type = 0
      0x72, 0x00, 0x00, 0x00, // value = 114 (fixed32)

      (byte) 0x88, 0x13, // tag = 305, type = 0
      0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)
      (byte) 0x88, 0x13, // tag = 305, type = 0
      0x73, 0x00, 0x00, 0x00, // value = 115 (sfixed32)

      (byte) 0x90, 0x13, // tag = 306, type = 0
      0x74, // value = 116
      (byte) 0x90, 0x13, // tag = 306, type = 0
      0x74, // value = 116

      (byte) 0x98, 0x13, // tag = 307, type = 0
      0x75, // value = 117
      (byte) 0x98, 0x13, // tag = 307, type = 0
      0x75, // value = 117

      (byte) 0xa0, 0x13, // tag = 308, type = 0
      (byte) 0xec, 0x01, // value = 236 (=118 zigzag)
      (byte) 0xa0, 0x13, // tag = 308, type = 0
      (byte) 0xec, 0x01, // value = 236 (=118 zigzag)

      (byte) 0xa8, 0x13, // tag = 309, type = 0
      0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119
      (byte) 0xa8, 0x13, // tag = 309, type = 0
      0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 119

      (byte) 0xb0, 0x13, // tag = 310, type = 0
      0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120
      (byte) 0xb0, 0x13, // tag = 310, type = 0
      0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // value = 120

      (byte) 0xb8, 0x13, // tag = 311, type = 0
      0x01, // value = 1 (true)
      (byte) 0xb8, 0x13, // tag = 311, type = 0
      0x01, // value = 1 (true)

      (byte) 0xc0, 0x13, // tag = 312, type = 0
      0x00, 0x00, (byte) 0xf4, 0x42, // value = 122.0F
      (byte) 0xc0, 0x13, // tag = 312, type = 0
      0x00, 0x00, (byte) 0xf4, 0x42, // value = 122.0F

      (byte) 0xc8, 0x13, // tag = 313, type = 0
      0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x5e, 0x40, // value = 123.0
      (byte) 0xc8, 0x13, // tag = 313, type = 0
      0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x5e, 0x40, // value = 123.0

      (byte) 0xe0, 0x13, // tag = 316, type = 0
      0x01, // value = 1
      (byte) 0xe0, 0x13, // tag = 316, type = 0
      0x01, // value = 1

      // extension

      (byte) 0x98, 0x3f, // tag = 1011, type = 0
      0x01, // value = 1 (true)

      (byte) 0xb8, 0x45, // tag = 1111, type = 0
      0x01, // value = 1 (true)
      (byte) 0xb8, 0x45, // tag = 1111, type = 0
      0x01, // value = 1 (true)

      (byte) 0xd8, 0x4b, // tag = 1211, type = 0
      0x01, // value = 1 (true)
      (byte) 0xd8, 0x4b, // tag = 1211, type = 0
      0x01, // value = 1 (true)
  };
}
