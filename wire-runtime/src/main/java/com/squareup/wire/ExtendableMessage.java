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

import java.util.List;

/**
 * Superclass for protocol buffer messages that declare an extension range.
 *
 * @param <T> the message type being extended.
 */
public abstract class ExtendableMessage<T extends ExtendableMessage<?>> extends Message {

  private static final ExtensionMap EMPTY_EXTENSION_MAP = new ExtensionMap();

  @SuppressWarnings("unchecked")
  ExtensionMap<T> extensionMap = (ExtensionMap<T>) EMPTY_EXTENSION_MAP;

  /**
   * Constructs an ExtendableMessage initialized to the current builder state.
   */
  protected ExtendableMessage(ExtendableBuilder<T> builder) {
    super(builder);
    if (!builder.extensionMap.isEmpty()) {
      this.extensionMap = new ExtensionMap<T>(builder.extensionMap);
    }
  }

  /**
   * Returns a {@link List} of extensions that are present on this message instance in tag order.
   */
  public List<Extension<T, ?>> getExtensions() {
    return extensionMap.getExtensions();
  }

  /**
   * Returns the value of an extension field set on this message.
   *
   * @param extension the {@link Extension}
   * @param <E> the (boxed) Java datatype of the extension value
   * @return the extension value, or null
   */
  public <E> E getExtension(Extension<T, E> extension) {
    return extensionMap.get(extension);
  }

  /**
   * Returns true if the set of extensions on this message is equal to the set of extensions
   * on the given message.
   */
  protected boolean extensionsEqual(ExtendableMessage<T> other) {
    return extensionMap.equals(other.extensionMap);
  }

  /**
   * Returns a hash code for the current set of extensions.
   */
  protected int extensionsHashCode() {
    return extensionMap.hashCode();
  }

  /**
   * Returns a human-readable dump of the current set of extensions.
   */
  String extensionsToString() {
    return extensionMap.toString();
  }

  /**
   * Superclass for builders of extensible protocol buffer messages.
   */
  public abstract static class ExtendableBuilder<T extends ExtendableMessage<?>>
      extends Builder<T> {

    @SuppressWarnings("unchecked")
    ExtensionMap<T> extensionMap = EMPTY_EXTENSION_MAP;

    protected ExtendableBuilder() {
    }

    protected ExtendableBuilder(ExtendableMessage<T> message) {
      super(message);
      if (message != null && !message.extensionMap.isEmpty()) {
        this.extensionMap = new ExtensionMap<T>(message.extensionMap);
      }
    }

    /**
     * Returns the value of an extension field set on this builder.
     *
     * @param extension the {@link Extension}
     * @param <E> the (boxed) Java datatype of the extension value
     * @return the extension value, or null
     */
    public <E> E getExtension(Extension<T, E> extension) {
      return extensionMap.get(extension);
    }

    /**
     * Sets the value of an extension field on this builder.
     *
     * @param extension the {@link Extension}
     * @param value the extension value
     * @param <E> the (boxed) Java datatype of the extension value
     * @return a reference to this builder
     */
    public <E> ExtendableBuilder<T> setExtension(Extension<T, E> extension, E value) {
      if (extensionMap == EMPTY_EXTENSION_MAP) {
        extensionMap = new ExtensionMap<T>();
      }
      extensionMap.put(extension, value);
      return this;
    }
  }
}
