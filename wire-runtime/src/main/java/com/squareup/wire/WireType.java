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

public enum WireType {
  VARINT(0), FIXED64(1), LENGTH_DELIMITED(2), START_GROUP(3), END_GROUP(4), FIXED32(5);

  private static final int TAG_TYPE_MASK = 0x7;

  public static final int FIXED_32_SIZE = 4;
  public static final int FIXED_64_SIZE = 8;
  public static final int TAG_TYPE_BITS = 3;

  public static WireType valueOf(int tagAndType) throws IOException {
    switch (tagAndType & TAG_TYPE_MASK) {
      case 0: return VARINT;
      case 1: return FIXED64;
      case 2: return LENGTH_DELIMITED;
      case 3: return START_GROUP;
      case 4: return END_GROUP;
      case 5: return FIXED32;
      default:
        throw new IOException("No WireType for type " + (tagAndType & TAG_TYPE_MASK));
    }
  }

  private final int value;

  private WireType(int value) {
    this.value = value;
  }

  public int value() {
    return value;
  }
}
