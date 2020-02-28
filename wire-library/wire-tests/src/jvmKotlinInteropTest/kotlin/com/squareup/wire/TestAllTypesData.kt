/*
 * Copyright 2019 Square Inc.
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
package com.squareup.wire

import okio.ByteString.Companion.decodeHex

object TestAllTypesData {
    val expectedOutput = (""
      // optional

        + "08" // tag = 1, type = 0
        + "6f" // value = 111
        + "10" // tag = 2, type = 0
        + "70" // value = 112
        + "18" // tag = 3, type = 0
        + "e201" // value = 226 (=113 zig-zag)
        + "25" // tag = 4, type = 5
        + "72000000" // value = 114 (fixed32)
        + "2d" // tag = 5, type = 5
        + "73000000" // value = 115 (sfixed32)
        + "30" // tag = 6, type = 0
        + "74" // value = 116
        + "38" // tag = 7, type = 0
        + "75" // value = 117
        + "40" // tag = 8, type = 0
        + "ec01" // value = 236 (=118 zigzag)
        + "49" // tag = 9, type = 1
        + "7700000000000000" // value = 119
        + "51" // tag = 10, type = 1
        + "7800000000000000" // value = 120
        + "58" // tag = 11, type = 0
        + "01" // value = 1 (true)
        + "65" // tag = 12, type = 5
        + "0000f442" // value = 122.0F
        + "69" // tag = 13, type = 1
        + "0000000000c05e40" // value = 123.0
        + "72"  // tag = 14, type = 2
        + "03"  // length = 3
        + "313234"
        + "7a" // tag = 15, type = 2
        + "02" // length = 2
        + "7de1" // value = { 125, 225 }
        + "8001" // tag = 16, type = 0
        + "01" // value = 1
        + "8a01" // tag = 17, type = 2
        + "03" // length = 3
        + "08" // nested tag = 1, type = 0
        + "e707" // value = 999

      // required

        + "a806" // tag = 101, type = 0
        + "6f" // value = 111
        + "b006" // tag = 102, type = 0
        + "70" // value = 112
        + "b806" // tag = 103, type = 0
        + "e201" // value = 226 (=113 zig-zag)
        + "c506" // tag = 104, type = 5
        + "72000000" // value = 114 (fixed32)
        + "cd06" // tag = 105, type = 5
        + "73000000" // value = 115 (sfixed32)
        + "d006" // tag = 106, type = 0
        + "74" // value = 116
        + "d806" // tag = 107, type = 0
        + "75" // value = 117
        + "e006" // tag = 108, type = 0
        + "ec01" // value = 236 (=118 zigzag)
        + "e906" // tag = 109, type = 1
        + "7700000000000000" // value = 119
        + "f106" // tag = 110, type = 1
        + "7800000000000000" // value = 120
        + "f806" // tag = 111, type = 0
        + "01" // value = 1 (true)
        + "8507" // tag = 112, type = 5
        + "0000f442" // value = 122.0F
        + "8907" // tag = 113, type = 1
        + "0000000000c05e40" // value = 123.0
        + "9207" // tag = 114, type = 2
        + "03" // length = 3
        + "313234" // value = "124"
        + "9a07" // tag = 115, type = 2
        + "02" // length = 2
        + "7de1" // value = { 125, 225 }
        + "a007" // tag = 116, type = 0
        + "01" // value = 1
        + "aa07" // tag = 117, type = 2
        + "03" // length = 3
        + "08" // nested tag = 1, type = 0
        + "e707" // value = 999

      // repeated

        + "c80c"// tag = 201, type = 0
        + "6f" // value = 111
        + "c80c" // tag = 201, type = 0
        + "6f" // value = 111
        + "d00c" // tag = 202, type = 0
        + "70" // value = 112
        + "d00c" // tag = 202, type = 0
        + "70" // value = 112
        + "d80c" // tag = 203, type = 0
        + "e201" // value = 226 (=113 zig-zag)
        + "d80c" // tag = 203, type = 0
        + "e201" // value = 226 (=113 zig-zag)
        + "e50c" // tag = 204, type = 5
        + "72000000" // value = 114 (fixed32)
        + "e50c" // tag = 204, type = 5
        + "72000000" // value = 114 (fixed32)
        + "ed0c" // tag = 205, type = 5
        + "73000000" // value = 115 (sfixed32)
        + "ed0c" // tag = 205, type = 5
        + "73000000" // value = 115 (sfixed32)
        + "f00c" // tag = 206, type = 0
        + "74" // value = 116
        + "f00c" // tag = 206, type = 0
        + "74" // value = 116
        + "f80c" // tag = 207, type = 0
        + "75" // value = 117
        + "f80c" // tag = 207, type = 0
        + "75" // value = 117
        + "800d" // tag = 208, type = 0
        + "ec01" // value = 236 (=118 zigzag)
        + "800d" // tag = 208, type = 0
        + "ec01" // value = 236 (=118 zigzag)
        + "890d" // tag = 209, type = 1
        + "7700000000000000" // value = 119
        + "890d" // tag = 209, type = 1
        + "7700000000000000" // value = 119
        + "910d" // tag = 210, type = 1
        + "7800000000000000" // value = 120
        + "910d" // tag = 210, type = 1
        + "7800000000000000" // value = 120
        + "980d" // tag = 211, type = 0
        + "01" // value = 1 (true)
        + "980d" // tag = 211, type = 0
        + "01" // value = 1 (true)
        + "a50d" // tag = 212, type = 5
        + "0000f442" // value = 122.0F
        + "a50d" // tag = 212, type = 5
        + "0000f442" // value = 122.0F
        + "a90d" // tag = 213, type = 1
        + "0000000000c05e40" // value = 123.0
        + "a90d" // tag = 213, type = 1
        + "0000000000c05e40" // value = 123.0
        + "b20d" // tag = 214, type = 2
        + "03" // length = 3
        + "313234" // value = "124"
        + "b20d" // tag = 214, type = 2
        + "03" // length = 3
        + "313234" // value = "124"
        + "ba0d" // tag = 215, type = 2
        + "02" // length = 2
        + "7de1" // value = { 125, 225 }
        + "ba0d" // tag = 215, type = 2
        + "02" // length = 2
        + "7de1" // value = { 125, 225 }
        + "c00d" // tag = 216, type = 0
        + "01" // value = 1
        + "c00d" // tag = 216, type = 0
        + "01" // value = 1
        + "ca0d" // tag = 217, type = 2
        + "03" // length = 3
        + "08" // nested tag = 1, type = 0
        + "e707" // value = 999
        + "ca0d" // tag = 217, type = 2
        + "03" // length = 3
        + "08" // nested tag = 1, type = 0
        + "e707" // value = 999

      // packed

        + "ea12" // tag = 301, type = 2
        + "02" // length = 2
        + "6f" // value = 111
        + "6f" // value = 111
        + "f212" // tag = 302, type = 2
        + "02" // length = 2
        + "70" // value = 112
        + "70" // value = 112
        + "fa12" // tag = 303, type = 2
        + "04" // length = 4
        + "e201" // value = 226 (=113 zig-zag)
        + "e201" // value = 226 (=113 zig-zag)
        + "8213" // tag = 304, type = 2
        + "08" // length = 8
        + "72000000" // value = 114 (fixed32)
        + "72000000" // value = 114 (fixed32)
        + "8a13" // tag = 305, type = 2
        + "08" // length = 8
        + "73000000" // value = 115 (sfixed32)
        + "73000000" // value = 115 (sfixed32)
        + "9213" // tag = 306, type = 2
        + "02" // length = 2
        + "74" // value = 116
        + "74" // value = 116
        + "9a13" // tag = 307, type = 2
        + "02" // length = 2
        + "75" // value = 117
        + "75" // value = 117
        + "a213" // tag = 308, type = 2
        + "04" // length = 4
        + "ec01" // value = 236 (=118 zigzag)
        + "ec01" // value = 236 (=118 zigzag)
        + "aa13" // tag = 309, type = 2
        + "10" // length = 16
        + "7700000000000000" // value = 119
        + "7700000000000000" // value = 119
        + "b213" // tag = 310, type = 2
        + "10" // length = 16
        + "7800000000000000" // value = 120
        + "7800000000000000" // value = 120
        + "ba13" // tag = 311, type = 2
        + "02" // length = 2
        + "01" // value = 1 (true)
        + "01" // value = 1 (true)
        + "c213" // tag = 312, type = 2
        + "08" // length = 8
        + "0000f442" // value = 122.0F
        + "0000f442" // value = 122.0F
        + "ca13" // tag = 313, type = 2
        + "10" // length = 16
        + "0000000000c05e40" // value = 123.0
        + "0000000000c05e40" // value = 123.0
        + "e213" // tag = 316, type = 2
        + "02" // length = 2
        + "01" // value = 1
        + "01" // value = 1

      // extensions

        + "983f" // tag = 1011, type = 0
        + "01" // value = 1 (true)
        + "b845" // tag = 1111, type = 0
        + "01" // value = 1 (true)
        + "b845" // tag = 1111, type = 0
        + "01" // value = 1 (true)
        + "da4b" // tag = 1211, type = 2
        + "02" // length = 2
        + "01" // value = 1 (true)
        + "01" // value = 1 (true)
      ).decodeHex()  
}
