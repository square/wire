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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** A protocol buffer message. */
public abstract class Message<T extends Message<T>> implements Serializable {
  private static final long serialVersionUID = 0L;

  final transient TagMap tagMap;

  /** If not {@code 0} then the serialized size of this message. */
  transient int cachedSerializedSize = 0;

  /** If non-zero, the hash code of this message. Accessed by generated code. */
  protected transient int hashCode = 0;

  protected Message(TagMap tagMap) {
    if (tagMap == null) {
      throw new NullPointerException("tagMap == null");
    }
    this.tagMap = tagMap;
  }

  public final TagMap tagMap() {
    return tagMap;
  }

  /**
   * Returns an immutable list of the extensions on this message in tag order.
   */
  public final Set<Extension<?, ?>> getExtensions() {
    return tagMap.extensions(false);
  }

  /**
   * Returns the value for {@code extension} on this message, or null if no
   * value is set.
   */
  public final <E> E getExtension(Extension<T, E> extension) {
    return (E) tagMap.get(extension);
  }

  @SuppressWarnings("unchecked")
  @Override public String toString() {
    return ProtoAdapter.get((Class<Message>) getClass()).toString(this);
  }

  protected final Object writeReplace() throws ObjectStreamException {
    return new MessageSerializedForm<>(this, (Class<Message>) getClass());
  }

  /** Returns a new builder for this class, initialized with the data in this message. */
  public final <B extends Builder<T, B>> B newBuilder() {
    Class<T> messageType = (Class) getClass();
    try {
      Class<B> builderType = RuntimeMessageAdapter.getBuilderType(messageType);
      Constructor<B> constructor = RuntimeMessageAdapter.getBuilderCopyConstructor(
          builderType, messageType);
      return constructor.newInstance(this);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Failed to create builder for " + messageType);
    }
  }

  /** Returns a copy of this message with unknown fields removed. */
  public final T withoutUnknownFields() {
    TagMap.Builder tagMapBuilder = null;
    for (Extension<?, ?> extension : tagMap.extensions(true)) {
      if (!extension.isUnknown()) continue;
      if (tagMapBuilder == null) {
        tagMapBuilder = new TagMap.Builder(tagMap);
      }
      tagMapBuilder.removeAll(extension.getTag());
    }

    if (tagMapBuilder == null) {
      return (T) this; // No unknown fields were removed.
    }

    Builder<T, ?> builder = ((Message) this).newBuilder();
    builder.tagMapBuilder = tagMapBuilder;
    return builder.build();
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
      if (message != null && message.tagMap != TagMap.EMPTY) {
        this.tagMapBuilder = new TagMap.Builder(message.tagMap);
      }
    }

    /** The {@link TagMap} builder in which unknown fields and extensions are stored. */
    public TagMap.Builder tagMap() {
      if (tagMapBuilder == null) {
        tagMapBuilder = new TagMap.Builder();
      }
      return tagMapBuilder;
    }

    /**
     * Returns the value for {@code extension} on this message, or null if no
     * value is set.
     */
    public final <E> E getExtension(Extension<T, E> extension) {
      return tagMapBuilder != null ? (E) tagMapBuilder.get(extension) : null;
    }

    /**
     * Sets the value of {@code extension} on this builder to {@code value}.
     */
    public final <E> B setExtension(Extension<T, E> extension, E value) {
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
     * Returns an immutable {@link TagMap} based on the unknown fields and extensions set in this
     * builder, or null.
     */
    public TagMap buildTagMap() {
      return tagMapBuilder != null ? tagMapBuilder.build() : TagMap.EMPTY;
    }

    /** Returns an immutable {@link Message} based on the fields that set in this builder. */
    public abstract T build();
  }

  /** <b>For generated code only.</b> */
  protected static <T> List<T> newMutableList() {
    return new MutableOnWriteList<>(Collections.<T>emptyList());
  }

  /** <b>For generated code only.</b> Utility method to return a mutable copy of {@code list}. */
  protected static <T> List<T> copyOf(List<T> list) {
    if (list == null) throw new NullPointerException("list == null");
    if (list == Collections.emptyList() || list instanceof ImmutableList) {
      return new MutableOnWriteList<>(list);
    }
    return new ArrayList<>(list);
  }

  /** <b>For generated code only.</b> Utility method to return an immutable copy of {@code list}. */
  protected static <T> List<T> immutableCopyOf(List<T> list) {
    if (list == null) throw new NullPointerException("list == null");
    if (list instanceof MutableOnWriteList) {
      list = ((MutableOnWriteList<T>) list).mutableList;
    }
    if (list == Collections.emptyList() || list instanceof ImmutableList) {
      return list;
    }
    return new ImmutableList<>(list);
  }

  /** <b>For generated code only.</b> */
  protected static <T> void redactElements(List<T> list, ProtoAdapter<T> adapter) {
    for (int i = 0, count = list.size(); i < count; i++) {
      list.set(i, adapter.redact(list.get(i)));
    }
  }

  /** <b>For generated code only.</b> */
  protected static boolean equals(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  /**
   * <b>For generated code only.</b> Create an exception for missing required fields.
   *
   * @param args Alternating field value and field name pairs.
   */
  protected static IllegalStateException missingRequiredFields(Object... args) {
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
   * <b>For generated code only.</b> Throw {@link NullPointerException} if {@code list} or one of
   * its items are null.
   */
  protected static void checkElementsNotNull(List<?> list) {
    if (list == null) throw new NullPointerException("list == null");
    for (int i = 0, size = list.size(); i < size; i++) {
      Object element = list.get(i);
      if (element == null) {
        throw new NullPointerException("Element at index " + i + " is null");
      }
    }
  }
}
