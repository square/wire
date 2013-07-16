// Copyright 2013 Square, Inc.
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
