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

import com.squareup.wire.Message.Builder;
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

public abstract class ProtoAdapter<E> {
  private static final int FIXED_BOOL_SIZE = 1;
  private static final int FIXED_32_SIZE = 4;
  private static final int FIXED_64_SIZE = 8;

  private final FieldEncoding fieldEncoding;
  final Class<?> javaType;

  ProtoAdapter<List<E>> packedAdapter;
  ProtoAdapter<List<E>> repeatedAdapter;

  public ProtoAdapter(FieldEncoding fieldEncoding, Class<?> javaType) {
    this.fieldEncoding = fieldEncoding;
    this.javaType = javaType;
  }

  /** Creates a new proto adapter for {@code type}. */
  public static <M extends Message<M, B>, B extends Builder<M, B>> ProtoAdapter<M>
      newMessageAdapter(Class<M> type) {
    return RuntimeMessageAdapter.create(type);
  }

  /** Creates a new proto adapter for {@code type}. */
  public static <E extends WireEnum> RuntimeEnumAdapter<E> newEnumAdapter(Class<E> type) {
    return new RuntimeEnumAdapter<>(type);
  }

  /** Returns the adapter for the type of {@code Message}. */
  public static <M extends Message> ProtoAdapter<M> get(M message) {
    return (ProtoAdapter<M>) get(message.getClass());
  }

  /** Returns the adapter for {@code type}. */
  public static <M> ProtoAdapter<M> get(Class<M> type) {
    try {
      return (ProtoAdapter<M>) type.getField("ADAPTER").get(null);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new IllegalArgumentException("failed to access " + type.getName() + "#ADAPTER", e);
    }
  }

