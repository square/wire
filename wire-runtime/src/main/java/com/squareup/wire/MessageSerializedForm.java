/*
 * Copyright 2015 Square Inc.
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
import java.io.StreamCorruptedException;
import okio.Buffer;

final class MessageSerializedForm implements Serializable {
  private static final long serialVersionUID = 0L;

  private final byte[] bytes;
  private final Class<? extends Message> messageClass;

  public MessageSerializedForm(Message message, Class<? extends Message> messageClass) {
    TypeAdapter<Message> adapter;
    try {
      adapter = (TypeAdapter<Message>) messageClass.getField("ADAPTER").get(null);
    } catch (IllegalAccessException e) {
      throw new AssertionError("ADAPTER field not found");
    } catch (NoSuchFieldException e) {
      throw new AssertionError("ADAPTER field not found");
    }
    Buffer buffer = new Buffer();
    try {
      adapter.write(message, new ProtoWriter(buffer));
    } catch (IOException e) {
      throw new AssertionError(e); // No I/O writing to a Buffer.
    }
    this.bytes = buffer.readByteArray();
    this.messageClass = messageClass;
  }

  Object readResolve() throws ObjectStreamException {
    TypeAdapter<?> adapter;
    try {
      adapter = (TypeAdapter<?>) messageClass.getField("ADAPTER").get(null);
    } catch (IllegalAccessException e) {
      throw new AssertionError("ADAPTER field not found");
    } catch (NoSuchFieldException e) {
      throw new AssertionError("ADAPTER field not found");
    }
    Buffer buffer = new Buffer().write(bytes);
    try {
      return adapter.read(new ProtoReader(buffer));
    } catch (IOException e) {
      throw new StreamCorruptedException(e.getMessage());
    }
  }
}
