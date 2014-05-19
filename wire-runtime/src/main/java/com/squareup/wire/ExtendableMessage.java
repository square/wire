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

import java.util.Collections;
import java.util.List;

/**
 * A message that declares an extension range.
 *
 * @param <T> the message type being extended.
 */
public abstract class ExtendableMessage<T extends ExtendableMessage<?>> extends Message {

  @SuppressWarnings("unchecked")
  transient ExtensionMap<T> extensionMap; // Null if empty.

  protected ExtendableMessage() {
  }

  /**
   * Initializes any extension and unknown field data to that stored in the given {@code Builder}.
   */
  protected void setBuilder(ExtendableBuilder<T> builder) {
    super.setBuilder(builder);
    if (builder.extensionMap != null) {
      this.extensionMap = new ExtensionMap<T>(builder.extensionMap);
    }
  }

  /**
   * Returns an immutable list of the extensions on this message in tag order.
   */
  public List<Extension<T, ?>> getExtensions() {
    return extensionMap == null ? Collections.<Extension<T, ?>>emptyList()
        : extensionMap.getExtensions();
  }

  /**
   * Returns the value for {@code extension} on this message, or null if no
   * value is set.
   */
  public <E> E getExtension(Extension<T, E> extension) {
    return extensionMap == null ? null : extensionMap.get(extension);
  }

  /**
   * Returns true if the extensions on this message equals the extensions of
   * {@code other}.
   */
  protected boolean extensionsEqual(ExtendableMessage<T> other) {
    if (extensionMap == null) {
      return other.extensionMap == null;
    }
    return extensionMap.equals(other.extensionMap);
  }

  /**
   * Returns a hash code for the extensions on this message.
   */
  protected int extensionsHashCode() {
    return extensionMap == null ? 0 : extensionMap.hashCode();
  }

  /**
   * Returns a string describing the extensions on this message.
   */
  String extensionsToString() {
    return extensionMap == null ? "{}" : extensionMap.toString();
  }

  /**
   * Builds a message that declares an extension range.
   */
  public abstract static class ExtendableBuilder<T extends ExtendableMessage<?>>
      extends Builder<T> {

    @SuppressWarnings("unchecked")
    ExtensionMap<T> extensionMap; // Null if empty.

    protected ExtendableBuilder() {
    }

    protected ExtendableBuilder(ExtendableMessage<T> message) {
      super(message);
      if (message != null && message.extensionMap != null) {
        this.extensionMap = new ExtensionMap<T>(message.extensionMap);
      }
    }

    /**
     * Returns the value for {@code extension} on this message, or null if no
     * value is set.
     */
    public <E> E getExtension(Extension<T, E> extension) {
      return extensionMap == null ? null : extensionMap.get(extension);
    }

    /**
     * Sets the value of {@code extension} on this builder to {@code value}.
     */
    public <E> ExtendableBuilder<T> setExtension(Extension<T, E> extension, E value) {
      if (extensionMap == null) {
        extensionMap = new ExtensionMap<T>(extension, value);
      } else {
        extensionMap.put(extension, value);
      }
      return this;
    }
  }
}
