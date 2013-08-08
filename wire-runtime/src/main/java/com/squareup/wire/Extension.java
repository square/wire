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
 * associated with a particular {@link com.squareup.wire.ExtendableMessage} type being extended.
 *
 * @param <T> the type of message being extended
 * @param <E> the (boxed) Java data type of the extension value
 */
public final class Extension<T extends ExtendableMessage<?>, E>
    implements Comparable<Extension<?, ?>> {

  public static final class Builder<T extends ExtendableMessage<?>, E> {
    private final Class<T> extendedType;
    private final Class<? extends Message> messageType;
    private final Class<? extends Enum> enumType;
    private final Datatype datatype;
    private String name = null;
    private int tag = -1;
    private Label label = null;

    private Builder(Class<T> extendedType, Datatype datatype) {
      this.extendedType = extendedType;
      this.messageType = null;
      this.enumType = null;
      this.datatype = datatype;
    }

    private Builder(Class<T> extendedType, Class<? extends Message> messageType,
        Class<? extends Enum> enumType, Datatype datatype) {
      this.extendedType = extendedType;
      this.messageType = messageType;
      this.enumType = enumType;
      this.datatype = datatype;
    }

    public Builder<T, E> setName(String name) {
      this.name = name;
      return this;
    }

    public Builder<T, E> setTag(int tag) {
      this.tag = tag;
      return this;
    }

    public Extension<T, E> buildOptional() {
      this.label = Label.OPTIONAL;
      validate();
      return new Extension<T, E>(extendedType, messageType, enumType, name, tag, label, datatype);
    }

    public Extension<T, E> buildRequired() {
      this.label = Label.REQUIRED;
      validate();
      return new Extension<T, E>(extendedType, messageType, enumType, name, tag, label, datatype);
    }

    public Extension<T, List<E>> buildRepeated() {
      this.label = Label.REPEATED;
      validate();
      return new Extension<T, List<E>>(extendedType, messageType, enumType, name, tag, label,
          datatype);
    }

    public Extension<T, List<E>> buildPacked() {
      this.label = Label.PACKED;
      validate();
      return new Extension<T, List<E>>(extendedType, messageType, enumType, name, tag, label,
          datatype);
    }

    private void validate() {
      if (extendedType == null) {
        throw new IllegalArgumentException("extendedType == null");
      }
      if (name == null) {
        throw new IllegalArgumentException("name == null");
      }
      if (datatype == null) {
        throw new IllegalArgumentException("datatype == null");
      }
      if (label == null) {
        throw new IllegalArgumentException("label == null");
      }
      if (tag <= 0) {
        throw new IllegalArgumentException("tag == " + tag);
      }
      if (datatype == Datatype.MESSAGE) {
        if (messageType == null || enumType != null) {
          throw new IllegalStateException("Message w/o messageType or w/ enumType");
        }
      } else if (datatype == Datatype.ENUM) {
        if (messageType != null || enumType == null) {
          throw new IllegalStateException("Enum w/ messageType or w/o enumType");
        }
      } else {
        if (messageType != null || enumType != null) {
          throw new IllegalStateException("Scalar w/ messageType or enumType");
        }
      }
    }
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Integer> int32Extending(
      Class<T> extendedType) {
    return new Builder<T, Integer>(extendedType, Datatype.INT32);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Integer> sint32Extending(
      Class<T> extendedType) {
    return new Builder<T, Integer>(extendedType, Datatype.SINT32);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Integer> uint32Extending(
      Class<T> extendedType) {
    return new Builder<T, Integer>(extendedType, Datatype.UINT32);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Integer> fixed32Extending(
      Class<T> extendedType) {
    return new Builder<T, Integer>(extendedType, Datatype.FIXED32);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Integer> sfixed32Extending(
      Class<T> extendedType) {
    return new Builder<T, Integer>(extendedType, Datatype.SFIXED32);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Long> int64Extending(
      Class<T> extendedType) {
    return new Builder<T, Long>(extendedType, Datatype.INT64);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Long> sint64Extending(
      Class<T> extendedType) {
    return new Builder<T, Long>(extendedType, Datatype.SINT64);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Long> uint64Extending(
      Class<T> extendedType) {
    return new Builder<T, Long>(extendedType, Datatype.UINT64);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Long> fixed64Extending(
      Class<T> extendedType) {
    return new Builder<T, Long>(extendedType, Datatype.FIXED64);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Long> sfixed64Extending(
      Class<T> extendedType) {
    return new Builder<T, Long>(extendedType, Datatype.SFIXED64);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Boolean> boolExtending(
      Class<T> extendedType) {
    return new Builder<T, Boolean>(extendedType, Datatype.BOOL);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, String> stringExtending(
      Class<T> extendedType) {
    return new Builder<T, String>(extendedType, Datatype.STRING);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, ByteString> bytesExtending(
      Class<T> extendedType) {
    return new Builder<T, ByteString>(extendedType, Datatype.BYTES);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Float> floatExtending(
      Class<T> extendedType) {
    return new Builder<T, Float>(extendedType, Datatype.FLOAT);
  }

  public static <T extends ExtendableMessage<?>> Builder<T, Double> doubleExtending(
      Class<T> extendedType) {
    return new Builder<T, Double>(extendedType, Datatype.DOUBLE);
  }

  public static <T extends ExtendableMessage<?>, E extends Enum> Builder<T, E> enumExtending(
      Class<E> enumType, Class<T> extendedType) {
    return new Builder<T, E>(extendedType, null, enumType, Datatype.ENUM);
  }

  public static <T extends ExtendableMessage<?>, M extends Message> Builder<T, M> messageExtending(
      Class<M> messageType, Class<T> extendedType) {
    return new Builder<T, M>(extendedType, messageType, null, Datatype.MESSAGE);
  }

  private final Class<T> extendedType;
  private final Class<? extends Message> messageType;
  private final Class<? extends Enum> enumType;
  private final String name;
  private final int tag;
  private final Datatype datatype;
  private final Label label;

  private Extension(Class<T> extendedType, Class<? extends Message> messageType,
      Class<? extends Enum> enumType, String name, int tag, Label label, Datatype datatype) {
    this.extendedType = extendedType;
    this.name = name;
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

  public String getName() {
    return name;
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
