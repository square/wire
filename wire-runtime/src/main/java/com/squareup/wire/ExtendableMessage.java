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

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A message that declares an extension range.
 *
 * @param <T> the message type being extended.
 */
public abstract class ExtendableMessage<T extends ExtendableMessage<T>> extends Message {
  private static final long serialVersionUID = 0L;

  protected ExtendableMessage() {
  }

  /**
   * Initializes any extension and unknown field data to that stored in the given {@code Builder}.
   */
  protected void setBuilder(ExtendableBuilder<T, ?> builder) {
    super.setBuilder(builder);
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
  protected boolean extensionsEqual(ExtendableMessage<T> other) {
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

  /**
   * Builds a message that declares an extension range.
   */
  public abstract static class ExtendableBuilder<T extends ExtendableMessage<T>,
      B extends ExtendableBuilder<T, B>> extends Builder<T> {

    @SuppressWarnings("unchecked")
    private final B self;

    protected ExtendableBuilder(Class<B> selfType) {
      this(selfType, null);
    }

    protected ExtendableBuilder(Class<B> selfType, ExtendableMessage<T> message) {
      super(message);
      self = selfType.cast(this);
    }

    /**
     * Returns the value for {@code extension} on this message, or null if no
     * value is set.
     */
    public <E> E getExtension(Extension<T, E> extension) {
      return tagMap != null ? (E) tagMap.get(extension) : null;
    }

    /**
     * Sets the value of {@code extension} on this builder to {@code value}.
     */
    public <E> B setExtension(Extension<T, E> extension, E value) {
      if (tagMap == null) {
        tagMap = new NewTagMap();
      } else {
        tagMap.removeAll(extension.getTag());
      }
      if (value instanceof List) {
        for (Object o : (List) value) {
          tagMap.add(extension, o);
        }
      } else {
        tagMap.add(extension, value);
      }
      return self;
    }
  }
}
