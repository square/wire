// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.util.List;

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
     * An object describing a ProtocolBuffer extension, i.e., a (tag, type, label) tuple
     * associated with a particular {@link Message} type being extended.
     *
     * @param <ExtendedType> the type of message being extended
     * @param <Type> the (boxed) Java data type of the extension value
     */
    public static final class Extension<ExtendedType extends ExtendableMessage, Type>
        implements Comparable<Extension<?, ?>> {
      private final Class<ExtendedType> extendedType;
      private final Class<? extends Message> messageType;
      private final Class<? extends Enum> enumType;
      private final int tag;
      private final int type;
      private final int label;
      private final boolean packed;

      /**
       * Returns an {@link Extension} instance for a built-in datatype.
       *
       * @param extendedType the type of message being extended
       * @param tag the tag number of the extension
       * @param type one of {@code Wire.INT32}, etc.
       * @param label one of {@code Wire.OPTIONAL} or {@code Wire.REQUIRED}
       * @param <X> the type of message being extended
       * @param <Type> the (boxed) Java data type of the {@link Extension} value
       */
      public static <X extends ExtendableMessage, Type> Extension<X, Type>
          getExtension(Class<X> extendedType, int tag, int type, int label) {
        return new Extension<X, Type>(extendedType, tag, type, label, false, null, null);
      }

      /**
       * Returns an {@link Extension} instance for a repeated built-in datatype.
       *
       * @param extendedType the type of message being extended
       * @param tag the tag number of the extension
       * @param type one of {@code Wire.INT32}, etc.
       * @param packed true if the '[packed = true]' extension is present
       * @param <X> the type of message being extended
       * @param <Type> the (boxed) Java data type of the {@link Extension} value
       */
      public static <X extends ExtendableMessage, Type> Extension<X, List<Type>>
          getRepeatedExtension(Class<X> extendedType, int tag, int type, boolean packed) {
        return new Extension<X, List<Type>>(extendedType, tag, type, Wire.REPEATED, packed, null,
            null);
      }

      /**
       * Returns an {@link Extension} instance for a message datatype.
       *
       * @param extendedType the type of message being extended
       * @param tag the tag number of the extension
       * @param label one of {@code Wire.OPTIONAL} or {@code Wire.REQUIRED}
       * @param messageType the class type of the {@link Extension}'s message value
       * @param <X> the type of message being extended
       * @param <Type> the Java data type of the {@link Extension} message value
       */
      public static <X extends ExtendableMessage, Type extends Message> Extension<X, Type>
          getMessageExtension(Class<X> extendedType, int tag, int label,
              Class<Type> messageType) {
        return new Extension<X, Type>(extendedType, tag, Wire.MESSAGE, label, false, messageType,
            null);
      }

      /**
       * Returns an {@link Extension} instance for a repeated message datatype.
       *
       * @param extendedType the type of message being extended
       * @param tag the tag number of the extension
       * @param messageType the class type of the {@link Extension}'s message value
       * @param <X> the type of message being extended
       * @param <Type> the Java data type of the {@link Extension} message value
       */
      public static <X extends ExtendableMessage, Type extends Message> Extension<X, List<Type>>
          getRepeatedMessageExtension(Class<X> extendedType, int tag, Class<Type> messageType) {
        return new Extension<X, List<Type>>(extendedType, tag, Wire.MESSAGE, Wire.REPEATED, false,
            messageType, null);
      }

      /**
       * Returns an {@link Extension} instance for an enum datatype.
       *
       * @param extendedType the type of message being extended
       * @param tag the tag number of the extension
       * @param label one of {@code Wire.OPTIONAL}, {@code Wire.REQUIRED}, or {@code Wire.REPEATED}
       * @param enumType the class type of the {@link Extension}'s enum value
       * @param <X> the type of message being extended
       * @param <Type> the Java data type of the {@link Extension} enum value
       */
      public static <X extends ExtendableMessage, Type extends Enum> Extension<X, Type>
          getEnumExtension(Class<X> extendedType, int tag, int label, Class<Type> enumType) {
        return new Extension<X, Type>(extendedType, tag, Wire.ENUM, label, false, null, enumType);
      }

      /**
       * Returns an {@link Extension} instance for an enum datatype.
       *
       * @param extendedType the type of message being extended
       * @param tag the tag number of the extension
       * @param packed true if the '[packed = true]' extension is present
       * @param enumType the class type of the {@link Extension}'s enum value
       * @param <X> the type of message being extended
       * @param <Type> the Java data type of the {@link Extension} enum value
       */
      public static <X extends ExtendableMessage, Type extends Enum> Extension<X, List<Type>>
          getRepeatedEnumExtension(Class<X> extendedType, int tag, boolean packed,
              Class<Type> enumType) {
        return new Extension<X, List<Type>>(extendedType, tag, Wire.ENUM, Wire.REPEATED, packed,
            null, enumType);
      }

      private Extension(Class<ExtendedType> extendedType, int tag, int type, int label,
          boolean packed, Class<? extends Message> messageType, Class<? extends Enum> enumType) {
        this.extendedType = extendedType;
        this.tag = tag;
        this.type = type;
        this.label = label;
        this.packed = packed;
        this.messageType = messageType;
        this.enumType = enumType;
      }

      /**
       * Orders Extensions in ascending tag order.
       */
      @Override public int compareTo(Extension<?, ?> o) {
        if (tag != o.tag) {
          return tag - o.tag;
        }
        if (type != o.type) {
          return type - o.type;
        }
        if (label != o.label) {
          return label - o.label;
        }
        if (packed != o.packed) {
          return packed ? 1 : -1;
        }
        if (extendedType != null && !extendedType.equals(o.extendedType)) {
          return extendedType.getCanonicalName().compareTo(o.extendedType.getCanonicalName());
        }
        if (messageType != null && !messageType.equals(o.messageType)) {
          return messageType.getCanonicalName().compareTo(o.messageType.getCanonicalName());
        }
        if (enumType != null && !enumType.equals(o.enumType)) {
          return enumType.getCanonicalName().compareTo(o.enumType.getCanonicalName());
        }
        return 0;
      }

      @Override public boolean equals(Object other) {
        if (!(other instanceof Extension<?, ?>)) {
          return false;
        }
        return compareTo((Extension<?, ?>) other) == 0;
      }

      @Override public int hashCode() {
        int hash = tag;
        hash = hash * 37 + type;
        hash = hash * 37 + label;
        hash = hash * 37 + (packed ? 1 : 0);
        hash = hash * 37 + extendedType.getCanonicalName().hashCode();
        hash = hash * 37 + (messageType != null ? messageType.getCanonicalName().hashCode() : 0);
        hash = hash * 37 + (enumType != null ? enumType.getCanonicalName().hashCode() : 0);
        return hash;
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

      public boolean getPacked() {
        return packed;
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
