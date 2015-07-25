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

/**
 * A message that declares an extension range.
 *
 * @param <T> the message type being extended.
 */
public abstract class ExtendableMessage<T extends ExtendableMessage<T>> extends Message<T> {
  private static final long serialVersionUID = 0L;

  @SuppressWarnings("unchecked")
  transient ExtensionMap<T> extensionMap; // Null if empty.

  protected ExtendableMessage(String name) {
    super(name);
  }

  /** Initializes extension field data to that stored in the given {@code builder}. */
  protected void setBuilder(ExtendableBuilder<T, ?> builder) {
    super.setBuilder(builder);
    if (builder.extensionMap != null) {
      this.extensionMap = new ExtensionMap<T>(builder.extensionMap);
    }
  }

  /** Returns the value for {@code extension} on this message, or null if no value is set. */
  public <E> E getExtension(Extension<T, ?, E> extension) {
    return extensionMap == null ? null : extensionMap.get(extension);
  }

  /** Builds a message that declares an extension range. */
  public abstract static class ExtendableBuilder<T extends ExtendableMessage<T>,
      B extends ExtendableBuilder<T, B>> extends Builder<T> {

    ExtensionMap<T> extensionMap; // Null if empty.
    private final Class<T> messageType;
    private final Class<B> selfType;

    protected ExtendableBuilder(Class<T> messageType, Class<B> selfType) {
      this(messageType, selfType, null);
    }

    protected ExtendableBuilder(Class<T> messageType, Class<B> selfType, ExtendableMessage<T> message) {
      super(message);
      this.messageType = messageType;
      this.selfType = selfType;

      if (message != null && message.extensionMap != null) {
        extensionMap = new ExtensionMap<T>(message.extensionMap);
      }
    }

    public final void readExtensionOrUnknown(int tag, ProtoReader reader) throws IOException {
      Extension<T, ?, Object> extension =
          (Extension<T, ?, Object>) reader.getExtension(messageType, tag);
      if (extension != null) {
        Object existing = null;
        if (extensionMap != null) {
          existing = extensionMap.get(extension);
        }
        setExtension(extension, extension.read(existing, reader));
      } else {
        readUnknown(tag, reader);
      }
    }

    /** Returns the value for {@code extension} on this message, or null if no value is set. */
    public final <E, R> R getExtension(Extension<T, E, R> extension) {
      return extensionMap == null ? null : extensionMap.get(extension);
    }

    /** Sets the value of {@code extension} on this builder to {@code value}. */
    public final <E> B setExtension(Extension<T, ?, E> extension, E value) {
      ExtensionMap<T> extensionMap = this.extensionMap;
      if (extensionMap == null) {
        this.extensionMap = new ExtensionMap<T>(extension, value);
      } else {
        extensionMap.put(extension, value);
      }
      return selfType.cast(this);
    }
  }
}
