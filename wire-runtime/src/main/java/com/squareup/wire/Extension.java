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
import okio.ByteString;

import static com.squareup.wire.Message.Label;

/**
 * An extended attribute of on a protocol buffer message. Extensions are used to
 * declare additional fields for a message beyond the message's original
 * declaration. For example, this declaration defines two extended attributes
 * for a {@code MenuItem} message: <pre>   {@code
 *
 *   extend MenuItem {
 *     optional int32 calorie_count = 10001;
 *     optional int32 fat_grams = 10002;
 *   }
 * }</pre>
 *
 * <p>An extension instance is a key; it names the attribute. Use it to set a
 * value on a message's builder, or to get the value on a message. <pre>   {@code
 *
 *   MenuItem sandwich = new MenuItem.Builder()
 *       .setBread(Bread.SOURDOUGH)
 *       .setExtension(Ext_nutrition.calorie_count, 300)
 *       .setExtension(Ext_nutrition.fat_grams, 6)
 *       .build();
 *   int calorieCount = sandwich.getExtension(Ext_nutrition.calorie_count));
 *   int fatGrams = sandwich.getExtension(Ext_nutrition.fat_grams));
 * }</pre>
 *
 * <p>Application code shouldn't create extension instances directly; instead
 * they should use the generated instances created with {@code Ext_} prefixes.
 * To serialize and deserialize extensions, specify all of your {@code Ext_}
 * classes when creating a {@link Wire} instance.
 *
 * @param <T> the type of message being extended
 * @param <E> the (boxed) Java data type of the extension value
 */
public final class Extension<T extends Message<T>, E> implements Comparable<Extension<?, ?>> {

  public static final class Builder<T extends Message<T>, E> {
    private final Class<T> extendedType;
    private final Class<? extends Message> messageType;
    private final Class<? extends WireEnum> enumType;
    private final ProtoType type;
    private String name = null;
    private int tag = -1;
    private Label label = null;

    private Builder(Class<T> extendedType, ProtoType type) {
      this.extendedType = extendedType;
      this.messageType = null;
      this.enumType = null;
      this.type = type;
    }

    private Builder(Class<T> extendedType, Class<? extends Message> messageType,
        Class<? extends WireEnum> enumType, ProtoType type) {
      this.extendedType = extendedType;
      this.messageType = messageType;
      this.enumType = enumType;
      this.type = type;
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
      return new Extension<>(extendedType, messageType, enumType, name, tag, label, type);
    }

    public Extension<T, List<E>> buildRepeated() {
      this.label = Label.REPEATED;
      validate();
      return new Extension<>(extendedType, messageType, enumType, name, tag, label, type);
    }

    public Extension<T, List<E>> buildPacked() {
      this.label = Label.PACKED;
      validate();
      return new Extension<>(extendedType, messageType, enumType, name, tag, label, type);
    }

    private void validate() {
      if (extendedType == null) {
        throw new IllegalArgumentException("extendedType == null");
      }
      if (name == null) {
        throw new IllegalArgumentException("name == null");
      }
      if (type == null) {
        throw new IllegalArgumentException("type == null");
      }
      if (label == null) {
        throw new IllegalArgumentException("label == null");
      }
      if (tag <= 0) {
        throw new IllegalArgumentException("tag == " + tag);
      }
      if (!(type.isScalar() ^ messageType != null ^ enumType != null)) {
        throw new IllegalStateException("type must be a scalar, enum, or message");
      }
    }
  }

