// Copyright 2013 Square, Inc.
package com.squareup.wire;

/**
 * An interface implemented by protocol buffer messages.
 */
public interface Message {

  /**
   * An interface implemented by protocol buffer message builders.
   */
  public interface Builder<T extends Message> {
    /**
     * Returns true if all required fields have been set.
     */
    boolean isInitialized();

    /**
     * Returns an immutable {@link Message} based on the fields that have been set
     * in this builder.
     */
    T build();
  }

  /**
   * An interface implemented by protocol buffer messages that declare an extension range.
   *
   * @param <MessageType>
   */
  public interface ExtendableMessage<MessageType extends ExtendableMessage> extends Message {

    /**
     * Returns the value of an extension field set on this message.
     *
     * @param extension the {@link Extension}
     * @param <Type> the (boxed) Java datatype of the extension value
     * @return the extension value, or null
     */
    <Type> Type getExtension(Extension<MessageType, Type> extension);

    /**
     * An interface implemented by builders for extensible protocol buffer messages.
     */
    public interface ExtendableBuilder<MessageType extends ExtendableMessage>
        extends Builder<MessageType> {
      /**
       * Returns the value of an extension field set on this builder.
       *
       * @param extension the {@link Extension}
       * @param <Type> the (boxed) Java datatype of the extension value
       * @return the extension value, or null
       */
      <Type> Type getExtension(Extension<MessageType, Type> extension);

      /**
       * Sets the value of an extension field on this builder.
       *
       * @param extension the {@link Extension}
       * @param val the extension value
       * @param <Type> the (boxed) Java datatype of the extension value
       * @return a reference to this builder
       */
      <Type> ExtendableBuilder<MessageType> setExtension(Extension<MessageType, Type> extension,
          Type val);
    }
  }
}
