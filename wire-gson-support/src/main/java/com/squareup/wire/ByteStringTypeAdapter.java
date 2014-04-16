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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import okio.ByteString;

/**
 * A {@link com.google.gson.TypeAdapter} that may be used to serialize and deserialize
 * {@link ByteString} values using the GSON Json library. The byte data is serialized
 * in Base64 format.
 */
class ByteStringTypeAdapter extends TypeAdapter<ByteString> {

  @Override public void write(JsonWriter out, ByteString value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value.base64());
    }
  }

  @Override public ByteString read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    return ByteString.decodeBase64(in.nextString());
  }
}
