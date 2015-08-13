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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import okio.ByteString;

import static com.squareup.wire.ProtoWriter.decodeZigZag32;
import static com.squareup.wire.ProtoWriter.decodeZigZag64;
import static com.squareup.wire.ProtoWriter.encodeZigZag32;
import static com.squareup.wire.ProtoWriter.encodeZigZag64;
import static com.squareup.wire.ProtoWriter.int32Size;
import static com.squareup.wire.ProtoWriter.utf8Length;
import static com.squareup.wire.ProtoWriter.varint32Size;
import static com.squareup.wire.ProtoWriter.varint64Size;
import static java.lang.Double.doubleToLongBits;
import static java.lang.Float.floatToIntBits;

public abstract class TypeAdapter<E> {
  static final int FIXED_BOOL_SIZE = 1;
  static final int FIXED_32_SIZE = 4;
  static final int FIXED_64_SIZE = 8;

  final WireType type;

  public TypeAdapter(WireType type) {
    this.type = type;
  }

  /** Returns the redacted form of {@code value}. */
  E redact(E value) {
    return null;
  }

  /**
   * The size of the non-null data {@code value}. This does not include the size required for
   * a length-delimited prefix (should the type require one).
   */
  public abstract int dataSize(E value);

  /**
   * The size of {@code tag} and non-null {@code value} in the wire format. This size includes the
   * tag, type, length-delimited prefix (should the type require one), and value.
   */
  int serializedSize(int tag, E value) {
    int size = dataSize(value);
    if (type == WireType.LENGTH_DELIMITED) {
      size += varint32Size(size);
    }
    return size + ProtoWriter.tagSize(tag);
  }

  /** Write non-null {@code value} to {@code writer}. */
  public abstract void writeData(E value, ProtoWriter writer) throws IOException;

  /** Write {@code tag} and non-null {@code value} to {@code writer}. */
  void write(int tag, E value, ProtoWriter writer) throws IOException {
    writer.writeTag(tag, type);
    if (type == WireType.LENGTH_DELIMITED) {
      writer.writeVarint32(dataSize(value));
    }
    writeData(value, writer);
  }

  /** Read a non-null value from {@code reader}. */
  public abstract E read(ProtoReader reader) throws IOException;

  public static final TypeAdapter<Boolean> BOOL = new TypeAdapter<Boolean>(WireType.VARINT) {
    @Override public int dataSize(Boolean value) {
      return FIXED_BOOL_SIZE;
    }

    @Override public void writeData(Boolean value, ProtoWriter writer) throws IOException {
      writer.writeByte(value ? 1 : 0);
    }

    @Override public Boolean read(ProtoReader reader) throws IOException {
      int value = reader.readByte();
      if (value == 0) return Boolean.FALSE;
      if (value == 1) return Boolean.TRUE;
      throw new IOException(String.format("Invalid boolean value 0x%02x", value));
    }
  };
  public static final TypeAdapter<Integer> INT32 = new TypeAdapter<Integer>(WireType.VARINT) {
    @Override public int dataSize(Integer value) {
      return int32Size(value);
    }

    @Override public void writeData(Integer value, ProtoWriter writer) throws IOException {
      writer.writeSignedVarint32(value);
    }

    @Override public Integer read(ProtoReader reader) throws IOException {
      return reader.readVarint32();
    }
  };
  public static final TypeAdapter<Integer> UINT32 = new TypeAdapter<Integer>(WireType.VARINT) {
    @Override public int dataSize(Integer value) {
      return varint32Size(value);
    }

    @Override public void writeData(Integer value, ProtoWriter writer) throws IOException {
      writer.writeVarint32(value);
    }

    @Override public Integer read(ProtoReader reader) throws IOException {
      return reader.readVarint32();
    }
  };
  public static final TypeAdapter<Integer> SINT32 = new TypeAdapter<Integer>(WireType.VARINT) {
    @Override public int dataSize(Integer value) {
      return varint32Size(encodeZigZag32(value));
    }

    @Override public void writeData(Integer value, ProtoWriter writer) throws IOException {
      writer.writeVarint32(encodeZigZag32(value));
    }

    @Override public Integer read(ProtoReader reader) throws IOException {
      return decodeZigZag32(reader.readVarint32());
    }
  };
  public static final TypeAdapter<Integer> FIXED32 = new TypeAdapter<Integer>(WireType.FIXED32) {
    @Override public int dataSize(Integer value) {
      return FIXED_32_SIZE;
    }

    @Override public void writeData(Integer value, ProtoWriter writer) throws IOException {
      writer.writeFixed32(value);
    }

    @Override public Integer read(ProtoReader reader) throws IOException {
      return reader.readFixed32();
    }
  };
  public static final TypeAdapter<Integer> SFIXED32 = FIXED32;
  public static final TypeAdapter<Long> INT64 = new TypeAdapter<Long>(WireType.VARINT) {
    @Override public int dataSize(Long value) {
      return varint64Size(value);
    }

    @Override public void writeData(Long value, ProtoWriter writer) throws IOException {
      writer.writeVarint64(value);
    }

    @Override public Long read(ProtoReader reader) throws IOException {
      return reader.readVarint64();
    }
  };
  public static final TypeAdapter<Long> UINT64 = INT64;
  public static final TypeAdapter<Long> SINT64 = new TypeAdapter<Long>(WireType.VARINT) {
    @Override public int dataSize(Long value) {
      return varint64Size(encodeZigZag64(value));
    }

    @Override public void writeData(Long value, ProtoWriter writer) throws IOException {
      writer.writeVarint64(encodeZigZag64(value));
    }

    @Override public Long read(ProtoReader reader) throws IOException {
      return decodeZigZag64(reader.readVarint64());
    }
  };
  public static final TypeAdapter<Long> FIXED64 = new TypeAdapter<Long>(WireType.FIXED64) {
    @Override public int dataSize(Long value) {
      return FIXED_64_SIZE;
    }

    @Override public void writeData(Long value, ProtoWriter writer) throws IOException {
      writer.writeFixed64(value);
    }

    @Override public Long read(ProtoReader reader) throws IOException {
      return reader.readFixed64();
    }
  };
  public static final TypeAdapter<Long> SFIXED64 = FIXED64;
  public static final TypeAdapter<Float> FLOAT = new TypeAdapter<Float>(WireType.FIXED32) {
    @Override public int dataSize(Float value) {
      return FIXED_32_SIZE;
    }

    @Override public void writeData(Float value, ProtoWriter writer) throws IOException {
      writer.writeFixed32(floatToIntBits(value));
    }

    @Override public Float read(ProtoReader reader) throws IOException {
      return Float.intBitsToFloat(reader.readFixed32());
    }
  };
  public static final TypeAdapter<Double> DOUBLE = new TypeAdapter<Double>(WireType.FIXED64) {
    @Override public int dataSize(Double value) {
      return FIXED_64_SIZE;
    }

    @Override public void writeData(Double value, ProtoWriter writer) throws IOException {
      writer.writeFixed64(doubleToLongBits(value));
    }

    @Override public Double read(ProtoReader reader) throws IOException {
      return Double.longBitsToDouble(reader.readFixed64());
    }
  };
  public static final TypeAdapter<String> STRING =
      new TypeAdapter<String>(WireType.LENGTH_DELIMITED) {
        @Override public int dataSize(String value) {
          return utf8Length(value);
        }

        @Override public void writeData(String value, ProtoWriter writer) throws IOException {
          ByteString bytes = ByteString.encodeUtf8(value);
          writer.writeBytes(bytes);
        }

        @Override public String read(ProtoReader reader) throws IOException {
          return reader.readString();
        }
      };
  public static final TypeAdapter<ByteString> BYTES =
      new TypeAdapter<ByteString>(WireType.LENGTH_DELIMITED) {
        @Override public int dataSize(ByteString value) {
          return value.size();
        }

        @Override public void writeData(ByteString value, ProtoWriter writer) throws IOException {
          writer.writeBytes(value);
        }

        @Override public ByteString read(ProtoReader reader) throws IOException {
          return reader.readBytes();
        }
      };

