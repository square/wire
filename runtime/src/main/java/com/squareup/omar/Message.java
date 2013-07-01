// Copyright 2013 Square, Inc.
package com.squareup.omar;

/**
 * An interface implemented by protocol buffer messages.
 */
public interface Message {

  /**
   * An interface implemented by protocol buffer message builders.
   */
  public static interface Builder<T extends Message> {
    /**
     * Returns true if all required fields have been set.
     */
    boolean isInitialized();

    /**
     * Returns an immutable {@link Message} based on the fields that have been set
     * in this builder.
     *
     * @return
     */
    T build();
  }

  /**
   * An interface implemented by protocol buffer messages that declare an extension range.
   *
   * @param <MessageType>
   */
  public static interface ExtendableMessage<MessageType extends ExtendableMessage> extends Message {

    /**
     * An object describing a ProtocolBuffer extension, i.e., a (tag, type, label) tuple
     * associated with a particular {@link Message} type being extended.
     *
     * @param <ExtendedType> the type of message being extended
     * @param <Type> the (boxed) Java data type of the extension value
     */
    public static class Extension<ExtendedType extends ExtendableMessage, Type> implements Comparable<Extension> {
      private final Class<ExtendedType> extendedType;
      private final Class<? extends Message> messageType;
      private final Class<? extends Enum> enumType;
      private final int tag;
      private final int type;
      private final int label;

      /**
       * Returns an {@link Extension} instance for a built-in datatype.
       *
       * @param extendedType the type of message being extended
       * @param tag the tag number of the extension
       * @param type one of {@code Omar.INT32}, etc.
       * @param label one of {@code Omar.OPTIONAL}, {@code Omar.REQUIRED}, or {@code Omar.REPEATED}
       * @param <X> the type of message being extended
       * @param <Type> the (boxed) Java data type of the {@link Extension} value
       */
      public static <X extends ExtendableMessage, Type> Extension<X, Type> getExtension(Class<X> extendedType,
          int tag, int type, int label) {
        return new Extension<X, Type>(extendedType, tag, type, label, null, null);
      }

      /**
       * Returns an {@link Extension} instance for a message datatype.
       *
       * @param extendedType the type of message being extended
       * @param tag the tag number of the extension
       * @param label one of {@code Omar.OPTIONAL}, {@code Omar.REQUIRED}, or {@code Omar.REPEATED}
       * @param messageType the class type of the {@link Extension}'s message value
       * @param <X> the type of message being extended
       * @param <Type> the Java data type of the {@link Extension} message value
       */
      public static <X extends ExtendableMessage, Type extends Message> Extension<X, Type>
          getMessageExtension(Class<X> extendedType, int tag, int label,
              Class<Type> messageType) {
        return new Extension<X, Type>(extendedType, tag, Omar.MESSAGE, label, messageType, null);
      }

      /**
       * Returns an {@link Extension} instance for an enum datatype.
       *
       * @param extendedType the type of message being extended
       * @param tag the tag number of the extension
       * @param label one of {@code Omar.OPTIONAL}, {@code Omar.REQUIRED}, or {@code Omar.REPEATED}
       * @param enumType the class type of the {@link Extension}'s enum value
       * @param <X> the type of message being extended
       * @param <Type> the Java data type of the {@link Extension} enum value
       */
      public static <X extends ExtendableMessage, Type extends Enum> Extension<X, Type>
          getEnumExtension(Class<X> extendedType, int tag, int label,
              Class<Type> enumType) {
        return new Extension<X, Type>(extendedType, tag, Omar.ENUM, label, null, enumType);
      }

      private Extension(Class <ExtendedType> extendedType, int tag, int type, int label,
          Class<? extends Message> messageType, Class<? extends Enum> enumType) {
        this.extendedType = extendedType;
        this.messageType = messageType;
        this.enumType = enumType;
        this.tag = tag;
        this.type = type;
        this.label = label;
      }

      /**
       * Orders Extensions in ascending tag order.
       */
      @Override public final int compareTo(Extension o) {
        return tag - o.tag;
      }

      public Class<ExtendedType> getExtendedType() {
        return extendedType;
      }

      public Class<? extends Message> getMessageType() {
        return messageType;
      }

      public Class<? extends Enum> getEnumType() {
        return enumType;
      }

      public int getTag() {
        return tag;
      }

      public int getType() {
        return type;
      }

      public int getLabel() {
        return label;
      }
    }

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
    public static interface ExtendableBuilder<MessageType extends ExtendableMessage> extends Builder<MessageType> {
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
      <Type> ExtendableBuilder<MessageType> setExtension(Extension<MessageType, Type> extension, Type val);
    }
  }
}
