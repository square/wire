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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

import static com.squareup.wire.Preconditions.checkNotNull;
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

  final FieldEncoding fieldEncoding;
  final Class<?> javaType;

  public TypeAdapter(FieldEncoding fieldEncoding, Class<?> javaType) {
    this.fieldEncoding = fieldEncoding;
    this.javaType = javaType;
  }

  static TypeAdapter<?> get(Wire wire, Message.Datatype datatype,
      Class<? extends Message> messageType, Class<? extends ProtoEnum> enumType) {
    switch (datatype) {
      case BOOL: return BOOL;
      case BYTES: return TypeAdapter.BYTES;
      case DOUBLE: return DOUBLE;
      case ENUM: return wire.enumAdapter(enumType);
      case FIXED32: return TypeAdapter.FIXED32;
      case FIXED64: return TypeAdapter.FIXED64;
      case FLOAT: return FLOAT;
      case INT32: return INT32;
      case INT64: return INT64;
      case MESSAGE: return wire.adapter(messageType);
      case SFIXED32: return SFIXED32;
      case SFIXED64: return SFIXED64;
      case SINT32: return SINT32;
      case SINT64: return SINT64;
      case STRING: return STRING;
      case UINT32: return UINT32;
      case UINT64: return TypeAdapter.UINT64;
      default: throw new AssertionError("Unknown data type " + datatype);
    }
  }

  /** Returns the redacted form of {@code value}. */
  public E redact(E value) {
    return null;
  }

  /**
   * The size of the non-null data {@code value}. This does not include the size required for
   * a length-delimited prefix (should the type require one).
   */
  public abstract int encodedSize(E value);

  /**
   * The size of {@code tag} and non-null {@code value} in the wire format. This size includes the
   * tag, type, length-delimited prefix (should the type require one), and value.
   */
  public int serializedSize(int tag, E value) {
    int size = encodedSize(value);
    if (fieldEncoding == FieldEncoding.LENGTH_DELIMITED) {
      size += varint32Size(size);
    }
    return size + ProtoWriter.tagSize(tag);
  }

  /** Write non-null {@code value} to {@code writer}. */
  public abstract void write(ProtoWriter writer, E value) throws IOException;

  /** Write {@code tag} and non-null {@code value} to {@code writer}. */
  public void writeTagged(ProtoWriter writer, int tag, E value) throws IOException {
    writer.writeTag(tag, fieldEncoding);
    if (fieldEncoding == FieldEncoding.LENGTH_DELIMITED) {
      writer.writeVarint32(encodedSize(value));
    }
    write(writer, value);
  }

  /** Encode {@code value} and write it to {@code stream}. */
  public final void write(BufferedSink sink, E value) throws IOException {
    checkNotNull(value, "value == null");
    checkNotNull(sink, "sink == null");
    write(new ProtoWriter(sink), value);
  }

  /** Encode {@code value} as a {@code byte[]}. */
  public final byte[] writeBytes(E value) {
    checkNotNull(value, "value == null");
    Buffer buffer = new Buffer();
    try {
      write(buffer, value);
    } catch (IOException e) {
      throw new AssertionError(e); // No I/O writing to Buffer.
    }
    return buffer.readByteArray();
  }

  /** Encode {@code value} and write it to {@code stream}. */
  public final void writeStream(OutputStream stream, E value) throws IOException {
    checkNotNull(value, "value == null");
    checkNotNull(stream, "stream == null");
    BufferedSink buffer = Okio.buffer(Okio.sink(stream));
    write(buffer, value);
    buffer.emit();
  }

  /** Read a non-null value from {@code reader}. */
  public abstract E read(ProtoReader reader) throws IOException;

  /** Read an encoded message from {@code bytes}. */
  public final E readBytes(byte[] bytes) throws IOException {
    checkNotNull(bytes, "bytes == null");
    return read(new Buffer().write(bytes));
  }

  /** Read an encoded message from {@code stream}. */
  public final E readStream(InputStream stream) throws IOException {
    checkNotNull(stream, "stream == null");
    return read(Okio.buffer(Okio.source(stream)));
  }

  /** Read an encoded message from {@code source}. */
  public final E read(BufferedSource source) throws IOException {
    checkNotNull(source, "source == null");
    return read(new ProtoReader(source));
  }

  /** Returns a human-readable version of the given {@code value}. */
  public String toString(E value) {
    return value.toString();
  }

  public static final TypeAdapter<Boolean> BOOL = new TypeAdapter<Boolean>(
      FieldEncoding.VARINT, Boolean.class) {
    @Override public int encodedSize(Boolean value) {
      return FIXED_BOOL_SIZE;
    }

    @Override public void write(ProtoWriter writer, Boolean value) throws IOException {
      writer.writeVarint32(value ? 1 : 0);
    }

    @Override public Boolean read(ProtoReader reader) throws IOException {
      int value = reader.readVarint32();
      if (value == 0) return Boolean.FALSE;
      if (value == 1) return Boolean.TRUE;
      throw new IOException(String.format("Invalid boolean value 0x%02x", value));
    }
  };
  public static final TypeAdapter<Integer> INT32 = new TypeAdapter<Integer>(
      FieldEncoding.VARINT, Integer.class) {
    @Override public int encodedSize(Integer value) {
      return int32Size(value);
    }

    @Override public void write(ProtoWriter writer, Integer value) throws IOException {
      writer.writeSignedVarint32(value);
    }

    @Override public Integer read(ProtoReader reader) throws IOException {
      return reader.readVarint32();
    }
  };
  public static final TypeAdapter<Integer> UINT32 = new TypeAdapter<Integer>(
      FieldEncoding.VARINT, Integer.class) {
    @Override public int encodedSize(Integer value) {
      return varint32Size(value);
    }

    @Override public void write(ProtoWriter writer, Integer value) throws IOException {
      writer.writeVarint32(value);
    }

    @Override public Integer read(ProtoReader reader) throws IOException {
      return reader.readVarint32();
    }
  };
  public static final TypeAdapter<Integer> SINT32 = new TypeAdapter<Integer>(
      FieldEncoding.VARINT, Integer.class) {
    @Override public int encodedSize(Integer value) {
      return varint32Size(encodeZigZag32(value));
    }

    @Override public void write(ProtoWriter writer, Integer value) throws IOException {
      writer.writeVarint32(encodeZigZag32(value));
    }

    @Override public Integer read(ProtoReader reader) throws IOException {
      return decodeZigZag32(reader.readVarint32());
    }
  };
  public static final TypeAdapter<Integer> FIXED32 = new TypeAdapter<Integer>(
      FieldEncoding.FIXED32, Integer.class) {
    @Override public int encodedSize(Integer value) {
      return FIXED_32_SIZE;
    }

    @Override public void write(ProtoWriter writer, Integer value) throws IOException {
      writer.writeFixed32(value);
    }

    @Override public Integer read(ProtoReader reader) throws IOException {
      return reader.readFixed32();
    }
  };
  public static final TypeAdapter<Integer> SFIXED32 = FIXED32;
  public static final TypeAdapter<Long> INT64 = new TypeAdapter<Long>(
      FieldEncoding.VARINT, Long.class) {
    @Override public int encodedSize(Long value) {
      return varint64Size(value);
    }

    @Override public void write(ProtoWriter writer, Long value) throws IOException {
      writer.writeVarint64(value);
    }

    @Override public Long read(ProtoReader reader) throws IOException {
      return reader.readVarint64();
    }
  };
  public static final TypeAdapter<Long> UINT64 = INT64;
  public static final TypeAdapter<Long> SINT64 = new TypeAdapter<Long>(
      FieldEncoding.VARINT, Long.class) {
    @Override public int encodedSize(Long value) {
      return varint64Size(encodeZigZag64(value));
    }

    @Override public void write(ProtoWriter writer, Long value) throws IOException {
      writer.writeVarint64(encodeZigZag64(value));
    }

    @Override public Long read(ProtoReader reader) throws IOException {
      return decodeZigZag64(reader.readVarint64());
    }
  };
  public static final TypeAdapter<Long> FIXED64 = new TypeAdapter<Long>(
      FieldEncoding.FIXED64, Long.class) {
    @Override public int encodedSize(Long value) {
      return FIXED_64_SIZE;
    }

    @Override public void write(ProtoWriter writer, Long value) throws IOException {
      writer.writeFixed64(value);
    }

    @Override public Long read(ProtoReader reader) throws IOException {
      return reader.readFixed64();
    }
  };
  public static final TypeAdapter<Long> SFIXED64 = FIXED64;
  public static final TypeAdapter<Float> FLOAT = new TypeAdapter<Float>(
      FieldEncoding.FIXED32, Float.class) {
    @Override public int encodedSize(Float value) {
      return FIXED_32_SIZE;
    }

    @Override public void write(ProtoWriter writer, Float value) throws IOException {
      writer.writeFixed32(floatToIntBits(value));
    }

    @Override public Float read(ProtoReader reader) throws IOException {
      return Float.intBitsToFloat(reader.readFixed32());
    }
  };
  public static final TypeAdapter<Double> DOUBLE = new TypeAdapter<Double>(
      FieldEncoding.FIXED64, Double.class) {
    @Override public int encodedSize(Double value) {
      return FIXED_64_SIZE;
    }

    @Override public void write(ProtoWriter writer, Double value) throws IOException {
      writer.writeFixed64(doubleToLongBits(value));
    }

    @Override public Double read(ProtoReader reader) throws IOException {
      return Double.longBitsToDouble(reader.readFixed64());
    }
  };
  public static final TypeAdapter<String> STRING = new TypeAdapter<String>(
      FieldEncoding.LENGTH_DELIMITED, String.class) {
    @Override public int encodedSize(String value) {
      return utf8Length(value);
    }

    @Override public void write(ProtoWriter writer, String value) throws IOException {
      writer.writeString(value);
    }

    @Override public String read(ProtoReader reader) throws IOException {
      return reader.readString();
    }
  };
  public static final TypeAdapter<ByteString> BYTES = new TypeAdapter<ByteString>(
      FieldEncoding.LENGTH_DELIMITED, ByteString.class) {
    @Override public int encodedSize(ByteString value) {
      return value.size();
    }

    @Override public void write(ProtoWriter writer, ByteString value) throws IOException {
      writer.writeBytes(value);
    }

    @Override public ByteString read(ProtoReader reader) throws IOException {
      return reader.readBytes();
    }
  };

  TypeAdapter<?> withLabel(Message.Label label) {
    if (label.isRepeated()) {
      return label.isPacked()
          ? createPacked(this)
          : createRepeated(this);
    }
    return this;
  }

  private static <T> TypeAdapter<List<T>> createPacked(final TypeAdapter<T> adapter) {
    if (adapter.fieldEncoding == FieldEncoding.LENGTH_DELIMITED) {
      throw new IllegalArgumentException("Unable to pack a length-delimited type.");
    }
    return new TypeAdapter<List<T>>(FieldEncoding.LENGTH_DELIMITED, List.class) {
      @Override public int encodedSize(List<T> value) {
        int size = 0;
        for (int i = 0, count = value.size(); i < count; i++) {
          size += adapter.encodedSize(value.get(i));
        }
        return size;
      }

      @Override public void write(ProtoWriter writer, List<T> value) throws IOException {
        for (int i = 0, count = value.size(); i < count; i++) {
          adapter.write(writer, value.get(i));
        }
      }

      @Override public List<T> read(ProtoReader reader) throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override public List<T> redact(List<T> value) {
        return Collections.emptyList();
      }
    };
  }

  private static <T> TypeAdapter<List<T>> createRepeated(final TypeAdapter<T> adapter) {
    return new TypeAdapter<List<T>>(adapter.fieldEncoding, List.class) {
      @Override public int encodedSize(List<T> value) {
        throw new UnsupportedOperationException();
      }

      @Override public int serializedSize(int tag, List<T> value) {
        int size = 0;
        for (int i = 0, count = value.size(); i < count; i++) {
          size += adapter.serializedSize(tag, value.get(i));
        }
        return size;
      }

      @Override public void write(ProtoWriter writer, List<T> value) throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override public void writeTagged(ProtoWriter writer, int tag, List<T> value)
          throws IOException {
        for (int i = 0, count = value.size(); i < count; i++) {
          adapter.writeTagged(writer, tag, value.get(i));
        }
      }

      @Override public List<T> read(ProtoReader reader) throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override public List<T> redact(List<T> value) {
        return Collections.emptyList();
      }
    };
  }
}
