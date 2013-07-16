// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.util.Collections;
import java.util.Set;

/**
 * Superclass for protocol buffer messages that declare an extension range.
 *
 * @param <T> the message type being extended.
 */
public abstract class ExtendableMessage<T extends ExtendableMessage<?>> extends Message {

  ExtensionMap<T> extensionMap;

  /**
   * Constructs an ExtendableMessage initialized to the current builder state.
   */
  protected ExtendableMessage(ExtendableBuilder<T> builder) {
    super(builder);
    if (builder.extensionMap != null) {
      this.extensionMap = new ExtensionMap<T>(builder.extensionMap);
    }
  }

  /**
   * Returns the set of extensions that are present on this message instance.
   */
  public Set<Extension<T, ?>> getExtensions() {
    if (extensionMap == null) {
      return Collections.emptySet();
    }
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
    return extensionMap == null ? null : extensionMap.get(extension);
  }

  /**
   * Returns true if the set of extensions on this message is equal to the set of extensions
   * on the given message.
   */
  protected boolean extensionsEqual(ExtendableMessage<T> other) {
    if (extensionMap == null) {
      return other.extensionMap == null;
    }
    return extensionMap.equals(other.extensionMap);
  }

  /**
   * Returns a hash code for the current set of extensions.
   */
  protected int extensionsHashCode() {
    return extensionMap == null ? 0 : extensionMap.hashCode();
  }

  /**
   * Returns a human-readable dump of the current set of extensions.
   */
  protected String extensionsToString() {
    return extensionMap == null ? "{}" : extensionMap.toString();
  }

  /**
   * Superclass for builders of extensible protocol buffer messages.
   */
  public abstract static class ExtendableBuilder<T extends ExtendableMessage<?>>
      extends Builder<T> {

    ExtensionMap<T> extensionMap;

    protected ExtendableBuilder() {
    }

    protected ExtendableBuilder(ExtendableMessage<T> message) {
      super(message);
      if (message != null && message.extensionMap != null) {
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
      return extensionMap == null ? null : extensionMap.get(extension);
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
      if (extensionMap == null) {
        extensionMap = new ExtensionMap<T>();
      }
      extensionMap.put(extension, value);
      return this;
    }
  }
}
