// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.util.HashMap;
import java.util.Map;

final class ExtensionRegistry {

  private final Map<Class<? extends ExtendableMessage>, Map<Integer, Extension<?, ?>>>
      extensions = new HashMap<Class<? extends ExtendableMessage>,
          Map<Integer, Extension<?, ?>>>();

  public <MessageType extends ExtendableMessage, Type> void
      add(Extension<MessageType, Type> extension) {
    Class<? extends ExtendableMessage> messageClass = extension.getExtendedType();
    Map<Integer, Extension<?, ?>> map = extensions.get(messageClass);
    if (map == null) {
      map = new HashMap<Integer, Extension<?, ?>>();
      extensions.put(messageClass, map);
    }
    map.put(extension.getTag(), extension);
  }

  @SuppressWarnings("unchecked")
  public <M extends ExtendableMessage, E> Extension<M, E>
      getExtension(Class<M> messageClass, int tag) {
    Map<Integer, Extension<?, ?>> map = extensions.get(messageClass);
    return map == null ? null : (Extension<M, E>) map.get(tag);
  }
}