  public static <T extends Message<T>> Builder<T, Integer> int32Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.INT32);
  }

  public static <T extends Message<T>> Builder<T, Integer> sint32Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.SINT32);
  }

  public static <T extends Message<T>> Builder<T, Integer> uint32Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.UINT32);
  }

  public static <T extends Message<T>> Builder<T, Integer> fixed32Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.FIXED32);
  }

  public static <T extends Message<T>> Builder<T, Integer> sfixed32Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.SFIXED32);
  }

  public static <T extends Message<T>> Builder<T, Long> int64Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.INT64);
  }

  public static <T extends Message<T>> Builder<T, Long> sint64Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.SINT64);
  }

  public static <T extends Message<T>> Builder<T, Long> uint64Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.UINT64);
  }

  public static <T extends Message<T>> Builder<T, Long> fixed64Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.FIXED64);
  }

  public static <T extends Message<T>> Builder<T, Long> sfixed64Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.SFIXED64);
  }

  public static <T extends Message<T>> Builder<T, Boolean> boolExtending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.BOOL);
  }

  public static <T extends Message<T>> Builder<T, String> stringExtending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.STRING);
  }

  public static <T extends Message<T>> Builder<T, ByteString> bytesExtending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.BYTES);
  }

  public static <T extends Message<T>> Builder<T, Float> floatExtending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.FLOAT);
  }

  public static <T extends Message<T>> Builder<T, Double> doubleExtending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ProtoType.DOUBLE);
  }

  public static <T extends Message<T>, E extends Enum & WireEnum> Builder<T, E> //
  enumExtending(String type, Class<E> enumType, Class<T> extendedType) {
    return new Builder<>(extendedType, null, enumType, ProtoType.get(type));
  }

  public static <T extends Message<T>, M extends Message> Builder<T, M> messageExtending(
      String type, Class<M> messageType, Class<T> extendedType) {
    return new Builder<>(extendedType, messageType, null, ProtoType.get(type));
  }

  private final Class<T> extendedType;
  private final Class<? extends Message> messageType;
  private final Class<? extends WireEnum> enumType;
  private final String name;
  private final int tag;
  private final ProtoType type;
  private final Label label;

  private Extension(Class<T> extendedType, Class<? extends Message> messageType,
      Class<? extends WireEnum> enumType, String name, int tag, Label label, ProtoType type) {
    this.extendedType = extendedType;
    this.name = name;
    this.tag = tag;
    this.type = type;
    this.label = label;
    this.messageType = messageType;
    this.enumType = enumType;
  }

  /**
   * Returns an extension that represents an unknown value. This occurs when the decoder was
   * prepared with an older schema (if a field was added), or if an extension is not registered.
   */
  public static <T> Extension<?, T> unknown(int tag, FieldEncoding fieldEncoding) {
    return new Extension<>(Message.class, null, null, null, tag, Label.REPEATED,
        fieldEncoding.protoType());
  }

  public boolean isUnknown() {
    return extendedType == (Class<?>) Message.class;
  }

  /**
   * Orders Extensions in ascending tag order.
   */
  @Override public int compareTo(Extension<?, ?> o) {
    return tag - o.tag;
  }

  @Override public boolean equals(Object other) {
    return other instanceof Extension<?, ?> && compareTo((Extension<?, ?>) other) == 0;
  }

  @Override public int hashCode() {
    int hash = tag;
    hash = hash * 37 + type.hashCode();
    hash = hash * 37 + label.hashCode();
    hash = hash * 37 + extendedType.hashCode();
    hash = hash * 37 + (messageType != null ? messageType.hashCode() : 0);
    hash = hash * 37 + (enumType != null ? enumType.hashCode() : 0);
    return hash;
  }

  @Override public String toString() {
    return isUnknown()
        ? String.format("[UNKNOWN %s = %d]", type, tag)
        : String.format("[%s %s %s = %d]", label, type, name, tag);
  }

  public Class<T> getExtendedType() {
    return extendedType;
  }

  public Class<? extends Message> getMessageType() {
    return messageType;
  }

  public Class<? extends WireEnum> getEnumType() {
    return enumType;
  }

  public String getName() {
    return name;
  }

  public int getTag() {
    return tag;
  }

  public ProtoType getType() {
    return type;
  }

  public Label getLabel() {
    return label;
  }
}
