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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main class for Wire mobile protocol buffers.
 */
public final class Wire {

  private final Map<Class<? extends Message>, MessageAdapter<? extends Message>> messageAdapters =
      new HashMap<Class<? extends Message>, MessageAdapter<? extends Message>>();
  private final Map<Class<? extends Message.Builder>,
      BuilderAdapter<? extends Message.Builder>> builderAdapters =
          new HashMap<Class<? extends Message.Builder>,
              BuilderAdapter<? extends Message.Builder>>();
  private final Map<Class<? extends Enum>, EnumAdapter<? extends Enum>> enumAdapters =
      new HashMap<Class<? extends Enum>, EnumAdapter<? extends Enum>>();

  // Visible to MessageAdapter
  final ExtensionRegistry registry;

  /**
   * Register all {@link Extension} objects defined as static fields on the given classes.
   *
   * @param extensionClasses an array of zero or more classes to search
   */
  public Wire(Class<?>... extensionClasses) {
    this(Arrays.asList(extensionClasses));
  }

  /**
   * Register all {@link Extension} objects defined as static fields on the given classes.
   *
   * @param extensionClasses a list of zero or more classes to search
   */
  public Wire(List<Class<?>> extensionClasses) {
    this.registry = new ExtensionRegistry();
    for (Class<?> extensionClass : extensionClasses) {
      for (Field field : extensionClass.getDeclaredFields()) {
        if (field.getType().equals(Extension.class)) {
          try {
            Extension extension = (Extension) field.get(null);
            registry.add(extension);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  /**
   * Returns a {@link MessageAdapter} for the given message type.
   *
   * @param messageType the {@link Message} class
   */
  @SuppressWarnings("unchecked")
  synchronized <M extends Message> MessageAdapter<M> messageAdapter(Class<M> messageType) {
    MessageAdapter<M> adapter = (MessageAdapter<M>) messageAdapters.get(messageType);
    if (adapter == null) {
      adapter = new MessageAdapter<M>(this, messageType);
      messageAdapters.put(messageType, adapter);
    }
    return adapter;
  }

  /**
   * Returns a {@link BuilderAdapter} for the given message type.
   *
   * @param builderType the {@link Message.Builder} class
   */
  @SuppressWarnings("unchecked")
  synchronized <B extends Message.Builder> BuilderAdapter<B>
      builderAdapter(Class<B> builderType) {
    BuilderAdapter<B> adapter = (BuilderAdapter<B>) builderAdapters.get(builderType);
    if (adapter == null) {
      adapter = new BuilderAdapter<B>(builderType);
      builderAdapters.put(builderType, adapter);
    }
    return adapter;
  }

  /**
   * Returns an {@link EnumAdapter} for the given enum class.
   *
   * @param enumClass the enum class
   */
  @SuppressWarnings("unchecked")
  synchronized <E extends Enum> EnumAdapter<E> enumAdapter(Class<E> enumClass) {
    EnumAdapter<E> adapter = (EnumAdapter<E>) enumAdapters.get(enumClass);
    if (adapter == null) {
      adapter = new EnumAdapter<E>(enumClass);
      enumAdapters.put(enumClass, adapter);
    }
    return adapter;
  }

  /**
   * Parse an entire byte array into a {@link Message} of the given message class.
   * Equivalent to {@code parseFrom(messageClass, bytes, 0, bytes.length)}.
   *
   * @param messageClass the class of the outermost {@link Message}
   * @param bytes an array of bytes
   * @param <M> the outermost {@link Message} class
   * @return an instance of the desired message class
   * @throws IOException if parsing fails
   */
  public <M extends Message> M parseFrom(Class<M> messageClass, byte[] bytes) throws IOException {
    return parseFrom(messageClass, bytes, 0, bytes.length);
  }

  /**
   * Parses a given range of bytes into a {@link Message} of the given message class.
   *
   * @param messageClass the class of the outermost {@link Message}
   * @param bytes an array of bytes
   * @param offset the starting offset within the array
   * @param count the number of bytes to use
   * @param <M> the outermost {@link Message} class
   * @return an instance of the desired message class
   * @throws IOException if parsing fails
   */
  public <M extends Message> M parseFrom(Class<M> messageClass,
      byte[] bytes, int offset, int count) throws IOException {
    return parseFrom(messageClass, WireInput.newInstance(bytes, offset, count));
  }

  /**
   * Parse from a {@link WireInput} instance into a {@link Message} of the given message class.
   *
   * @param messageClass the class of the outermost {@link Message}
   * @param input an instance of {@link WireInput}
   * @param <M> the outermost {@link Message} class
   * @return an instance of the desired message class
   * @throws IOException if parsing fails
   */
  public <M extends Message> M parseFrom(Class<M> messageClass, WireInput input)
      throws IOException {
    MessageAdapter<M> adapter = messageAdapter(messageClass);
    return adapter.read(input);
  }

  /**
   * Utility to return a default value when a protobuf value is null.
   * For example,
   *
   * <pre>
   * MyProto myProto = ...
   * MyField field = Wire.get(myProto.f, MyProto.f_default);
   * </pre>
   *
   * will attempt to retrieve the value of the field 'f' defined by MyProto.
   * If the field is null (i.e., unset), <code>get</code> will return its
   * second argument, which in this case is the default value for the field
   * 'f'.
   *
   * @param value the value to return if non-null
   * @param defaultValue the value to return if value is null
   * @param <T> the value type
   * @return one of value or defaultValue
   */
  public static <T> T get(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }
}
