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

import static com.squareup.wire.Message.Datatype;
import static com.squareup.wire.Message.Label;

/**
 * An object describing a ProtocolBuffer extension, i.e., a (tag, datatype, label) tuple
 * associated with a particular {@link com.squareup.wire.Message} type being extended.
 *
 * @param <T> the type of message being extended
 * @param <E> the (boxed) Java data type of the extension value
 */
public final class Extension<T extends ExtendableMessage<?>, E>
    implements Comparable<Extension<?, ?>> {
  private final Class<T> extendedType;
  private final Class<? extends Message> messageType;
  private final Class<? extends Enum> enumType;
  private final int tag;
  private final Datatype datatype;
  private final Label label;

  /**
   * Returns an {@link Extension} instance for a built-in datatype.
   *
   * @param extendedType the type of message being extended
   * @param tag the tag number of the extension
   * @param label one of {@link Label#OPTIONAL} or {@link Label#REQUIRED}
   * @param <T> the type of message being extended
   * @param <E> the (boxed) Java data type of the {@link Extension} value
   */
  public static <T extends ExtendableMessage<?>, E> Extension<T, E>
      getExtension(Class<T> extendedType, int tag, Datatype datatype, Label label) {
    return new Extension<T, E>(extendedType, tag, datatype, label, null, null);
  }

  /**
   * Returns an {@link Extension} instance for a repeated built-in datatype.
   *
   * @param extendedType the type of message being extended
   * @param tag the tag number of the extension
   * @param <T> the type of message being extended
   * @param <E> the (boxed) Java data type of the {@link Extension} value
   */
  public static <T extends ExtendableMessage<?>, E> Extension<T, List<E>>
      getRepeatedExtension(Class<T> extendedType, int tag, Datatype datatype, boolean packed) {
    return new Extension<T, List<E>>(extendedType, tag, datatype,
        packed ? Label.PACKED : Label.REPEATED, null, null);
  }

  /**
   * Returns an {@link Extension} instance for a message datatype.
   *
   * @param extendedType the type of message being extended
   * @param tag the tag number of the extension
   * @param label one of {@code Label#OPTIONAL} or {@code Label#REQUIRED}
   * @param messageType the class type of the {@link Extension}'s message value
   * @param <T> the type of message being extended
   * @param <E> the Java data type of the {@link Extension} message value
   */
  public static <T extends ExtendableMessage<?>, E extends Message> Extension<T, E>
      getMessageExtension(Class<T> extendedType, int tag, Label label,
      Class<E> messageType) {
    return new Extension<T, E>(extendedType, tag, Datatype.MESSAGE, label, messageType, null);
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
  public static <T extends ExtendableMessage<?>, E extends Message> Extension<T, List<E>>
      getRepeatedMessageExtension(Class<T> extendedType, int tag, Class<E> messageType) {
    return new Extension<T, List<E>>(extendedType, tag, Datatype.MESSAGE, Label.REPEATED,
        messageType, null);
  }

  /**
   * Returns an {@link Extension} instance for an enum datatype.
   *
   * @param extendedType the type of message being extended
   * @param tag the tag number of the extension
   * @param label one of {@code Label#OPTIONAL} or {@code Label#REQUIRED}
   * @param enumType the class type of the {@link Extension}'s enum value
   * @param <T> the type of message being extended
   * @param <E> the Java data type of the {@link Extension} enum value
   */
  public static <T extends ExtendableMessage<?>, E extends Enum> Extension<T, E>
      getEnumExtension(Class<T> extendedType, int tag, Label label, Class<E> enumType) {
    return new Extension<T, E>(extendedType, tag, Datatype.ENUM, label, null, enumType);
  }

  /**
   * Returns an {@link Extension} instance for a repeated enum datatype.
   *
   * @param extendedType the type of message being extended
   * @param tag the tag number of the extension
   * @param packed true if the '[packed = true]' extension is present
   * @param enumType the class type of the {@link Extension}'s enum value
   * @param <T> the type of message being extended
   * @param <E> the Java data type of the {@link Extension} enum value
   */
  public static <T extends ExtendableMessage<?>, E extends Enum> Extension<T, List<E>>
      getRepeatedEnumExtension(Class<T> extendedType, int tag, boolean packed,
      Class<E> enumType) {
    return new Extension<T, List<E>>(extendedType, tag, Datatype.ENUM,
        packed ? Label.PACKED : Label.REPEATED, null, enumType);
  }

  private Extension(Class<T> extendedType, int tag, Datatype datatype, Label label,
      Class<? extends Message> messageType, Class<? extends Enum> enumType) {
    this.extendedType = extendedType;
    this.tag = tag;
    this.datatype = datatype;
    this.label = label;
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
    if (datatype != o.datatype) {
      return datatype.value() - o.datatype.value();
    }
    if (label != o.label) {
      return label.value() - o.label.value();
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
    hash = hash * 37 + datatype.value();
    hash = hash * 37 + label.value();
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

  public Datatype getDatatype() {
    return datatype;
  }

  public Label getLabel() {
    return label;
  }
}