  @SuppressWarnings("unchecked")
  static ProtoAdapter<?> get(String adapterString) {
    try {
      int hash = adapterString.indexOf('#');
      String className = adapterString.substring(0, hash);
      String fieldName = adapterString.substring(hash + 1);
      return (ProtoAdapter<Object>) Class.forName(className).getField(fieldName).get(null);
    } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException e) {
      throw new IllegalArgumentException("failed to access " + adapterString, e);
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
  public int encodedSizeWithTag(int tag, E value) {
    int size = encodedSize(value);
    if (fieldEncoding == FieldEncoding.LENGTH_DELIMITED) {
      size += varint32Size(size);
    }
    return size + ProtoWriter.tagSize(tag);
  }

  /** Write non-null {@code value} to {@code writer}. */
  public abstract void encode(ProtoWriter writer, E value) throws IOException;

  /** Write {@code tag} and non-null {@code value} to {@code writer}. */
  public void encodeWithTag(ProtoWriter writer, int tag, E value) throws IOException {
    writer.writeTag(tag, fieldEncoding);
    if (fieldEncoding == FieldEncoding.LENGTH_DELIMITED) {
      writer.writeVarint32(encodedSize(value));
    }
    encode(writer, value);
  }

  /** Encode {@code value} and write it to {@code stream}. */
  public final void encode(BufferedSink sink, E value) throws IOException {
    checkNotNull(value, "value == null");
    checkNotNull(sink, "sink == null");
    encode(new ProtoWriter(sink), value);
  }

  /** Encode {@code value} as a {@code byte[]}. */
  public final byte[] encode(E value) {
    checkNotNull(value, "value == null");
    Buffer buffer = new Buffer();
    try {
      encode(buffer, value);
    } catch (IOException e) {
      throw new AssertionError(e); // No I/O writing to Buffer.
    }
    return buffer.readByteArray();
  }

  /** Encode {@code value} and write it to {@code stream}. */
  public final void encode(OutputStream stream, E value) throws IOException {
    checkNotNull(value, "value == null");
    checkNotNull(stream, "stream == null");
    BufferedSink buffer = Okio.buffer(Okio.sink(stream));
    encode(buffer, value);
    buffer.emit();
  }

  /** Read a non-null value from {@code reader}. */
  public abstract E decode(ProtoReader reader) throws IOException;

  /** Read an encoded message from {@code bytes}. */
  public final E decode(byte[] bytes) throws IOException {
    checkNotNull(bytes, "bytes == null");
    return decode(new Buffer().write(bytes));
  }

  /** Read an encoded message from {@code bytes}. */
  public final E decode(ByteString bytes) throws IOException {
    checkNotNull(bytes, "bytes == null");
    return decode(new Buffer().write(bytes));
  }

  /** Read an encoded message from {@code stream}. */
  public final E decode(InputStream stream) throws IOException {
    checkNotNull(stream, "stream == null");
    return decode(Okio.buffer(Okio.source(stream)));
  }

  /** Read an encoded message from {@code source}. */
  public final E decode(BufferedSource source) throws IOException {
    checkNotNull(source, "source == null");
    return decode(new ProtoReader(source));
  }

  /** Returns a human-readable version of the given {@code value}. */
  public String toString(E value) {
    return value.toString();
  }

  public static final ProtoAdapter<Boolean> BOOL = new ProtoAdapter<Boolean>(
      FieldEncoding.VARINT, Boolean.class) {
    @Override public int encodedSize(Boolean value) {
      return FIXED_BOOL_SIZE;
    }

    @Override public void encode(ProtoWriter writer, Boolean value) throws IOException {
      writer.writeVarint32(value ? 1 : 0);
    }

    @Override public Boolean decode(ProtoReader reader) throws IOException {
      int value = reader.readVarint32();
      if (value == 0) return Boolean.FALSE;
      if (value == 1) return Boolean.TRUE;
      throw new IOException(String.format("Invalid boolean value 0x%02x", value));
    }
  };
  public static final ProtoAdapter<Integer> INT32 = new ProtoAdapter<Integer>(
      FieldEncoding.VARINT, Integer.class) {
    @Override public int encodedSize(Integer value) {
      return int32Size(value);
    }

    @Override public void encode(ProtoWriter writer, Integer value) throws IOException {
      writer.writeSignedVarint32(value);
    }

    @Override public Integer decode(ProtoReader reader) throws IOException {
      return reader.readVarint32();
    }
  };
  public static final ProtoAdapter<Integer> UINT32 = new ProtoAdapter<Integer>(
      FieldEncoding.VARINT, Integer.class) {
    @Override public int encodedSize(Integer value) {
      return varint32Size(value);
    }

    @Override public void encode(ProtoWriter writer, Integer value) throws IOException {
      writer.writeVarint32(value);
    }

    @Override public Integer decode(ProtoReader reader) throws IOException {
      return reader.readVarint32();
    }
  };
  public static final ProtoAdapter<Integer> SINT32 = new ProtoAdapter<Integer>(
      FieldEncoding.VARINT, Integer.class) {
    @Override public int encodedSize(Integer value) {
      return varint32Size(encodeZigZag32(value));
    }

    @Override public void encode(ProtoWriter writer, Integer value) throws IOException {
      writer.writeVarint32(encodeZigZag32(value));
    }

    @Override public Integer decode(ProtoReader reader) throws IOException {
      return decodeZigZag32(reader.readVarint32());
    }
  };
  public static final ProtoAdapter<Integer> FIXED32 = new ProtoAdapter<Integer>(
      FieldEncoding.FIXED32, Integer.class) {
    @Override public int encodedSize(Integer value) {
      return FIXED_32_SIZE;
    }

    @Override public void encode(ProtoWriter writer, Integer value) throws IOException {
      writer.writeFixed32(value);
    }

    @Override public Integer decode(ProtoReader reader) throws IOException {
      return reader.readFixed32();
    }
  };
  public static final ProtoAdapter<Integer> SFIXED32 = FIXED32;
  public static final ProtoAdapter<Long> INT64 = new ProtoAdapter<Long>(
      FieldEncoding.VARINT, Long.class) {
    @Override public int encodedSize(Long value) {
      return varint64Size(value);
    }

    @Override public void encode(ProtoWriter writer, Long value) throws IOException {
      writer.writeVarint64(value);
    }

    @Override public Long decode(ProtoReader reader) throws IOException {
      return reader.readVarint64();
    }
  };
  /**
   * Like INT64, but negative longs are interpreted as large positive values, and encoded that way
   * in JSON.
   */
  public static final ProtoAdapter<Long> UINT64 = new ProtoAdapter<Long>(
      FieldEncoding.VARINT, Long.class) {
    @Override public int encodedSize(Long value) {
      return varint64Size(value);
    }

    @Override public void encode(ProtoWriter writer, Long value) throws IOException {
      writer.writeVarint64(value);
    }

    @Override public Long decode(ProtoReader reader) throws IOException {
      return reader.readVarint64();
    }
  };
  public static final ProtoAdapter<Long> SINT64 = new ProtoAdapter<Long>(
      FieldEncoding.VARINT, Long.class) {
    @Override public int encodedSize(Long value) {
      return varint64Size(encodeZigZag64(value));
    }

    @Override public void encode(ProtoWriter writer, Long value) throws IOException {
      writer.writeVarint64(encodeZigZag64(value));
    }

    @Override public Long decode(ProtoReader reader) throws IOException {
      return decodeZigZag64(reader.readVarint64());
    }
  };
  public static final ProtoAdapter<Long> FIXED64 = new ProtoAdapter<Long>(
      FieldEncoding.FIXED64, Long.class) {
    @Override public int encodedSize(Long value) {
      return FIXED_64_SIZE;
    }

    @Override public void encode(ProtoWriter writer, Long value) throws IOException {
      writer.writeFixed64(value);
    }

    @Override public Long decode(ProtoReader reader) throws IOException {
      return reader.readFixed64();
    }
  };
  public static final ProtoAdapter<Long> SFIXED64 = FIXED64;
  public static final ProtoAdapter<Float> FLOAT = new ProtoAdapter<Float>(
      FieldEncoding.FIXED32, Float.class) {
    @Override public int encodedSize(Float value) {
      return FIXED_32_SIZE;
    }

    @Override public void encode(ProtoWriter writer, Float value) throws IOException {
      writer.writeFixed32(floatToIntBits(value));
    }

    @Override public Float decode(ProtoReader reader) throws IOException {
      return Float.intBitsToFloat(reader.readFixed32());
    }
  };
  public static final ProtoAdapter<Double> DOUBLE = new ProtoAdapter<Double>(
      FieldEncoding.FIXED64, Double.class) {
    @Override public int encodedSize(Double value) {
      return FIXED_64_SIZE;
    }

    @Override public void encode(ProtoWriter writer, Double value) throws IOException {
      writer.writeFixed64(doubleToLongBits(value));
    }

    @Override public Double decode(ProtoReader reader) throws IOException {
      return Double.longBitsToDouble(reader.readFixed64());
    }
  };
  public static final ProtoAdapter<String> STRING = new ProtoAdapter<String>(
      FieldEncoding.LENGTH_DELIMITED, String.class) {
    @Override public int encodedSize(String value) {
      return utf8Length(value);
    }

    @Override public void encode(ProtoWriter writer, String value) throws IOException {
      writer.writeString(value);
    }

    @Override public String decode(ProtoReader reader) throws IOException {
      return reader.readString();
    }
  };
  public static final ProtoAdapter<ByteString> BYTES = new ProtoAdapter<ByteString>(
      FieldEncoding.LENGTH_DELIMITED, ByteString.class) {
    @Override public int encodedSize(ByteString value) {
      return value.size();
    }

    @Override public void encode(ProtoWriter writer, ByteString value) throws IOException {
      writer.writeBytes(value);
    }

    @Override public ByteString decode(ProtoReader reader) throws IOException {
      return reader.readBytes();
    }
  };

  ProtoAdapter<?> withLabel(WireField.Label label) {
    if (label.isRepeated()) {
      return label.isPacked()
          ? asPacked()
          : asRepeated();
    }
    return this;
  }

  /** Returns an adapter for {@code E} but as a packed, repeated value. */
  public final ProtoAdapter<List<E>> asPacked() {
    ProtoAdapter<List<E>> adapter = packedAdapter;
    return adapter != null ? adapter : (packedAdapter = createPacked());
  }

  /** Returns an adapter for {@code E} but as a repeated value. */
  public final ProtoAdapter<List<E>> asRepeated() {
    ProtoAdapter<List<E>> adapter = repeatedAdapter;
    return adapter != null ? adapter : (repeatedAdapter = createRepeated());
  }

  private ProtoAdapter<List<E>> createPacked() {
    if (fieldEncoding == FieldEncoding.LENGTH_DELIMITED) {
      throw new IllegalArgumentException("Unable to pack a length-delimited type.");
    }
    return new ProtoAdapter<List<E>>(FieldEncoding.LENGTH_DELIMITED, List.class) {
      @Override public void encodeWithTag(ProtoWriter writer, int tag, List<E> value)
          throws IOException {
        if (!value.isEmpty()) {
          super.encodeWithTag(writer, tag, value);
        }
      }

      @Override public int encodedSize(List<E> value) {
        int size = 0;
        for (int i = 0, count = value.size(); i < count; i++) {
          size += ProtoAdapter.this.encodedSize(value.get(i));
        }
        return size;
      }

      @Override public int encodedSizeWithTag(int tag, List<E> value) {
        return value.isEmpty() ? 0 : super.encodedSizeWithTag(tag, value);
      }

      @Override public void encode(ProtoWriter writer, List<E> value) throws IOException {
        for (int i = 0, count = value.size(); i < count; i++) {
          ProtoAdapter.this.encode(writer, value.get(i));
        }
      }

      @Override public List<E> decode(ProtoReader reader) throws IOException {
        E value = ProtoAdapter.this.decode(reader);
        return Collections.singletonList(value);
      }

      @Override public List<E> redact(List<E> value) {
        return Collections.emptyList();
      }
    };
  }

  private ProtoAdapter<List<E>> createRepeated() {
    return new ProtoAdapter<List<E>>(fieldEncoding, List.class) {
      @Override public int encodedSize(List<E> value) {
        throw new UnsupportedOperationException("Repeated values can only be sized with a tag.");
      }

      @Override public int encodedSizeWithTag(int tag, List<E> value) {
        int size = 0;
        for (int i = 0, count = value.size(); i < count; i++) {
          size += ProtoAdapter.this.encodedSizeWithTag(tag, value.get(i));
        }
        return size;
      }

      @Override public void encode(ProtoWriter writer, List<E> value) {
        throw new UnsupportedOperationException("Repeated values can only be encoded with a tag.");
      }

      @Override public void encodeWithTag(ProtoWriter writer, int tag, List<E> value)
          throws IOException {
        for (int i = 0, count = value.size(); i < count; i++) {
          ProtoAdapter.this.encodeWithTag(writer, tag, value.get(i));
        }
      }

      @Override public List<E> decode(ProtoReader reader) throws IOException {
        E value = ProtoAdapter.this.decode(reader);
        return Collections.singletonList(value);
      }

      @Override public List<E> redact(List<E> value) {
        return Collections.emptyList();
      }
    };
  }

  public static final class EnumConstantNotFoundException extends IllegalArgumentException {
    public final int value;

    EnumConstantNotFoundException(int value, Class<?> type) {
      super("Unknown enum tag " + value + " for " + type.getCanonicalName());
      this.value = value;
    }
  }
}