  public static <M> TypeAdapter<M> forMessage(final MessageAdapter<M> adapter) {
    return new TypeAdapter<M>(WireType.LENGTH_DELIMITED) {
      @Override public int dataSize(M value) {
        return adapter.serializedSize(value);
      }

      @Override public void writeData(M value, ProtoWriter writer) throws IOException {
        adapter.write(value, writer);
      }

      @Override public M read(ProtoReader reader) throws IOException {
        long token = reader.beginLengthDelimited();
        M value = adapter.read(reader);
        reader.endLengthDelimited(token);
        return value;
      }

      @Override M redact(M value) {
        return adapter.redact(value);
      }
    };
  }

  public static <T> TypeAdapter<List<T>> createPacked(final TypeAdapter<T> adapter) {
    if (adapter.type == WireType.LENGTH_DELIMITED) {
      throw new IllegalArgumentException("Unable to pack a length-delimited type.");
    }
    return new TypeAdapter<List<T>>(WireType.LENGTH_DELIMITED) {
      @Override public int dataSize(List<T> value) {
        int size = 0;
        for (int i = 0, count = value.size(); i < count; i++) {
          size += adapter.dataSize(value.get(i));
        }
        return size;
      }

      @Override public void writeData(List<T> value, ProtoWriter writer) throws IOException {
        for (int i = 0, count = value.size(); i < count; i++) {
          adapter.writeData(value.get(i), writer);
        }
      }

      @Override public List<T> read(ProtoReader reader) throws IOException {
        // Check to ensure the bytes are actually packed on the wire which is optional.
        if (reader.peekType() != WireType.LENGTH_DELIMITED) {
          // TODO delegate to repeated if we get peekTag() on ProtoReader
          return Collections.singletonList(adapter.read(reader));
        }

        List<T> items = new ArrayList<T>();
        long token = reader.beginLengthDelimited();
        while (reader.hasNext()) {
          items.add(adapter.read(reader));
        }
        reader.endLengthDelimited(token);
        return items;
      }

      @Override List<T> redact(List<T> value) {
        return Collections.emptyList();
      }
    };
  }

  public static <T> TypeAdapter<List<T>> createRepeated(final TypeAdapter<T> adapter) {
    return new TypeAdapter<List<T>>(adapter.type) {
      @Override public int dataSize(List<T> value) {
        throw new UnsupportedOperationException();
      }

      @Override int serializedSize(int tag, List<T> value) {
        int size = 0;
        for (int i = 0, count = value.size(); i < count; i++) {
          size += adapter.serializedSize(tag, value.get(i));
        }
        return size;
      }

      @Override public void writeData(List<T> value, ProtoWriter writer) throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override void write(int tag, List<T> value, ProtoWriter writer) throws IOException {
        for (int i = 0, count = value.size(); i < count; i++) {
          adapter.write(tag, value.get(i), writer);
        }
      }

      @Override public List<T> read(ProtoReader reader) throws IOException {
        throw new UnsupportedOperationException(); // TODO
      }

      @Override List<T> redact(List<T> value) {
        return Collections.emptyList();
      }
    };
  }
}
