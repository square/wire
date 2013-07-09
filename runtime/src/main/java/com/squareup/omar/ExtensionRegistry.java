// Copyright 2013 Square, Inc.
package com.squareup.omar;

import java.util.HashMap;
import java.util.Map;

import static com.squareup.omar.Message.ExtendableMessage.Extension;

final class ExtensionRegistry {

  private final Map<Class<? extends Message.ExtendableMessage>, Map<Integer, Extension<?, ?>>>
      extensions = new HashMap<Class<? extends Message.ExtendableMessage>,
          Map<Integer, Extension<?, ?>>>();

  public <MessageType extends Message.ExtendableMessage, Type> void
      add(Extension<MessageType, Type> extension) {
    Class<? extends Message.ExtendableMessage> messageClass = extension.getExtendedType();
    Map<Integer, Extension<?, ?>> map = extensions.get(messageClass);
    if (map == null) {
      map = new HashMap<Integer, Extension<?, ?>>();
      extensions.put(messageClass, map);
    }
    map.put(extension.getTag(), extension);
  }

  public <MessageType extends Message.ExtendableMessage, Type> Extension<MessageType, Type>
      getExtension(Class<MessageType> messageClass, int tag) {
    Map<Integer, Extension<?, ?>> map = extensions.get(messageClass);
    return map == null ? null : (Extension<MessageType, Type>) map.get(tag);
  }
}
