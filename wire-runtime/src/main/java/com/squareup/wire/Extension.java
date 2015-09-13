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

import static com.squareup.wire.Preconditions.checkNotNull;
import static com.squareup.wire.WireField.Label;

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
  private final Class<T> extendedType;
  private final Label label;
  private final String name;
  private final int tag;

  private final String adapterString;
  private ProtoAdapter<?> adapter;

  private Extension(Class<T> extendedType, Label label, String name, int tag,
      ProtoAdapter<?> adapter, String adapterString) {
    this.extendedType = extendedType;
    this.adapter = adapter;
    this.adapterString = adapterString;
    this.name = name;
    this.tag = tag;
    this.label = label;
  }

  public static <T extends Message<T>, E> Extension<T, E> get(
      Class<T> extendedType, Label label, String name, int tag, String adapter) {
    checkNotNull(extendedType, "extendedType == null");
    checkNotNull(adapter, "adapter == null");
    checkNotNull(name, "name == null");
    checkNotNull(label, "label == null");
    return new Extension<>(extendedType, label, name, tag, null, adapter);
  }

  /**
   * Returns an extension that represents an unknown value. This occurs when the decoder was
   * prepared with an older schema (if a field was added), or if an extension is not registered.
   */
  public static <T extends Message<T>, E> Extension<T, E> unknown(
      Class<T> messageType, int tag, FieldEncoding fieldEncoding) {
    return new Extension<>(messageType, Label.REPEATED, null, tag, fieldEncoding.rawProtoAdapter(),
        null);
  }

  public boolean isUnknown() {
    return name == null;
  }

  public ProtoAdapter<?> getAdapter() {
    ProtoAdapter<?> result = adapter;
    return result != null ? result : (adapter = ProtoAdapter.get(adapterString));
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
    hash = hash * 37 + getAdapter().hashCode();
    hash = hash * 37 + label.hashCode();
    hash = hash * 37 + extendedType.hashCode();
    return hash;
  }

  @Override public String toString() {
    return isUnknown()
        ? String.format("[UNKNOWN %s = %d]", getAdapter().javaType, tag)
        : String.format("[%s %s %s = %d]", label, getAdapter().javaType.getName(), name, tag);
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

  public Label getLabel() {
    return label;
  }
}
