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

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import okio.ByteString;

import static com.squareup.wire.ProtoWriter.tagSize;
import static com.squareup.wire.TypeAdapter.TYPE_LEN_DELIMITED;
import static com.squareup.wire.TypeAdapter.varint32Size;

/**
 * Superclass for protocol buffer messages.
 */
public abstract class Message<M extends Message<M>> implements Serializable {
  private static final long serialVersionUID = 0L;

  /** Set to null until a field is added. */
  protected transient UnknownFieldMap unknownFields;

  /** If not {@code -1} then the serialized size of this message. */
  protected transient int serializedSize = -1;

  /** If non-zero, the hash code of this message. Accessed by generated code. */
  protected transient int hashCode = 0;

  private transient final String name;

  protected Message(String name) {
    if (name == null) throw new NullPointerException("name == null");
    this.name = name;
  }

  /**
   * Initializes any unknown field data to that stored in the given {@code Builder}.
   */
  protected void setBuilder(Builder builder) {
    if (builder.unknownFieldMap != null) {
      unknownFields = new UnknownFieldMap(builder.unknownFieldMap);
    }
  }

  // Increase visibility for testing
  protected Collection<List<UnknownFieldMap.Value<?>>> unknownFields() {
    return unknownFields == null ? Collections.<List<UnknownFieldMap.Value<?>>>emptySet()
        : unknownFields.fieldMap.values();
  }

  /** Utility method to return a mutable copy of a given List. Used by generated code. */
  protected static <T> List<T> copyOf(List<T> list) {
    if (list == null) {
      throw new NullPointerException("list == null");
    }
    if (list == Collections.emptyList()) {
      return list;
    }
    return new ArrayList<T>(list);
  }

  /** Utility method to return an immutable copy of a given List. Used by generated code. */
  protected static <T> List<T> immutableCopyOf(List<T> list) {
    if (list == null) {
      throw new NullPointerException("list == null");
    }
    if (list == Collections.emptyList()) {
      return list;
    }
    return Collections.unmodifiableList(new ArrayList<T>(list));
  }

  protected abstract void visitFields(Visitor visitor);

  final int serializedSize() {
    int serializedSize = this.serializedSize;
    if (serializedSize == -1) {
      SerializedSizeVisitor visitor = new SerializedSizeVisitor();
      visitFields(visitor);
      serializedSize = this.serializedSize = visitor.size;
    }
    return serializedSize;
  }

  final void write(ProtoWriter writer) throws IOException {
    try {
      visitFields(writer.visitor);
    } catch (WriteIOException e) {
      throw e.cause;
    }
  }

  @Override public final String toString() {
    ToStringVisitor visitor = new ToStringVisitor();
    visitFields(visitor);
    return visitor.finish(this);
  }

  @Override public final int hashCode() {
    int hashCode = this.hashCode;
    if (hashCode == 0) {
      HashCodeVisitor visitor = new HashCodeVisitor();
      visitFields(visitor);
      hashCode = this.hashCode = visitor.result;
    }
    return hashCode;
  }

  @Override public final boolean equals(Object o) {
    if (o == this) return true;
    if (o == null || o.getClass() != getClass()) return false;

    EqualsVisitor thisVisitor = new EqualsVisitor();
    visitFields(thisVisitor);

    EqualsVisitor otherVisitor = new EqualsVisitor();
    ((Message<?>) o).visitFields(otherVisitor);

    return thisVisitor.isEqualTo(otherVisitor);
  }

  private Object writeReplace() throws ObjectStreamException {
    return new MessageSerializedForm(this, getClass());
  }

  /**
   * Superclass for protocol buffer message builders.
   */
  public abstract static class Builder<T extends Message> {

    UnknownFieldMap unknownFieldMap;

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
      if (message != null && message.unknownFields != null) {
        this.unknownFieldMap = new UnknownFieldMap(message.unknownFields);
      }
    }

    public final void readUnknown(int tag, ProtoReader reader) throws IOException {
      ensureUnknownFieldMap().read(tag, reader);
    }

