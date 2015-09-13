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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Superclass for protocol buffer messages.
 */
public abstract class Message<T extends Message<T>> implements Serializable {
  private static final long serialVersionUID = 0L;

  /** A protocol buffer label. */
  public enum Label {
    REQUIRED, OPTIONAL, REPEATED, ONE_OF,
    /** Implies {@link #REPEATED}. */
    PACKED;

    boolean isRepeated() {
      return this == REPEATED || this == PACKED;
    }

    boolean isPacked() {
      return this == PACKED;
    }

    boolean isOneOf() {
      return this == ONE_OF;
    }
  }

  /** Set to null until a field is added. */
  transient TagMap tagMap;

  /** If not {@code 0} then the serialized size of this message. */
  transient int cachedSerializedSize = 0;

  /** If non-zero, the hash code of this message. Accessed by generated code. */
  protected transient int hashCode = 0;

  protected Message() {
  }

  /**
   * Initializes any unknown field data to that stored in the given {@code Builder}.
   */
  protected void setBuilder(Builder builder) {
    if (builder.tagMapBuilder != null) {
      tagMap = builder.tagMapBuilder.build();
    }
  }

  // Increase visibility for testing
  TagMap tagMap() {
    return tagMap;
  }

  /** Utility method to return a mutable copy of a given List. Used by generated code. */
  protected static <T> List<T> copyOf(List<T> list) {
    if (list == null) {
      throw new NullPointerException("list == null");
    }
    return new ArrayList<>(list);
  }

  /** Utility method to return an immutable copy of a given List. Used by generated code. */
  protected static <T> List<T> immutableCopyOf(List<T> list) {
    if (list == null) {
      throw new NullPointerException("list == null");
    }
    if (list == Collections.emptyList() || list instanceof ImmutableList) {
      return list;
    }
    return Collections.unmodifiableList(new ArrayList<>(list));
  }

  /**
   * Returns the enumerated value tagged with the given integer value for the
   * given enum class. If no enum value in the given class is initialized
   * with the given integer tag value, an exception will be thrown.
   *
   * @param <E> the enum class type
   */
  public static <E extends Enum & WireEnum> E enumFromInt(Class<E> enumClass, int value) {
    RuntimeEnumAdapter<E> adapter = new RuntimeEnumAdapter<>(enumClass);
    return adapter.fromInt(value);
  }

  int tagMapEncodedSize() {
    return tagMap == null ? 0 : tagMap.encodedSize();
  }

  protected static boolean equals(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  /**
   * Returns an immutable list of the extensions on this message in tag order.
   */
  public Set<Extension<?, ?>> getExtensions() {
    return tagMap != null
        ? tagMap.extensions(false)
        : Collections.<Extension<?, ?>>emptySet();
  }

  /**
   * Returns the value for {@code extension} on this message, or null if no
   * value is set.
   */
  public <E> E getExtension(Extension<T, E> extension) {
    return tagMap != null ? (E) tagMap.get(extension) : null;
  }

  /**
   * Returns true if the extensions on this message equals the extensions of
   * {@code other}.
   */
  protected boolean extensionsEqual(Message<T> other) {
    return tagMap != null
        ? tagMap.equals(other.tagMap)
        : other.tagMap == null;
  }

  /**
   * Returns a hash code for the extensions on this message.
   */
  protected int extensionsHashCode() {
    return tagMap != null ? tagMap.hashCode() : 0;
  }

  @SuppressWarnings("unchecked")
  @Override public String toString() {
    return RuntimeMessageAdapter.create((Class<Message>) getClass()).toString(this);
  }

  private Object writeReplace() throws ObjectStreamException {
    return new MessageSerializedForm(this, getClass());
  }

  /**
   * Superclass for protocol buffer message builders.
   */
  public abstract static class Builder<T extends Message<T>, B extends Builder<T, B>> {

    TagMap.Builder tagMapBuilder;

    /**
     * Constructs a Builder with no unknown field data.
     */
    public Builder() {
    }

    /**
     * Constructs a Builder with unknown field data initialized to a copy of any unknown
     * field data in the given {@link Message}.
     */
    public Builder(Message message) {
      if (message != null && message.tagMap != null) {
        this.tagMapBuilder = new TagMap.Builder(message.tagMap);
      }
    }

    TagMap.Builder ensureTagMap() {
      if (tagMapBuilder == null) {
        tagMapBuilder = new TagMap.Builder();
      }
      return tagMapBuilder;
    }

    /**
     * Create an exception for missing required fields.
     *
     * @param args Alternating field value and field name pairs.
     */
    protected IllegalStateException missingRequiredFields(Object... args) {
      StringBuilder sb = new StringBuilder();
      String plural = "";
      for (int i = 0, size = args.length; i < size; i += 2) {
        if (args[i] == null) {
          if (sb.length() > 0) {
            plural = "s"; // Found more than one missing field
          }
          sb.append("\n  ");
          sb.append(args[i + 1]);
        }
      }
      throw new IllegalStateException("Required field" + plural + " not set:" + sb);
    }

    /**
     * If {@code list} is null it will be replaced with {@link Collections#emptyList()}.
     * Otherwise look for null items and throw {@link NullPointerException} if one is found.
     */
    protected static <T> List<T> canonicalizeList(List<T> list) {
      if (list == null) {
        throw new NullPointerException("list == null");
      }
      for (int i = 0, size = list.size(); i < size; i++) {
        T element = list.get(i);
        if (element == null) {
          throw new NullPointerException("Element at index " + i + " is null");
        }
      }
      return list;
    }

    /**
     * Returns the value for {@code extension} on this message, or null if no
     * value is set.
     */
    public <E> E getExtension(Extension<T, E> extension) {
      return tagMapBuilder != null ? (E) tagMapBuilder.get(extension) : null;
    }

    /**
     * Sets the value of {@code extension} on this builder to {@code value}.
     */
    public <E> B setExtension(Extension<T, E> extension, E value) {
      if (tagMapBuilder == null) {
        tagMapBuilder = new TagMap.Builder();
      } else {
        tagMapBuilder.removeAll(extension.getTag());
      }
      if (value instanceof List) {
        for (Object o : (List) value) {
          tagMapBuilder.add(extension, o);
        }
      } else {
        tagMapBuilder.add(extension, value);
      }
      return (B) this;
    }

    /**
     * Returns an immutable {@link com.squareup.wire.Message} based on the fields that have been set
     * in this builder.
     */
    public abstract T build();
  }
}
