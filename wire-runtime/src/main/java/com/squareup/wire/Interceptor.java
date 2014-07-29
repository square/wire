/*
 * Copyright 2014 Square Inc.
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

/**
 * An Interceptor is invoked to perform an arbitrary transformation on a {@link Message}
 * during serialization and deserialization. The only methods in Wire that cause Interceptors to be
 * invoked are {@link Wire#toByteArray(Message)}, {@link Wire#writeTo(Message, byte[])},
 * {@link Wire#writeTo(Message, byte[], int, int)}, {@link Wire#parseFrom(byte[], Class)}
 */
public interface Interceptor {

  /**
   * Transforms the given source message prior to serialization.
   */
  <M extends Message> M preSerialize(M source);

  /**
   * Transforms the given source message following deserialization.
   */
  <M extends Message> M postDeserialize(M source);
}
