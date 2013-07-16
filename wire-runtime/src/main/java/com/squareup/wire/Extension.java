// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.util.List;

import static com.squareup.wire.Message.ExtendableMessage;

/**
 * An object describing a ProtocolBuffer extension, i.e., a (tag, type, label) tuple
 * associated with a particular {@link com.squareup.wire.Message} type being extended.
 *
 * @param <T> the type of message being extended
 * @param <E> the (boxed) Java data type of the extension value
 */
public final class Extension<T extends ExtendableMessage, E>
    implements Comparable<Extension<?, ?>> {
  private final Class<T> extendedType;
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
   * @param <T> the type of message being extended
   * @param <E> the (boxed) Java data type of the {@link Extension} value
   */
  public static <T extends ExtendableMessage, E> Extension<T, E>
      getExtension(Class<T> extendedType, int tag, int type, int label) {
    return new Extension<T, E>(extendedType, tag, type, label, false, null, null);
  }

  /**
   * Returns an {@link Extension} instance for a repeated built-in datatype.
   *
   * @param extendedType the type of message being extended
   * @param tag the tag number of the extension
   * @param type one of {@code Wire.INT32}, etc.
   * @param packed true if the '[packed = true]' extension is present
   * @param <T> the type of message being extended
   * @param <E> the (boxed) Java data type of the {@link Extension} value
   */
  public static <T extends ExtendableMessage, E> Extension<T, List<E>>
      getRepeatedExtension(Class<T> extendedType, int tag, int type, boolean packed) {
    return new Extension<T, List<E>>(extendedType, tag, type, Wire.REPEATED, packed, null,
        null);
  }

  /**
   * Returns an {@link Extension} instance for a message datatype.
   *
   * @param extendedType the type of message being extended
   * @param tag the tag number of the extension
   * @param label one of {@code Wire.OPTIONAL} or {@code Wire.REQUIRED}
   * @param messageType the class type of the {@link Extension}'s message value
   * @param <T> the type of message being extended
   * @param <E> the Java data type of the {@link Extension} message value
   */
  public static <T extends ExtendableMessage, E extends Message> Extension<T, E>
      getMessageExtension(Class<T> extendedType, int tag, int label,
      Class<E> messageType) {
    return new Extension<T, E>(extendedType, tag, Wire.MESSAGE, label, false, messageType,
        null);
  }

  /**
   * Returns an {@link Extension} instance for a repeated message datatype.
   *
   * @param extendedType the type of message being extended
   * @param tag the tag number of the extension
   * @param messageType the class type of the {@link Extension}'s message value
   * @param <T> the type of message being extended
   * @param <E> the Java data type of the {@link Extension} message value
   */
  public static <T extends ExtendableMessage, E extends Message> Extension<T, List<E>>
      getRepeatedMessageExtension(Class<T> extendedType, int tag, Class<E> messageType) {
    return new Extension<T, List<E>>(extendedType, tag, Wire.MESSAGE, Wire.REPEATED, false,
        messageType, null);
  }

  /**
   * Returns an {@link Extension} instance for an enum datatype.
   *
   * @param extendedType the type of message being extended
   * @param tag the tag number of the extension
   * @param label one of {@code Wire.OPTIONAL}, {@code Wire.REQUIRED}, or {@code Wire.REPEATED}
   * @param enumType the class type of the {@link Extension}'s enum value
   * @param <T> the type of message being extended
   * @param <E> the Java data type of the {@link Extension} enum value
   */
  public static <T extends ExtendableMessage, E extends Enum> Extension<T, E>
      getEnumExtension(Class<T> extendedType, int tag, int label, Class<E> enumType) {
    return new Extension<T, E>(extendedType, tag, Wire.ENUM, label, false, null, enumType);
  }

  /**
   * Returns an {@link Extension} instance for an enum datatype.
   *
   * @param extendedType the type of message being extended
   * @param tag the tag number of the extension
   * @param packed true if the '[packed = true]' extension is present
   * @param enumType the class type of the {@link Extension}'s enum value
   * @param <T> the type of message being extended
   * @param <E> the Java data type of the {@link Extension} enum value
   */
  public static <T extends ExtendableMessage, E extends Enum> Extension<T, List<E>>
      getRepeatedEnumExtension(Class<T> extendedType, int tag, boolean packed,
      Class<E> enumType) {
    return new Extension<T, List<E>>(extendedType, tag, Wire.ENUM, Wire.REPEATED, packed,
        null, enumType);
  }

  private Extension(Class<T> extendedType, int tag, int type, int label,
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
    return other instanceof Extension<?, ?> && compareTo((Extension<?, ?>) other) == 0;
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

  public Class<T> getExtendedType() {
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
