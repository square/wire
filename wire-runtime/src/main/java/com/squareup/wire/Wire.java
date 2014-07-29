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
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import okio.Source;

import static com.squareup.wire.Preconditions.checkArgument;
import static com.squareup.wire.Preconditions.checkNotNull;

/**
 * Encode and decode Wire protocol buffers.
 */
public final class Wire {

  private final Map<Class<? extends Message>, MessageAdapter<? extends Message>> messageAdapters =
      new LinkedHashMap<Class<? extends Message>, MessageAdapter<? extends Message>>();
  private final Map<Class<? extends Message.Builder>,
      BuilderAdapter<? extends Message.Builder>> builderAdapters =
          new LinkedHashMap<Class<? extends Message.Builder>,
              BuilderAdapter<? extends Message.Builder>>();
  private final Map<Class<? extends ProtoEnum>, EnumAdapter<? extends ProtoEnum>> enumAdapters =
      new LinkedHashMap<Class<? extends ProtoEnum>, EnumAdapter<? extends ProtoEnum>>();

  // Visible to MessageAdapter
  final ExtensionRegistry registry;

  /**
   * Creates a new Wire that can encode and decode the extensions specified in
   * {@code extensionClasses}. Typically the classes in this list are generated
   * and start with the "Ext_" prefix.
   */
  public Wire(Class<?>... extensionClasses) {
    this(Arrays.asList(extensionClasses));
  }

  /**
   * Creates a new Wire that can encode and decode the extensions specified in
   * {@code extensionClasses}. Typically the classes in this list are generated
   * and start with the "Ext_" prefix.
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
            throw new AssertionError(e);
          }
        }
      }
    }
  }

  /**
   * Returns a message adapter for {@code messageType}.
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
   * Returns a builder adapter for {@code builderType}.
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
   * Returns an enum adapter for {@code enumClass}.
   */
  @SuppressWarnings("unchecked")
  synchronized <E extends ProtoEnum> EnumAdapter<E> enumAdapter(Class<E> enumClass) {
    EnumAdapter<E> adapter = (EnumAdapter<E>) enumAdapters.get(enumClass);
    if (adapter == null) {
      adapter = new EnumAdapter<E>(enumClass);
      enumAdapters.put(enumClass, adapter);
    }
    return adapter;
  }

  /**
   * Reads a message of type {@code messageClass} from {@code bytes} and returns
   * it.
   */
  public <M extends Message> M parseFrom(byte[] bytes, Class<M> messageClass) throws IOException {
    checkNotNull(bytes, "bytes");
    checkNotNull(messageClass, "messageClass");
    return parseFrom(WireInput.newInstance(bytes), messageClass);
  }

  /**
   * Reads a message of type {@code messageClass} from the given range of {@code
   * bytes} and returns it.
   */
  public <M extends Message> M parseFrom(byte[] bytes, int offset, int count, Class<M> messageClass)
      throws IOException {
    checkNotNull(bytes, "bytes");
    checkArgument(offset >= 0, "offset < 0");
    checkArgument(count >= 0, "count < 0");
    checkArgument(offset + count <= bytes.length, "offset + count > bytes");
    checkNotNull(messageClass, "messageClass");
    return parseFrom(WireInput.newInstance(bytes, offset, count), messageClass);
  }

  /**
   * Reads a message of type {@code messageClass} from the given {@link InputStream} and returns it.
   */
  public <M extends Message> M parseFrom(InputStream input, Class<M> messageClass)
      throws IOException {
    checkNotNull(input, "input");
    checkNotNull(messageClass, "messageClass");
    return parseFrom(WireInput.newInstance(input), messageClass);
  }

  /**
   * Reads a message of type {@code messageClass} from the given {@link Source} and returns it.
   */
  public <M extends Message> M parseFrom(Source input, Class<M> messageClass)
      throws IOException {
    checkNotNull(input, "input");
    checkNotNull(messageClass, "messageClass");
    return parseFrom(WireInput.newInstance(input), messageClass);
  }

  /**
   * Reads a message of type {@code messageClass} from {@code input} and returns it.
   */
  private <M extends Message> M parseFrom(WireInput input, Class<M> messageClass)
      throws IOException {
    MessageAdapter<M> adapter = messageAdapter(messageClass);
    return adapter.read(input);
  }

  /**
   * Returns {@code value} if it is not null; {@code defaultValue} otherwise.
   * This is used to conveniently return a default value when a value is null.
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
   */
  public static <T> T get(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }
}
