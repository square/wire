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

import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.squareup.wire.Message.Builder;

final class RuntimeMessageAdapter<M extends Message<M, B>, B extends Builder<M, B>>
    extends ProtoAdapter<M> {
  private static final String REDACTED = "\u2588\u2588";

  static <M extends Message<M, B>, B extends Builder<M, B>> RuntimeMessageAdapter<M, B> create(
      Class<M> messageType) {
    Class<B> builderType = getBuilderType(messageType);
    Map<Integer, FieldBinding<M, B>> fieldBindings = new LinkedHashMap<>();

    // Create tag bindings for fields annotated with '@WireField'
    for (Field messageField : messageType.getDeclaredFields()) {
      WireField wireField = messageField.getAnnotation(WireField.class);
      if (wireField != null) {
        fieldBindings.put(wireField.tag(),
            new FieldBinding<>(wireField, messageField, builderType));
      }
    }

    return new RuntimeMessageAdapter<>(messageType, builderType,
        Collections.unmodifiableMap(fieldBindings));
  }

  private final Class<M> messageType;
  private final Class<B> builderType;
  private final Map<Integer, FieldBinding<M, B>> fieldBindings;

  RuntimeMessageAdapter(Class<M> messageType, Class<B> builderType,
      Map<Integer, FieldBinding<M, B>> fieldBindings) {
    super(FieldEncoding.LENGTH_DELIMITED, messageType);
    this.messageType = messageType;
    this.builderType = builderType;
    this.fieldBindings = fieldBindings;
  }

  Map<Integer, FieldBinding<M, B>> fieldBindings() {
    return fieldBindings;
  }

  B newBuilder() {
    try {
      return builderType.newInstance();
    } catch (IllegalAccessException | InstantiationException e) {
      throw new AssertionError(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <M extends Message<M, B>, B extends Builder<M, B>> Class<B> getBuilderType(
      Class<M> messageType) {
    try {
      return (Class<B>) Class.forName(messageType.getName() + "$Builder");
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("No builder class found for message type "
          + messageType.getName());
    }
  }

  @Override public int encodedSize(M message) {
    int cachedSerializedSize = message.cachedSerializedSize;
    if (cachedSerializedSize != 0) return cachedSerializedSize;

    int size = 0;
    for (FieldBinding<M, B> fieldBinding : fieldBindings.values()) {
      Object value = fieldBinding.get(message);
      if (value == null) continue;
      size += fieldBinding.adapter().encodedSizeWithTag(fieldBinding.tag, value);
    }
    size += message.unknownFields().size();

    message.cachedSerializedSize = size;
    return size;
  }

  @Override public void encode(ProtoWriter writer, M message) throws IOException {
    for (FieldBinding<M, B> fieldBinding : fieldBindings.values()) {
      Object value = fieldBinding.get(message);
      if (value == null) continue;
      fieldBinding.adapter().encodeWithTag(writer, fieldBinding.tag, value);
    }
    writer.writeBytes(message.unknownFields());
  }

  @Override public M redact(M message) {
    B builder = (B) message.newBuilder();
    for (FieldBinding<M, B> fieldBinding : fieldBindings.values()) {
      if (fieldBinding.redacted && fieldBinding.label == WireField.Label.REQUIRED) {
        throw new UnsupportedOperationException(String.format(
            "Field '%s' in %s is required and cannot be redacted.",
            fieldBinding.name, javaType.getName()));
      }
      boolean isMessage = Message.class.isAssignableFrom(fieldBinding.singleAdapter().javaType);
      if (fieldBinding.redacted || (isMessage && !fieldBinding.label.isRepeated())) {
        Object builderValue = fieldBinding.getFromBuilder(builder);
        if (builderValue != null) {
          Object redactedValue = fieldBinding.adapter().redact(builderValue);
          fieldBinding.set(builder, redactedValue);
        }
      } else if (isMessage && fieldBinding.label.isRepeated()) {
        //noinspection unchecked
        List<Object> values = (List<Object>) fieldBinding.getFromBuilder(builder);
        //noinspection unchecked
        ProtoAdapter<Object> adapter = (ProtoAdapter<Object>) fieldBinding.singleAdapter();
        Internal.redactElements(values, adapter);
      }
    }
    builder.clearUnknownFields();
    return builder.build();
  }

  @Override public boolean equals(Object o) {
    return o instanceof RuntimeMessageAdapter
        && ((RuntimeMessageAdapter) o).messageType == messageType;
  }

  @Override public int hashCode() {
    return messageType.hashCode();
  }

  @Override public String toString(M message) {
    StringBuilder sb = new StringBuilder();
    for (FieldBinding<M, B> fieldBinding : fieldBindings.values()) {
      Object value = fieldBinding.get(message);
      if (value != null) {
        sb.append(", ")
            .append(fieldBinding.name)
            .append('=')
            .append(fieldBinding.redacted ? REDACTED : value);
      }
    }

    // Replace leading comma with class name and opening brace.
    sb.replace(0, 2, messageType.getSimpleName() + '{');
    return sb.append('}').toString();
  }

  @Override public M decode(ProtoReader reader) throws IOException {
    B builder = newBuilder();
    long token = reader.beginMessage();
    for (int tag; (tag = reader.nextTag()) != -1;) {
      FieldBinding<M, B> fieldBinding = fieldBindings.get(tag);
      try {
        if (fieldBinding != null) {
          Object value = fieldBinding.singleAdapter().decode(reader);
          fieldBinding.value(builder, value);
        } else {
          FieldEncoding fieldEncoding = reader.peekFieldEncoding();
          Object value = fieldEncoding.rawProtoAdapter().decode(reader);
          builder.addUnknownField(tag, fieldEncoding, value);
        }
      } catch (ProtoAdapter.EnumConstantNotFoundException e) {
        // An unknown Enum value was encountered, store it as an unknown field.
        builder.addUnknownField(tag, FieldEncoding.VARINT, (long) e.value);
      }
    }
    reader.endMessage(token);

    return builder.build();
  }
}
