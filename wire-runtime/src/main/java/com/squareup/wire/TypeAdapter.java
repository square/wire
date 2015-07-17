/*
 * Copyright 2015 Square Inc.
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
import okio.ByteString;

import static java.lang.Double.doubleToLongBits;
import static java.lang.Float.floatToIntBits;

public abstract class TypeAdapter<E> {
  static final int TYPE_VARINT = 0;
  static final int TYPE_FIXED64 = 1;
  static final int TYPE_LEN_DELIMITED = 2;
  static final int TYPE_START_GROUP = 3;
  static final int TYPE_END_GROUP = 4;
  static final int TYPE_FIXED32 = 5;

  static final int FIXED_BOOL_SIZE = 1;
  static final int FIXED_32_SIZE = 4;
  static final int FIXED_64_SIZE = 8;

  static int utf8Length(String s) {
    int count = 0;
    for (int i = 0, length = s.length(); i < length; i++) {
      char ch = s.charAt(i);
      if (ch <= 0x7F) {
        count++;
      } else if (ch <= 0x7FF) {
        count += 2;
      } else if (Character.isHighSurrogate(ch)) {
        count += 4;
        ++i;
      } else {
        count += 3;
      }
    }
    return count;
  }

  /**
   * Encode a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that can be
   * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
   * to be varint encoded, thus always taking 10 bytes on the wire.)
   *
   * @param n A signed 32-bit integer.
   * @return An unsigned 32-bit integer, stored in a signed int because Java has no explicit
   * unsigned support.
   */
  static int encodeZigZag32(int n) {
    // Note:  the right-shift must be arithmetic
    return (n << 1) ^ (n >> 31);
  }

  /**
   * Decodes a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that can be
   * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
   * to be varint encoded, thus always taking 10 bytes on the wire.)
   *
   * @param n An unsigned 32-bit integer, stored in a signed int because Java has no explicit
   * unsigned support.
   * @return A signed 32-bit integer.
   */
  static int decodeZigZag32(int n) {
    return (n >>> 1) ^ -(n & 1);
  }

  /**
   * Encode a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be
   * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
   * to be varint encoded, thus always taking 10 bytes on the wire.)
   *
   * @param n A signed 64-bit integer.
   * @return An unsigned 64-bit integer, stored in a signed int because Java has no explicit
   * unsigned support.
   */
  static long encodeZigZag64(long n) {
    // Note:  the right-shift must be arithmetic
    return (n << 1) ^ (n >> 63);
  }

  /**
   * Decodes a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be
   * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
   * to be varint encoded, thus always taking 10 bytes on the wire.)
   *
   * @param n An unsigned 64-bit integer, stored in a signed int because Java has no explicit
   * unsigned support.
   * @return A signed 64-bit integer.
   */
  static long decodeZigZag64(long n) {
    return (n >>> 1) ^ -(n & 1);
  }

  /** Compute the number of bytes that would be needed to encode a varint. */
  static int varint64Size(long value) {
    if ((value & (0xffffffffffffffffL <<  7)) == 0) return 1;
    if ((value & (0xffffffffffffffffL << 14)) == 0) return 2;
    if ((value & (0xffffffffffffffffL << 21)) == 0) return 3;
    if ((value & (0xffffffffffffffffL << 28)) == 0) return 4;
    if ((value & (0xffffffffffffffffL << 35)) == 0) return 5;
    if ((value & (0xffffffffffffffffL << 42)) == 0) return 6;
    if ((value & (0xffffffffffffffffL << 49)) == 0) return 7;
    if ((value & (0xffffffffffffffffL << 56)) == 0) return 8;
    if ((value & (0xffffffffffffffffL << 63)) == 0) return 9;
    return 10;
  }

  /**
   * Computes the number of bytes that would be needed to encode a signed variable-length integer
   * of up to 32 bits.
   */
  static int int32Size(int value) {
    if (value >= 0) {
      return varint32Size(value);
    } else {
      // Must sign-extend.
      return 10;
    }
  }

  /**
   * Compute the number of bytes that would be needed to encode a varint. {@code value} is treated
   * as unsigned, so it won't be sign-extended if negative.
   */
  static int varint32Size(int value) {
    if ((value & (0xffffffff <<  7)) == 0) return 1;
    if ((value & (0xffffffff << 14)) == 0) return 2;
    if ((value & (0xffffffff << 21)) == 0) return 3;
    if ((value & (0xffffffff << 28)) == 0) return 4;
    return 5;
  }

  public static final TypeAdapter<Boolean> BOOL = new TypeAdapter<Boolean>(TYPE_VARINT) {
    @Override public int serializedSize(Boolean value) {
      return FIXED_BOOL_SIZE;
    }

    @Override public void write(Boolean value, ProtoWriter writer) throws IOException {
      writer.writeRawByte(value ? 1 : 0);
    }

    @Override public Boolean read(ProtoReader reader) throws IOException {
      int value = reader.readRawByte();
      if (value == 0) return Boolean.FALSE;
      if (value == 1) return Boolean.TRUE;
      throw new IllegalStateException(String.format("Invalid boolean value 0x%02x", value));
    }
  };
  public static final TypeAdapter<Integer> INT32 = new TypeAdapter<Integer>(TYPE_VARINT) {
    @Override public int serializedSize(Integer value) {
      return int32Size(value);
    }

    @Override public void write(Integer value, ProtoWriter writer) throws IOException {
      writer.writeSignedVarint32(value);
    }

    @Override public Integer read(ProtoReader reader) throws IOException {
      return reader.readVarint32();
    }
  };
  public static final TypeAdapter<Integer> UINT32 = new TypeAdapter<Integer>(TYPE_VARINT) {
    @Override public int serializedSize(Integer value) {
      return varint32Size(value);
    }

    @Override public void write(Integer value, ProtoWriter writer) throws IOException {
      writer.writeVarint32(value);
    }

    @Override public Integer read(ProtoReader reader) throws IOException {
      return reader.readVarint32();
    }
  };
  public static final TypeAdapter<Integer> SINT32 = new TypeAdapter<Integer>(TYPE_VARINT) {
    @Override public int serializedSize(Integer value) {
      return varint32Size(encodeZigZag32(value));
    }

    @Override public void write(Integer value, ProtoWriter writer) throws IOException {
      writer.writeVarint32(encodeZigZag32(value));
    }

    @Override public Integer read(ProtoReader reader) throws IOException {
      return decodeZigZag32(reader.readVarint32());
    }
  };
  public static final TypeAdapter<Integer> FIXED32 = new TypeAdapter<Integer>(TYPE_FIXED32) {
    @Override public int serializedSize(Integer value) {
      return FIXED_32_SIZE;
    }

    @Override public void write(Integer value, ProtoWriter writer) throws IOException {
      writer.writeFixed32(value);
    }

    @Override public Integer read(ProtoReader reader) throws IOException {
      return reader.readFixed32();
    }
  };
  public static final TypeAdapter<Integer> SFIXED32 = FIXED32;
  public static final TypeAdapter<Long> INT64 = new TypeAdapter<Long>(TYPE_VARINT) {
    @Override public int serializedSize(Long value) {
      return varint64Size(value);
    }

    @Override public void write(Long value, ProtoWriter writer) throws IOException {
      writer.writeVarint64(value);
    }

    @Override public Long read(ProtoReader reader) throws IOException {
      return reader.readVarint64();
    }
  };
  public static final TypeAdapter<Long> UINT64 = INT64;
  public static final TypeAdapter<Long> SINT64 = new TypeAdapter<Long>(TYPE_VARINT) {
    @Override public int serializedSize(Long value) {
      return varint64Size(encodeZigZag64(value));
    }

    @Override public void write(Long value, ProtoWriter writer) throws IOException {
      writer.writeVarint64(encodeZigZag64(value));
    }

    @Override public Long read(ProtoReader reader) throws IOException {
      return decodeZigZag64(reader.readVarint64());
    }
  };
  public static final TypeAdapter<Long> FIXED64 = new TypeAdapter<Long>(TYPE_FIXED64) {
    @Override public int serializedSize(Long value) {
      return FIXED_64_SIZE;
    }

    @Override public void write(Long value, ProtoWriter writer) throws IOException {
      writer.writeFixed64(value);
    }

    @Override public Long read(ProtoReader reader) throws IOException {
      return reader.readFixed64();
    }
  };
  public static final TypeAdapter<Long> SFIXED64 = FIXED64;
  public static final TypeAdapter<Float> FLOAT = new TypeAdapter<Float>(TYPE_FIXED32) {
    @Override public int serializedSize(Float value) {
      return FIXED_32_SIZE;
    }

    @Override public void write(Float value, ProtoWriter writer) throws IOException {
      writer.writeFixed32(floatToIntBits(value));
    }

    @Override public Float read(ProtoReader reader) throws IOException {
      return Float.intBitsToFloat(reader.readFixed32());
    }
  };
  public static final TypeAdapter<Double> DOUBLE = new TypeAdapter<Double>(TYPE_FIXED64) {
    @Override public int serializedSize(Double value) {
      return FIXED_64_SIZE;
    }

    @Override public void write(Double value, ProtoWriter writer) throws IOException {
      writer.writeFixed64(doubleToLongBits(value));
    }

    @Override public Double read(ProtoReader reader) throws IOException {
      return Double.longBitsToDouble(reader.readFixed64());
    }
  };
  public static final TypeAdapter<String> STRING = new TypeAdapter<String>(TYPE_LEN_DELIMITED) {
    @Override public int serializedSize(String value) {
      return utf8Length(value);
    }

    @Override public void write(String value, ProtoWriter writer) throws IOException {
      ByteString bytes = ByteString.encodeUtf8(value);
      writer.writeRawBytes(bytes);
    }

    @Override public String read(ProtoReader reader) throws IOException {
      return reader.readString();
    }
  };
  public static final TypeAdapter<ByteString> BYTES = new TypeAdapter<ByteString>(TYPE_LEN_DELIMITED) {
    @Override public int serializedSize(ByteString value) {
      return value.size();
    }

    @Override public void write(ByteString value, ProtoWriter writer) throws IOException {
      writer.writeRawBytes(value);
    }

    @Override public ByteString read(ProtoReader reader) throws IOException {
      return reader.readBytes();
    }
  };

  final int type;

  TypeAdapter(int type) {
    this.type = type;
  }

  final int makeTag(int tag) {
    return ProtoWriter.makeTag(tag, type);
  }

  /** The serialized size of non-null {@code value}. */
  public abstract int serializedSize(E value);

  /** Write non-null {@code value} to {@code writer}. */
  public abstract void write(E value, ProtoWriter writer) throws IOException;

  /** Read a non-null value from {@code reader}. */
  public abstract E read(ProtoReader reader) throws IOException;

  public static abstract class MessageAdapter<M extends Message> extends TypeAdapter<M> {
    protected MessageAdapter() {
      super(TYPE_LEN_DELIMITED);
    }

    @Override public final int serializedSize(M value) {
      return value.serializedSize();
    }

    @Override public final void write(M value, ProtoWriter writer) throws IOException {
      value.write(writer);
    }
  }

  public static abstract class EnumAdapter<M extends Enum<M> & WireEnum> extends TypeAdapter<M> {
    protected EnumAdapter() {
      super(TYPE_VARINT);
    }

    @Override public final int serializedSize(M value) {
      return TypeAdapter.UINT32.serializedSize(value.value());
    }

    @Override public final void write(M value, ProtoWriter writer) throws IOException {
      TypeAdapter.UINT32.write(value.value(), writer);
    }

    @Override
    public M read(ProtoReader reader) throws IOException {
      int value = TypeAdapter.UINT32.read(reader);
      M constant = fromValue(value);
      if (constant == null) {
        throw new IllegalStateException("No constant for value " + value);
      }
      return constant;
    }

    public abstract M fromValue(int value);
  }
}