    /**
     * Adds a {@code varint} value to the unknown field set with the given tag number.
     */
    public final void addVarint(int tag, long value) {
      try {
        ensureUnknownFieldMap().addVarint(tag, value);
      } catch (IOException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }

    /**
     * Adds a {@code fixed32} value to the unknown field set with the given tag number.
     */
    public final void addFixed32(int tag, int value) {
      try {
        ensureUnknownFieldMap().addFixed32(tag, value);
      } catch (IOException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }

    /**
     * Adds a {@code fixed64} value to the unknown field set with the given tag number.
     */
    public final void addFixed64(int tag, long value) {
      try {
        ensureUnknownFieldMap().addFixed64(tag, value);
      } catch (IOException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }

    /**
     * Adds a length delimited value to the unknown field set with the given tag number.
     */
    public final void addLengthDelimited(int tag, ByteString value) {
      try {
        ensureUnknownFieldMap().addLengthDelimited(tag, value);
      } catch (IOException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }

    UnknownFieldMap ensureUnknownFieldMap() {
      if (unknownFieldMap == null) {
        unknownFieldMap = new UnknownFieldMap();
      }
      return unknownFieldMap;
    }

    /**
     * Create an exception for missing required fields.
     *
     * @param args Alternating field value and field name pairs.
     */
    protected final IllegalStateException missingRequiredFields(Object... args) {
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
     * Returns an immutable {@link com.squareup.wire.Message} based on the fields that have been set
     * in this builder.
     */
    public abstract T build();
  }

  protected static <M extends Message<M>> M message(ProtoReader reader, TypeAdapter<M> adapter)
      throws IOException {
    long cursor = reader.beginLengthDelimited();
    M message = adapter.read(reader);
    reader.endLengthDelimited(cursor);
    return message;
  }

  protected static <M extends Message<M>> List<M> repeatedMessage(List<M> existing,
      ProtoReader reader, TypeAdapter<M> adapter) throws IOException {
    long cursor = reader.beginLengthDelimited();
    existing = repeated(existing, adapter.read(reader));
    reader.endLengthDelimited(cursor);
    return existing;
  }

  protected static <M extends Enum<M> & WireEnum> M enumOrUnknown(int tag, ProtoReader reader,
      TypeAdapter.EnumAdapter<M> adapter, Builder builder) throws IOException {
    int value = reader.value(TypeAdapter.UINT32);
    M constant = adapter.fromValue(value);
    if (constant != null) {
      return constant;
    }
    builder.addVarint(tag, value);
    return null;
  }

  protected static <M extends Enum<M> & WireEnum> List<M> repeatedEnumOrUnknown(int tag,
      List<M> existing, ProtoReader reader, TypeAdapter.EnumAdapter<M> adapter, Builder builder)
      throws IOException {
    int value = TypeAdapter.UINT32.read(reader);
    M constant = adapter.fromValue(value);
    if (constant != null) {
      return repeated(existing, constant);
    }
    builder.addVarint(tag, value);
    return existing;
  }

  protected static <M extends Enum<M> & WireEnum> List<M> packedEnumOrUnknown(int tag,
      List<M> existing, ProtoReader reader, TypeAdapter.EnumAdapter<M> adapter, Builder builder)
      throws IOException {
    long cursor = reader.beginLengthDelimited();
    while (reader.hasNext()) {
      int value = TypeAdapter.UINT32.read(reader);
      M constant = adapter.fromValue(value);
      if (constant != null) {
        existing = repeated(existing, constant);
      } else {
        builder.addVarint(tag, value);
      }
    }
    reader.endLengthDelimited(cursor);
    return existing;
  }

  protected static <E> List<E> repeated(List<E> existing, E item) {
    List<E> value = existing == null || existing == Collections.emptyList()
        ? new ArrayList<E>()
        : existing;
    value.add(item);
    return value;
  }

  protected static <E> int sizeOf(int tag, E value, TypeAdapter<E> adapter) {
    if (value == null) {
      return 0;
    }
    int size = adapter.serializedSize(value);
    if (adapter.type == TYPE_LEN_DELIMITED) {
      size += varint32Size(size);
    }
    return ProtoWriter.tagSize(tag) + size;
  }

  protected static <E> int sizeOfRepeated(int tag, List<E> value, TypeAdapter<E> adapter) {
    if (value == null) {
      throw new NullPointerException("value == null)");
    }
    if (value.isEmpty()) {
      return 0;
    }
    int size = ProtoWriter.tagSize(tag);
    int valueCount = value.size();
    size += varint32Size(adapter.makeTag(tag)) * valueCount;
    for (int i = 0; i < valueCount; i++) {
      size += adapter.serializedSize(value.get(i));
    }
    return size;
  }

  protected static <E> int sizeOfPacked(int tag, List<E> value, TypeAdapter<E> adapter) {
    if (value == null) {
      throw new NullPointerException("value == null)");
    }
    if (value.isEmpty()) {
      return 0;
    }
    int size = 0;
    for (int i = 0, count = value.size(); i < count; i++) {
      size += adapter.serializedSize(value.get(i));
    }
    return tagSize(tag) + varint32Size(size) + size;
  }

  protected interface Visitor {
    <T> void value(int tag, String name, T value, TypeAdapter<T> adapter, boolean redacted);
    <T> void repeated(int tag, String name, List<T> list, TypeAdapter<T> adapter, boolean redacted);
    <T> void packed(int tag, String name, List<T> list, TypeAdapter<T> adapter, boolean redacted);
    void unknowns(Message<?> message);
    void extensions(ExtendableMessage<?> message);
  }

  private static final class SerializedSizeVisitor implements Visitor {
    int size;

    @Override
    public <T> void value(int tag, String name, T value, TypeAdapter<T> adapter, boolean redacted) {
      size += sizeOf(tag, value, adapter);
    }

    @Override public <T> void repeated(int tag, String name, List<T> value, TypeAdapter<T> adapter,
        boolean redacted) {
      size += sizeOfRepeated(tag, value, adapter);
    }

    @Override public <T> void packed(int tag, String name, List<T> value, TypeAdapter<T> adapter,
        boolean redacted) {
      size += sizeOfPacked(tag, value, adapter);
    }

    @Override public void unknowns(Message<?> message) {
      UnknownFieldMap unknownFields = message.unknownFields;
      if (unknownFields != null) {
        size += unknownFields.serializedSize();
      }
    }

    @Override public void extensions(ExtendableMessage<?> message) {
      ExtensionMap<?> extensionMap = message.extensionMap;
      if (extensionMap != null) {
        size += extensionMap.serializedSize();
      }
    }
  }

  private static final class ToStringVisitor implements Visitor {
    private final StringBuilder builder = new StringBuilder();

    @Override
    public <T> void value(int tag, String name, T value, TypeAdapter<T> adapter, boolean redacted) {
      if (value == null) return;
      if (builder.length() > 0) builder.append(", ");
      builder.append(name).append("=").append(redacted ? "██" : value);
    }

    @Override public <T> void repeated(int tag, String name, List<T> list, TypeAdapter<T> adapter,
        boolean redacted) {
      value(tag, name, list, null, redacted);
    }

    @Override public <T> void packed(int tag, String name, List<T> list, TypeAdapter<T> adapter,
        boolean redacted) {
      value(tag, name, list, null, redacted);
    }

    @Override public void unknowns(Message<?> message) {
    }

    @Override public void extensions(ExtendableMessage<?> message) {
      if (builder.length() > 0) builder.append(", ");
      ExtensionMap<?> extensionMap = message.extensionMap;
      builder.append("{extensions=")
          .append(extensionMap == null ? "{}" : extensionMap.toString())
          .append('}');
    }

    String finish(Message message) {
      builder.insert(0, message.name + '{');
      return builder.append('}').toString();
    }
  }

  private static final class HashCodeVisitor implements Visitor {
    int result = 0;

    @Override
    public <T> void value(int tag, String name, T value, TypeAdapter<T> adapter, boolean redacted) {
      result = result * 37 + (value != null ? value.hashCode() : 0);
    }

    @Override public <T> void repeated(int tag, String name, List<T> list, TypeAdapter<T> adapter,
        boolean redacted) {
      result = result * 37 + list.hashCode();
    }

    @Override public <T> void packed(int tag, String name, List<T> list, TypeAdapter<T> adapter,
        boolean redacted) {
      result = result * 37 + list.hashCode();
    }

    @Override public void unknowns(Message<?> message) {
    }

    @Override public void extensions(ExtendableMessage<?> message) {
      ExtensionMap<?> extensionMap = message.extensionMap;
      result = result * 37 + (extensionMap != null ? extensionMap.hashCode() : 0);
    }
  }

  private static final class EqualsVisitor implements Visitor {
    private final List<Object> instances = new ArrayList<Object>();

    @Override
    public <T> void value(int tag, String name, T value, TypeAdapter<T> adapter, boolean redacted) {
      instances.add(value);
    }

    @Override public <T> void repeated(int tag, String name, List<T> list, TypeAdapter<T> adapter,
        boolean redacted) {
      instances.add(list);
    }

    @Override public <T> void packed(int tag, String name, List<T> list, TypeAdapter<T> adapter,
        boolean redacted) {
      instances.add(list);
    }

    @Override public void unknowns(Message<?> message) {
    }

    @Override public void extensions(ExtendableMessage<?> message) {
      instances.add(message.extensionMap);
    }

    boolean isEqualTo(EqualsVisitor other) {
      return instances.equals(other.instances);
    }
  }
}
