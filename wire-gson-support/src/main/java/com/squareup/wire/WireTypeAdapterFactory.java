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
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import okio.ByteString;

/**
 * A {@link TypeAdapterFactory} that allows Wire messages to be serialized and deserialized
 * using the GSON Json library. To create a {@link Gson} instance that works with Wire,
 * use the {@link com.google.gson.GsonBuilder} interface:
 *
 * <pre>
 * Wire wire = new Wire(...extension classes...)
 * WireTypeAdapterFactory wireTypeAdapterFactory = new WireTypeAdapterFactory(wire)
 *     .useFieldNumbers(true);
 * Gson gson = new GsonBuilder()
 *     .registerTypeAdapterFactory(wireTypeAdapterFactory)
 *     .create();
 * </pre>
 *
 * The resulting {@link Gson} instance will be able to serialize and deserialize any Wire
 * {@link Message} type, including extensions and unknown field values. The JSON encoding is
 * intended to be compatible with the
 * <a href="https://code.google.com/p/protobuf-java-format/">protobuf-java-format</a>
 * library. Note that version 1.2 of that API has a
 * <a href="https://code.google.com/p/protobuf-java-format/issues/detail?id=47">bug</a>
 * in the way it serializes unknown fields, so we use our own approach for this case.
 */
public final class WireTypeAdapterFactory implements TypeAdapterFactory {

  private final Wire wire;
  private final boolean useFieldNumbers;

  private WireTypeAdapterFactory(Wire wire, boolean useFieldNumbers) {
    this.wire = wire;
    this.useFieldNumbers = useFieldNumbers;
  }

  /**
   * Constructs an adapter that is capable of serializing and deserializing extension values
   * that may have {@code Message} and {@code Enum} values belonging to the given whitelist. A
   * shortened version of the class name will be used as a type marker in the JSON serialized
   * form. It is not necessary to include non-extension classes in the whitelist.
   */
  public WireTypeAdapterFactory(Wire wire) {
    this(wire, false);
  }

  /**
   * If {@code useFieldNumbers} is true, the JSON output will use (quoted) field numbers
   * rather than field names. This allows for backwards-compatibility of the JSON schema
   * (analogous to the backwards-compatibility of standard binary protocol buffer serialization)
   * if field names evolve over time, as long as field numbers remain fixed.
   *
   * @return a reference to a new {@link com.squareup.wire.WireTypeAdapterFactory} instance.
   */
  public WireTypeAdapterFactory useFieldNumbers(boolean useFieldNumbers) {
    return new WireTypeAdapterFactory(wire, useFieldNumbers);
  }

  @SuppressWarnings("unchecked")
  @Override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    if (type.getRawType().equals(ByteString.class)) {
      return (TypeAdapter<T>) new ByteStringTypeAdapter();
    }
    if (Message.class.isAssignableFrom(type.getRawType())) {
      return (TypeAdapter<T>) new MessageTypeAdapter(wire, gson, type, useFieldNumbers);
    }
    return null;
  }
}
