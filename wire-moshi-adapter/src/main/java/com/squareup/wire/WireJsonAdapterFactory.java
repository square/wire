/*
 * Copyright 2018 Square Inc.
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

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import okio.ByteString;

/**
 * A {@link JsonAdapter.Factory} that allows Wire messages to be serialized and deserialized using
 * the Moshi Json library.
 *
 * <pre>
 * Moshi moshi = new Moshi.Builder()
 *   .add(new WireJsonAdapterFactory())
 *   .build();
 * </pre>
 *
 * The resulting {@link Moshi} instance will be able to serialize and deserialize Wire {@link
 * Message} types, including extensions. It ignores unknown field values.
 * The JSON encoding is intended to be compatible with the
 * <a href="https://code.google.com/p/protobuf-java-format/">protobuf-java-format</a>
 * library.
 */
public final class WireJsonAdapterFactory implements JsonAdapter.Factory {
  static final JsonAdapter<ByteString> BYTE_STRING_JSON_ADAPTER = new JsonAdapter<ByteString>() {
    @Override public void toJson(JsonWriter out, ByteString byteString) throws IOException {
      out.value(byteString.base64());
    }

    @Override public ByteString fromJson(JsonReader in) throws IOException {
      return ByteString.decodeBase64(in.nextString());
    }
  }.nullSafe();

  /**
   * Wire uses the signed long type to store unsigned longs. Sigh. But when we encode as JSON we
   * need to emit an unsigned value.
   */
  static final JsonAdapter<Long> UINT64_JSON_ADAPTER = new JsonAdapter<Long>() {
    // 2^64, used to convert sint64 values >= 2^63 to unsigned decimal form
    private final BigInteger power64 = new BigInteger("18446744073709551616");
    private final BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);

    @Override public Long fromJson(JsonReader reader) throws IOException {
      BigInteger bigInteger = new BigInteger(reader.nextString());
      return bigInteger.compareTo(maxLong) > 0
          ? bigInteger.subtract(power64).longValue()
          : bigInteger.longValue();
    }

    @Override public void toJson(JsonWriter writer, Long value) throws IOException {
      if (value < 0) {
        BigInteger unsigned = power64.add(BigInteger.valueOf(value));
        writer.value(unsigned);
      } else {
        writer.value(value);
      }
    }
  }.nullSafe();

  /**
   * Tragically Moshi doesn't know enough to follow a {@code @Uint64 List<Long>} really wants to be
   * treated as a {@code List<@Uint64 Long>} and so we have to do it manually.
   *
   * TODO delete when Moshi can handle that; see
   * <a href="https://github.com/square/moshi/issues/666">moshi/issues/666</a>
   */
  static final JsonAdapter<List<Long>> LIST_OF_UINT64_JSON_ADAPTER = new JsonAdapter<List<Long>>() {
    @Override public List<Long> fromJson(JsonReader reader) throws IOException {
      List<Long> result = new ArrayList<>();
      reader.beginArray();
      while (reader.hasNext()) {
        result.add(UINT64_JSON_ADAPTER.fromJson(reader));
      }
      reader.endArray();
      return result;
    }

    @Override public void toJson(JsonWriter writer, List<Long> value) throws IOException {
      writer.beginArray();
      for (Long v : value) {
        UINT64_JSON_ADAPTER.toJson(writer, v);
      }
      writer.endArray();
    }
  }.nullSafe();

  @Override
  public @Nullable JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations,
      Moshi moshi) {
    if ((type == Long.class || type == long.class)
        && Types.nextAnnotations(annotations, Uint64.class) != null) {
      return UINT64_JSON_ADAPTER;
    }

    Class<?> rawType = Types.getRawType(type);

    if (rawType == List.class
        && ((ParameterizedType) type).getActualTypeArguments()[0] == Long.class
        && Types.nextAnnotations(annotations, Uint64.class) != null) {
      return LIST_OF_UINT64_JSON_ADAPTER;
    }

    if (!annotations.isEmpty()) {
      return null;
    }

    if (rawType.equals(ByteString.class)) {
      return BYTE_STRING_JSON_ADAPTER;
    }
    if (Message.class.isAssignableFrom(rawType)) {
      return new MessageJsonAdapter(moshi, type);
    }
    return null;
  }
}
