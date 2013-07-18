// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.util.HashMap;
import java.util.Map;

final class ExtensionRegistry {

  private final Map<Class<? extends ExtendableMessage>, Map<Integer, Extension<?, ?>>>
      extensions = new HashMap<Class<? extends ExtendableMessage>,
          Map<Integer, Extension<?, ?>>>();

  public <T extends ExtendableMessage, E> void add(Extension<T, E> extension) {
    Class<? extends ExtendableMessage> messageClass = extension.getExtendedType();
    Map<Integer, Extension<?, ?>> map = extensions.get(messageClass);
    if (map == null) {
      map = new HashMap<Integer, Extension<?, ?>>();
      extensions.put(messageClass, map);
    }
    map.put(extension.getTag(), extension);
  }

  @SuppressWarnings("unchecked")
  public <T extends ExtendableMessage, E> Extension<T, E>
      getExtension(Class<T> messageClass, int tag) {
    Map<Integer, Extension<?, ?>> map = extensions.get(messageClass);
    return map == null ? null : (Extension<T, E>) map.get(tag);
  }
}
