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
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

class MessageJsonAdapter<M extends Message<M, B>, B extends Message.Builder<M, B>>
    extends JsonAdapter<M> {
  private final RuntimeMessageAdapter<M, B> messageAdapter;
  private final FieldBinding<M, B>[] fieldBindings;
  private final JsonReader.Options options;
  private final JsonAdapter<?>[] jsonAdapters;

  @SuppressWarnings("unchecked") MessageJsonAdapter(Moshi moshi, Type type) {
    this.messageAdapter = RuntimeMessageAdapter.create((Class<M>) type);

    this.fieldBindings = messageAdapter.fieldBindings()
        .values()
        .toArray(new FieldBinding[messageAdapter.fieldBindings().size()]);

    String[] names = new String[fieldBindings.length];
    for (int i = 0; i < fieldBindings.length; i++) {
      names[i] = fieldBindings[i].name;
    }
    this.options = JsonReader.Options.of(names);

    jsonAdapters = new JsonAdapter[fieldBindings.length];
    for (int i = 0; i < fieldBindings.length; i++) {
      FieldBinding<M, B> fieldBinding = fieldBindings[i];

      Type fieldType = fieldBinding.singleAdapter().javaType;
      if (fieldBinding.isMap()) {
        Class<?> keyType = fieldBinding.keyAdapter().javaType;
        fieldType = Types.newParameterizedType(Map.class, keyType, fieldType);
      } else if (fieldBinding.label.isRepeated()) {
        fieldType = Types.newParameterizedType(List.class, fieldType);
      }

      Class<? extends Annotation>[] qualifiers = new Class[0];
      if (fieldBinding.singleAdapter() == ProtoAdapter.UINT64) {
        qualifiers = new Class[] {Uint64.class};
      }

      jsonAdapters[i] = moshi.adapter(fieldType, qualifiers);
    }
  }

  @Override public void toJson(JsonWriter out, @Nullable M message) throws IOException {
    if (message == null) {
      out.nullValue();
      return;
    }

    out.beginObject();
    for (int i = 0; i < fieldBindings.length; i++) {
      FieldBinding<M, B> fieldBinding = fieldBindings[i];
      out.name(fieldBinding.name);
      Object value = fieldBinding.get(message);
      ((JsonAdapter) jsonAdapters[i]).toJson(out, value);
    }
    out.endObject();
  }

  @Override public @Nullable M fromJson(JsonReader in) throws IOException {
    if (in.peek() == JsonReader.Token.NULL) {
      in.nextNull();
      return null;
    }

    B builder = messageAdapter.newBuilder();

    in.beginObject();
    while (in.hasNext()) {
      int index = in.selectName(options);
      if (index == -1) {
        in.skipName();
        in.skipValue();
        continue;
      }

      FieldBinding<M, B> fieldBinding = fieldBindings[index];
      if (fieldBinding == null) {
        in.skipValue();
        continue;
      }

      Object value = jsonAdapters[index].fromJson(in);

      // If the value was explicitly null we ignore it rather than forcing null into the field.
      // Otherwise malformed JSON that sets a list to null will create a malformed message, and
      // we'd rather just ignore that problem.
      if (value == null) continue;

      fieldBinding.set(builder, value);
    }

    in.endObject();
    return builder.build();
  }
}
