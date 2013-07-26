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

class TestUtils {

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

  // Return the length of the varint starting at the given offset
  private int vlen(byte[] data, int off) {
    int len = 1;
    while (off < data.length && data[off++] < 0) {
      len++;
    }
    return len;
  }
}
