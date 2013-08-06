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

import java.util.LinkedHashMap;
import java.util.Map;

final class ExtensionRegistry {

  private final Map<Class<? extends ExtendableMessage>, Map<Integer, Extension<?, ?>>>
      extensionsByTag = new LinkedHashMap<Class<? extends ExtendableMessage>,
                Map<Integer, Extension<?, ?>>>();
  private final Map<Class<? extends ExtendableMessage>, Map<String, Extension<?, ?>>>
      extensionsByName = new LinkedHashMap<Class<? extends ExtendableMessage>,
          Map<String, Extension<?, ?>>>();

  public <T extends ExtendableMessage<?>, E> void add(Extension<T, E> extension) {
    Class<? extends ExtendableMessage<?>> messageClass = extension.getExtendedType();
    Map<Integer, Extension<?, ?>> tagMap = extensionsByTag.get(messageClass);
    Map<String, Extension<?, ?>> nameMap = extensionsByName.get(messageClass);
    if (tagMap == null) {
      tagMap = new LinkedHashMap<Integer, Extension<?, ?>>();
      nameMap = new LinkedHashMap<String, Extension<?, ?>>();
      extensionsByTag.put(messageClass, tagMap);
      extensionsByName.put(messageClass, nameMap);
    }
    tagMap.put(extension.getTag(), extension);
    nameMap.put(extension.getName(), extension);
  }

  @SuppressWarnings("unchecked")
  public <T extends ExtendableMessage<?>, E> Extension<T, E>
      getExtension(Class<T> messageClass, int tag) {
    Map<Integer, Extension<?, ?>> map = extensionsByTag.get(messageClass);
    return map == null ? null : (Extension<T, E>) map.get(tag);
  }

  @SuppressWarnings("unchecked")
  public <T extends ExtendableMessage<?>, E> Extension<T, E>
      getExtension(Class<T> messageClass, String name) {
    Map<String, Extension<?, ?>> nameMap = extensionsByName.get(messageClass);
    return nameMap == null ? null : (Extension<T, E>) nameMap.get(name);
  }
}
