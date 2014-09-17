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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okio.Source;

import static com.squareup.wire.Message.Builder;
import static com.squareup.wire.Message.Label;
import static com.squareup.wire.MessageAdapter.FieldInfo;
import static com.squareup.wire.Preconditions.checkArgument;
import static com.squareup.wire.Preconditions.checkNotNull;

/**
 * Encode and decode Wire protocol buffers.
 */
public final class Wire {

  private final Map<Class<? extends Message>, MessageAdapter<? extends Message>> messageAdapters =
      new LinkedHashMap<Class<? extends Message>, MessageAdapter<? extends Message>>();
  private final Map<Class<? extends Builder>,
      BuilderAdapter<? extends Builder>> builderAdapters =
          new LinkedHashMap<Class<? extends Builder>,
              BuilderAdapter<? extends Builder>>();
  private final Map<Class<? extends ProtoEnum>, EnumAdapter<? extends ProtoEnum>> enumAdapters =
      new LinkedHashMap<Class<? extends ProtoEnum>, EnumAdapter<? extends ProtoEnum>>();

  // Visible to MessageAdapter
  final ExtensionRegistry registry;
  final List<InterceptorFactory> interceptorFactories;

  /**
   * Creates a new Wire that can encode and decode the extensions specified in
   * {@code extensionClasses}. Typically the classes in this list are generated
   * and start with the "Ext_" prefix.
   */
  public Wire(Class<?>... extensionClasses) {
    this(Arrays.asList(extensionClasses), null);
  }


  /**
   * Creates a new Wire that can encode and decode the extensions specified in
   * {@code extensionClasses}. Typically the classes in this list are generated
   * and start with the "Ext_" prefix.
   */
  public Wire(List<Class<?>> extensionClasses) {
    this(extensionClasses, null);
  }

