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
import java.util.Collections;
import java.util.List;

public abstract class Extension<M extends ExtendableMessage<M>, T, R> extends TypeAdapter<R>
    implements Comparable<Extension<M, T, R>> {
  public static <M extends ExtendableMessage<M>, T> Extension<M, T, T> optional(
      Class<M> messageClass, int tag, TypeAdapter<T> adapter) {
    return single(messageClass, tag, adapter, false);
  }

  public static <M extends ExtendableMessage<M>, T> Extension<M, T, T> required(
      Class<M> messageClass, int tag, TypeAdapter<T> adapter) {
    return single(messageClass, tag, adapter, true);
  }

  private static <M extends ExtendableMessage<M>, T> Extension<M, T, T> single(
      Class<M> messageClass, int tag, final TypeAdapter<T> adapter, boolean required) {
    return new Extension<M, T, T>(messageClass, tag, adapter.type, required) {
      @Override public int serializedSize(T value) {
        return Message.sizeOf(tag, value, adapter);
      }

      @Override public void write(T value, ProtoWriter writer) throws IOException {
        writer.value(tag, value, adapter);
      }

      @Override public T read(ProtoReader reader) throws IOException {
        return reader.value(adapter);
      }

      @Override T read(T existing, ProtoReader reader) throws IOException {
        if (existing != null) {
          throw new IOException("Multiple values for extension " + messageClass + " " + tag);
        }

        long cursor = -1;
        if (type == TYPE_LEN_DELIMITED) {
          cursor = reader.beginLengthDelimited();
        }
        T read = read(reader);
        if (cursor != -1) {
          reader.endLengthDelimited(cursor);
        }
        return read;
      }
    };
  }

  public static <M extends ExtendableMessage<M>, T> Extension<M, T, List<T>> repeated(
      Class<M> messageClass, int tag, final TypeAdapter<T> adapter) {
    return new Extension<M, T, List<T>>(messageClass, tag, adapter.type, false) {
      @Override public int serializedSize(List<T> value) {
        return Message.sizeOfRepeated(tag, value, adapter);
      }

      @Override public void write(List<T> value, ProtoWriter writer) throws IOException {
        writer.repeated(tag, value, adapter);
      }

      @Override public List<T> read(ProtoReader reader) throws IOException {
        return read(Collections.<T>emptyList(), reader);
      }

      @Override List<T> read(List<T> existing, ProtoReader reader) throws IOException {
        return reader.repeated(existing, adapter);
      }
    };
  }

  public static <M extends ExtendableMessage<M>, T> Extension<M, T, List<T>> packed(
      Class<M> messageClass, int tag, final TypeAdapter<T> adapter) {
    return new Extension<M, T, List<T>>(messageClass, tag, adapter.type, false) {
      @Override public int serializedSize(List<T> value) {
        return Message.sizeOfPacked(tag, value, adapter);
      }

      @Override public void write(List<T> value, ProtoWriter writer) throws IOException {
        writer.packed(tag, value, adapter);
      }

      @Override public List<T> read(ProtoReader reader) throws IOException {
        return read(Collections.<T>emptyList(), reader);
      }

      @Override List<T> read(List<T> existing, ProtoReader reader) throws IOException {
        return reader.packed(existing, adapter);
      }
    };
  }

  final Class<M> messageClass;
  final int tag;
  final boolean required;

  private Extension(Class<M> messageClass, int tag, int type, boolean required) {
    super(type);
    this.messageClass = messageClass;
    this.tag = tag;
    this.required = required;
  }

  abstract R read(R existing, ProtoReader reader) throws IOException;

  @Override public final int compareTo(Extension<M, T, R> o) {
    if (messageClass != o.messageClass) {
      return messageClass.getName().compareTo(o.messageClass.getName());
    }
    return tag - o.tag;
  }
}
