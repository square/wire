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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
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

    TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
    B builder = messageAdapter.newBuilder();

    in.beginObject();
    while (in.peek() != JsonToken.END_OBJECT) {
      String name = in.nextName();

      FieldBinding<M, B> fieldBinding = fieldBindings.get(name);
      if (fieldBinding == null) {
        in.skipValue();
      } else {
        JsonElement element = elementAdapter.read(in);
        Object value = parseValue(fieldBinding, element);
        fieldBinding.set(builder, value);
      }
    }

    in.endObject();
    return builder.build();
  }

  private Object parseValue(FieldBinding<?, ?> fieldBinding, JsonElement element) {
    if (fieldBinding.label.isRepeated()) {
      if (element.isJsonNull()) {
        return Collections.emptyList();
      }

      Class<?> itemType = fieldBinding.singleAdapter().javaType;

      JsonArray array = element.getAsJsonArray();
      List<Object> result = new ArrayList<>(array.size());
      for (JsonElement item : array) {
        result.add(gson.fromJson(item, itemType));
      }
      return result;
    }

    if (fieldBinding.isMap()) {
      if (element.isJsonNull()) {
        return Collections.emptyMap();
      }

      Class<?> keyType = fieldBinding.keyAdapter().javaType;
      Class<?> valueType = fieldBinding.singleAdapter().javaType;

      JsonObject object = element.getAsJsonObject();
      Map<Object, Object> result = new LinkedHashMap<>(object.size());
      for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
        Object key = gson.fromJson(entry.getKey(), keyType);
        Object value = gson.fromJson(entry.getValue(), valueType);
        result.put(key, value);
      }
      return result;
    }

    Class<?> elementType = fieldBinding.singleAdapter().javaType;
    return gson.fromJson(element, elementType);
  }
}
