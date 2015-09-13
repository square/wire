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
 * classes when creating an {@linkplain ExtensionRegistry extension registry}.
 *
 * @param <T> the type of message being extended
 * @param <E> the (boxed) Java data type of the extension value
 */
public final class Extension<T extends Message<T>, E> implements Comparable<Extension<?, ?>> {

  public static final class Builder<T extends Message<T>, E> {
    private final Class<T> extendedType;
    private final Class<?> javaType;
    private final ProtoType protoType;
    private String name = null;
    private int tag = -1;
    private Label label = null;

    private Builder(Class<T> extendedType, Class<?> javaType, ProtoType protoType) {
      if (extendedType == null) throw new IllegalArgumentException("extendedType == null");
      if (javaType == null) throw new IllegalArgumentException("javaType == null");
      if (protoType == null) throw new IllegalArgumentException("type == null");
      this.extendedType = extendedType;
      this.javaType = javaType;
      this.protoType = protoType;
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
      return new Extension<>(extendedType, javaType, protoType, name, tag, label);
    }

    public Extension<T, List<E>> buildRepeated() {
      this.label = Label.REPEATED;
      validate();
      return new Extension<>(extendedType, javaType, protoType, name, tag, label);
    }

    public Extension<T, List<E>> buildPacked() {
      this.label = Label.PACKED;
      validate();
      return new Extension<>(extendedType, javaType, protoType, name, tag, label);
    }

    private void validate() {
      if (name == null) {
        throw new IllegalArgumentException("name == null");
      }
      if (label == null) {
        throw new IllegalArgumentException("label == null");
      }
      if (tag <= 0) {
        throw new IllegalArgumentException("tag == " + tag);
      }
    }
  }

  public static <T extends Message<T>> Builder<T, Integer> int32Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Integer.class, ProtoType.INT32);
  }

  public static <T extends Message<T>> Builder<T, Integer> sint32Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Integer.class, ProtoType.SINT32);
  }

  public static <T extends Message<T>> Builder<T, Integer> uint32Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Integer.class, ProtoType.UINT32);
  }

  public static <T extends Message<T>> Builder<T, Integer> fixed32Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Integer.class, ProtoType.FIXED32);
  }

  public static <T extends Message<T>> Builder<T, Integer> sfixed32Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Integer.class, ProtoType.SFIXED32);
  }

  public static <T extends Message<T>> Builder<T, Long> int64Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Long.class, ProtoType.INT64);
  }

  public static <T extends Message<T>> Builder<T, Long> sint64Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Long.class, ProtoType.SINT64);
  }

  public static <T extends Message<T>> Builder<T, Long> uint64Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Long.class, ProtoType.UINT64);
  }

  public static <T extends Message<T>> Builder<T, Long> fixed64Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Long.class, ProtoType.FIXED64);
  }

  public static <T extends Message<T>> Builder<T, Long> sfixed64Extending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Long.class, ProtoType.SFIXED64);
  }

  public static <T extends Message<T>> Builder<T, Boolean> boolExtending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Boolean.class, ProtoType.BOOL);
  }

  public static <T extends Message<T>> Builder<T, String> stringExtending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, String.class, ProtoType.STRING);
  }

  public static <T extends Message<T>> Builder<T, ByteString> bytesExtending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, ByteString.class, ProtoType.BYTES);
  }

  public static <T extends Message<T>> Builder<T, Float> floatExtending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Float.class, ProtoType.FLOAT);
  }

  public static <T extends Message<T>> Builder<T, Double> doubleExtending(
      Class<T> extendedType) {
    return new Builder<>(extendedType, Double.class, ProtoType.DOUBLE);
  }

  public static <T extends Message<T>, E extends Enum & WireEnum> Builder<T, E> //
  enumExtending(String type, Class<E> enumType, Class<T> extendedType) {
    return new Builder<>(extendedType, enumType, ProtoType.get(type));
  }

  public static <T extends Message<T>, M extends Message> Builder<T, M> messageExtending(
      String type, Class<M> messageType, Class<T> extendedType) {
    return new Builder<>(extendedType, messageType, ProtoType.get(type));
  }

  private final Class<T> extendedType;
  private final Class<?> javaType;
  private final ProtoType protoType;
  private final String name;
  private final int tag;
  private final Label label;

  private Extension(Class<T> extendedType, Class<?> javaType, ProtoType protoType,
      String name, int tag, Label label) {
    this.extendedType = extendedType;
    this.javaType = javaType;
    this.name = name;
    this.tag = tag;
    this.protoType = protoType;
    this.label = label;
  }

  /**
   * Returns an extension that represents an unknown value. This occurs when the decoder was
   * prepared with an older schema (if a field was added), or if an extension is not registered.
   */
  public static <T extends Message<T>, E> Extension<T, E> unknown(
      Class<T> messageType, int tag, ProtoEncoding protoEncoding) {
    return new Extension<>(messageType, protoEncoding.javaType(), protoEncoding.protoType(),
        null, tag, Label.REPEATED);
  }

  public boolean isUnknown() {
    return name == null;
  }

  public ProtoAdapter<?> getAdapter() {
    return ProtoAdapter.get(protoType, javaType);
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
    hash = hash * 37 + protoType.hashCode();
    hash = hash * 37 + label.hashCode();
    hash = hash * 37 + extendedType.hashCode();
    return hash;
  }

  @Override public String toString() {
    return isUnknown()
        ? String.format("[UNKNOWN %s = %d]", protoType, tag)
        : String.format("[%s %s %s = %d]", label, protoType, name, tag);
  }

  public Class<T> getExtendedType() {
    return extendedType;
  }

  public String getName() {
    return name;
  }

  public int getTag() {
    return tag;
  }

  public ProtoType getProtoType() {
    return protoType;
  }

  public Class<?> getJavaType() {
    return javaType;
  }

  public Label getLabel() {
    return label;
  }
}
