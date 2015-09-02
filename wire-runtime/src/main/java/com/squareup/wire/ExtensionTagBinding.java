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

import com.squareup.wire.ExtendableMessage.ExtendableBuilder;

final class ExtensionTagBinding<M extends Message> extends TagBinding<M, Message.Builder<M>> {
  private final Extension<?, ?> extension;

  public ExtensionTagBinding(Extension<?, ?> extension, WireAdapter<?> singleAdapter) {
    super(extension.getLabel(), extension.getDatatype(), extension.getName(), extension.getTag(),
        false, singleAdapter, extension.getMessageType());
    this.extension = extension;
  }

  @Override public Object get(M message) {
    TagMap extensionMap = ((ExtendableMessage<?>) message).tagMap;
    return extensionMap != null
        ? extensionMap.get(extension)
        : null;
  }

  @Override Object getFromBuilder(Message.Builder<M> builder) {
    return ((ExtendableBuilder) builder).getExtension(extension);
  }

  @Override void set(Message.Builder<M> builder, Object value) {
    ((ExtendableBuilder) builder).setExtension(extension, value);
  }
}
