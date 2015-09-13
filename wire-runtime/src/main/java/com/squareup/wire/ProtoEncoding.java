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

import java.io.IOException;
import java.net.ProtocolException;
import okio.ByteString;

/** The byte format of an encoded proto object (message or field value). */
public enum ProtoEncoding {
  VARINT(0), FIXED64(1), LENGTH_DELIMITED(2), FIXED32(5);

  final int value;

  ProtoEncoding(int value) {
    this.value = value;
  }

  static ProtoEncoding get(int value) throws IOException {
    switch (value) {
      case 0: return VARINT;
      case 1: return FIXED64;
      case 2: return LENGTH_DELIMITED;
      case 5: return FIXED32;
      default:
        throw new ProtocolException("Unexpected FieldEncoding: " + value);
    }
  }

  ProtoType protoType() {
    switch (this) {
      case VARINT:
        return ProtoType.UINT64;
      case FIXED32:
        return ProtoType.FIXED32;
      case FIXED64:
        return ProtoType.FIXED64;
      case LENGTH_DELIMITED:
        return ProtoType.BYTES;
      default:
        throw new AssertionError();
    }
  }

  Class<?> javaType() {
    switch (this) {
      case VARINT:
        return Long.class;
      case FIXED32:
        return Integer.class;
      case FIXED64:
        return Long.class;
      case LENGTH_DELIMITED:
        return ByteString.class;
      default:
        throw new AssertionError();
    }
  }

  /**
   * Returns a Wire adapter that reads this field encoding without interpretation. For example,
   * messages are returned as byte strings and enums are returned as integers.
   */
  public ProtoAdapter<?> rawProtoAdapter() {
    switch (this) {
      case VARINT:
        return ProtoAdapter.UINT64;
      case FIXED32:
        return ProtoAdapter.FIXED32;
      case FIXED64:
        return ProtoAdapter.FIXED64;
      case LENGTH_DELIMITED:
        return ProtoAdapter.BYTES;
      default:
        throw new AssertionError();
    }
  }
}
