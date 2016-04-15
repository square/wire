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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.squareup.wire.WireField.Label;
import static java.util.Collections.unmodifiableMap;

class MessageTypeAdapter<M extends Message<M, B>, B extends Message.Builder<M, B>>
    extends TypeAdapter<M> {

  // 2^64, used to convert sint64 values >= 2^63 to unsigned decimal form
  private static final BigInteger POWER_64 = new BigInteger("18446744073709551616");

  private final Gson gson;
  private final RuntimeMessageAdapter<M, B> messageAdapter;
  private final Map<String, FieldBinding<M, B>> fieldBindings;

  @SuppressWarnings("unchecked")
  public MessageTypeAdapter(Gson gson, TypeToken<M> type) {
    this.gson = gson;
    this.messageAdapter = RuntimeMessageAdapter.create((Class<M>) type.getRawType());

    Map<String, FieldBinding<M, B>> fieldBindings = new LinkedHashMap<>();
    for (FieldBinding<M, B> binding : messageAdapter.fieldBindings().values()) {
      fieldBindings.put(binding.name, binding);
    }
    this.fieldBindings = unmodifiableMap(fieldBindings);
  }

  @SuppressWarnings("unchecked")
  @Override public void write(JsonWriter out, M message) throws IOException {
    if (message == null) {
      out.nullValue();
      return;
    }

    out.beginObject();
    for (FieldBinding<M, B> tagBinding : messageAdapter.fieldBindings().values()) {
      Object value = tagBinding.get(message);
      if (value == null) {
        continue;
      }
      out.name(tagBinding.name);
      emitJson(out, value, tagBinding.singleAdapter(), tagBinding.label);
    }
    out.endObject();
  }

  @SuppressWarnings("unchecked")
  private void emitJson(JsonWriter out, Object value, ProtoAdapter<?> adapter, Label label)
      throws IOException {
    if (adapter == ProtoAdapter.UINT64) {
      if (label.isRepeated()) {
        List<Long> longs = (List<Long>) value;
        out.beginArray();
        for (int i = 0, count = longs.size(); i < count; i++) {
          emitUint64(longs.get(i), out);
        }
        out.endArray();
      } else {
        emitUint64((Long) value, out);
      }
    } else {
      gson.toJson(value, value.getClass(), out);
    }
  }

  private void emitUint64(Long value, JsonWriter out) throws IOException {
    if (value < 0) {
      BigInteger unsigned = POWER_64.add(BigInteger.valueOf(value));
      out.value(unsigned);
    } else {
      out.value(value);
    }
  }

  @SuppressWarnings("unchecked")
  @Override public M read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }

    B builder = messageAdapter.newBuilder();
    in.beginObject();

    while (in.peek() == JsonToken.NAME) {
      String name = in.nextName();

      FieldBinding<M, B> fieldBinding = fieldBindings.get(name);
      if (fieldBinding != null) {
        Object value = parseValue(fieldBinding.label, singleType(fieldBinding), parse(in));
        fieldBinding.set(builder, value);
        continue;
      }

      in.skipValue();
    }

    in.endObject();
    return builder.build();
  }

  private JsonElement parse(JsonReader in) {
    return gson.fromJson(in, JsonElement.class);
  }

  private Object parseValue(Label label, Type valueType, JsonElement valueElement) {
    if (label.isRepeated()) {
      if (valueElement.isJsonNull()) {
        return Collections.emptyList();
      }

      List<Object> valueList = new ArrayList<>();
      for (JsonElement element : valueElement.getAsJsonArray()) {
        valueList.add(readJson(valueType, element));
      }
      return valueList;
    } else {
      return readJson(valueType, valueElement);
    }
  }

  private Object readJson(Type valueType, JsonElement element) {
    return gson.fromJson(element, valueType);
  }

  private Type singleType(FieldBinding<M, B> tagBinding) {
    return tagBinding.singleAdapter().javaType;
  }
}