  /**
   * Creates a new Wire that can encode and decode the extensions specified in
   * {@code extensionClasses}. Typically the classes in this list are generated
   * and start with the "Ext_" prefix. Additionally, a list of {@link Interceptor}s
   * will be run in order when serializing messages using methods from this class.
   * When deserializing, the same set of interceptors will be run in reverse order.
   */
  public Wire(List<Class<?>> extensionClasses, List<InterceptorFactory> interceptorFactories) {
    this.registry = new ExtensionRegistry();
    for (Class<?> extensionClass : extensionClasses) {
      for (Field field : extensionClass.getDeclaredFields()) {
        if (field.getType().equals(Extension.class)) {
          try {
            Extension<? extends ExtendableMessage<?>, ?> extension =
                (Extension<? extends ExtendableMessage<?>, ?>) field.get(null);
            registry.add(extension);
          } catch (IllegalAccessException e) {
            throw new AssertionError(e);
          }
        }
      }
    }

    this.interceptorFactories = new ArrayList<InterceptorFactory>();
    if (interceptorFactories != null) {
      this.interceptorFactories.addAll(interceptorFactories);
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
   * Returns a Collection containing all known {@link Extension}s that may be applied to the
   * given message type.
   */
  public Collection<Extension<?, ?>> getExtensions(Class<? extends ExtendableMessage<?>>
      messageType) {
    return registry.getExtensions(messageType);
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
   * Reads a message of type {@code messageClass} from {@code input}, runs all matching
   * interceptors (in the reverse of the order they are run during serialization) and returns it.
   */
  private <M extends Message> M parseFrom(WireInput input, Class<M> messageClass)
      throws IOException {
    MessageAdapter<M> adapter = messageAdapter(messageClass);
    return interceptDeserialize(adapter.read(input), messageClass, adapter);
  }

  /**
   * Serializes the given Message, running all matching interceptors.
   */
  public byte[] toByteArray(Message message) {
    message = interceptSerialize(message);
    return message.toByteArray();
  }

  /**
   * Serializes the given Message, running all matching interceptors.
   */
  public void writeTo(Message message, byte[] output) {
    message = interceptSerialize(message);
    message.writeTo(output);
  }

  /**
   * Serializes the given Message, running all matching interceptors.
   */
  public void writeTo(Message message, byte[] output, int offset, int count) {
    message = interceptSerialize(message);
    message.writeTo(output, offset, count);
  }

  /**
   * A set of Message classes that have been determined to have no fields (or sub- or sub-sub-
   * fields, etc.) that require running interceptors. As we perform serialization and
   * deserialization, this set may grow. For example, consider a message A with sub-message B
   * and sub-sub-message C. The first time we serialize an instance of message C, we detect that
   * it has no message fields, so it is added to this set. Later when we serialize and instance
   * of message B, we detect that all of its message fields (namely C) are already in this set,
   * so we add B to the set as well. When we next attempt to serialize an instance of message B,
   * we can see immediately that there is no need to recurse further.
   */
  private Set<Class<? extends Message>> messagesWithNoInterceptedFields =
      new LinkedHashSet<Class<? extends Message>>();

  /**
   * Returns true if we have previously determined that the given Message class does not require
   * interception, i.e., it contains no fields that require interception and does not have any
   * interceptors itself.
   */
  private boolean canSkipFieldInterception(Class<? extends Message> messageClass) {
    return messagesWithNoInterceptedFields.contains(messageClass);
  }

  /**
   * Marks the given Message class as not requiring interception, i.e., it contains no fields that
   * require interception and does not have any interceptors itself.
   */
  private <M extends Message> void setCanSkipInterception(Class<M> messageClass) {
    messagesWithNoInterceptedFields.add(messageClass);
  }

  @SuppressWarnings("unchecked")
  private <M extends Message> M interceptSerialize(M message) {
    Class<M> messageClass = (Class<M>) message.getClass();
    return interceptSerialize(message, messageClass, messageAdapter(messageClass));
  }

  @SuppressWarnings("unchecked")
  private <M extends Message> M interceptSerialize(M message, Class<M> messageClass,
      MessageAdapter<M> adapter) {
    List<Interceptor> interceptors = adapter.getInterceptors();

    // Perform interception on nested messages if needed.
    if (!canSkipFieldInterception(messageClass)) {
      Builder<M> builder = null;
      boolean hasPossiblyInterceptedField = false;
      for (FieldInfo fieldInfo : adapter.getMessageFields()) {
        if (canSkipFieldInterception(fieldInfo.messageType)) {
          continue;
        }
        hasPossiblyInterceptedField = true;

        Object value = adapter.getFieldValue(message, fieldInfo);
        if (value == null) {
          continue;
        }
        if (fieldInfo.label.isRepeated()) {
          builder = interceptSerializeField(builder, message, adapter, fieldInfo,
              (List<Message>) value);
        } else {
          builder = interceptSerializeField(builder, message, adapter, fieldInfo, (Message) value);
        }
      }
      // Nothing in this message can require interception, so we can short-circuit this
      // process next time.
      if (!hasPossiblyInterceptedField && interceptors.isEmpty()) {
        setCanSkipInterception(messageClass);
      }
      if (builder != null) {
        message = builder.build();
      }
    }

    for (Interceptor interceptor : interceptors) {
      message = interceptor.preSerialize(message);
    }
    return message;
  }

  private <M extends Message> Builder<M> interceptSerializeField(Builder<M> builder, M outerMessage,
      MessageAdapter<M> adapter, FieldInfo fieldInfo, List<Message> valueList) {
    List<Message> newList = new ArrayList<Message>(valueList.size());
    boolean listChanged = false;
    for (int i = 0; i < valueList.size(); i++) {
      Message oldValue = valueList.get(i);
      Message newValue = interceptSerialize(oldValue);
      // Assume that the return value will be == to the original value if no change occurred.
      if (newValue != oldValue) {
        listChanged = true;
      }
      newList.add(newValue);
    }
    if (listChanged) {
      builder = setField(builder, outerMessage, adapter, fieldInfo.builderField, newList);
    }
    return builder;
  }

  private <M extends Message> Builder<M> interceptSerializeField(Builder<M> builder, M outerMessage,
      MessageAdapter<M> adapter, FieldInfo fieldInfo, Message value) {
    Message newValue = interceptSerialize(value);
    // Assume that the return value will be == to the original value if no change occurred.
    if (newValue != value) {
      builder = setField(builder, outerMessage, adapter, fieldInfo.builderField, newValue);
    }
    return builder;
  }

  private <M extends Message> Builder<M> setField(Builder<M> builder, M message,
      MessageAdapter<M> adapter, Field builderField, Object newValue) {
    if (builder == null) {
      builder = adapter.newBuilder(message);
    }
    try {
      builderField.set(builder, newValue);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
    return builder;
  }

  @SuppressWarnings("unchecked")
  private <M extends Message> M interceptDeserialize(M message) {
    Class<M> messageClass = (Class<M>) message.getClass();
    return interceptDeserialize(message, messageClass, messageAdapter(messageClass));
  }

  @SuppressWarnings("unchecked")
  private <M extends Message> M interceptDeserialize(M message, Class<M> messageClass,
      MessageAdapter<M> adapter) {
    // Traverse the interceptors in reverse order.
    List<Interceptor> interceptors = adapter.getInterceptors();
    for (int i = interceptors.size() - 1; i >= 0; i--) {
      message = interceptors.get(i).postDeserialize(message);
    }

    if (!canSkipFieldInterception(messageClass)) {
      Builder<M> builder = null;
      boolean hasPossiblyInterceptedField = false;
      for (FieldInfo fieldInfo : adapter.getMessageFields()) {
        if (canSkipFieldInterception(fieldInfo.messageType)) {
          continue;
        }
        hasPossiblyInterceptedField = true;
        Object value = adapter.getFieldValue(message, fieldInfo);
        if (value == null) {
          continue;
        }
        if (fieldInfo.label.isRepeated()) {
          builder = interceptDeserializeField(builder, message, adapter, fieldInfo,
              (List<Message>) value);
        } else {
          builder = interceptDeserializeField(builder, message, adapter, fieldInfo,
              (Message) value);
        }
      }
      // Nothing in this message can require interception, so we can short-circuit this
      // process next time.
      if (!hasPossiblyInterceptedField && interceptors.isEmpty()) {
        setCanSkipInterception(messageClass);
      }
      if (builder != null) {
        message = builder.build();
      }
    }

    return message;
  }

  private <M extends Message> Builder<M> interceptDeserializeField(Builder<M> builder,
      M outerMessage, MessageAdapter<M> adapter, FieldInfo fieldInfo, List<Message> valueList) {
    List<Message> newList = new ArrayList<Message>(valueList.size());
    boolean listChanged = false;
    for (Message oldValue : valueList) {
      Message newValue = interceptDeserialize(oldValue);
      // Assume that the return value will be == to the original value if no change occurred.
      if (newValue != oldValue) {
        listChanged = true;
      }
      newList.add(newValue);
    }
    if (listChanged) {
      builder = setField(builder, outerMessage, adapter, fieldInfo.builderField, newList);
    }
    return builder;
  }

  private <M extends Message> Builder<M> interceptDeserializeField(Builder<M> builder,
      M outerMessage, MessageAdapter<M> adapter, FieldInfo fieldInfo, Message value) {
    Message newValue = interceptDeserialize(value);
    // Assume that the return value will be == to the original value if no change occurred.
    if (newValue != value) {
      builder = setField(builder, outerMessage, adapter, fieldInfo.builderField, newValue);
    }
    return builder;
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
