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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExtensionRegistry {
  private final Map<Class<? extends Message>, List<Extension<?, ?>>> messageToExtensions =
      new LinkedHashMap<>();

  /**
   * Creates a new instance that can encode and decode the extensions specified in
   * {@code extensionClasses}. Typically the classes in this list are generated and start with the
   * "Ext_" prefix.
   */
  public ExtensionRegistry(Class<?>... extensionClasses) {
    this(Arrays.asList(extensionClasses));
  }

  /**
   * Creates a new instance that can encode and decode the extensions specified in
   * {@code extensionClasses}. Typically the classes in this list are generated and start with the
   * "Ext_" prefix.
   */
  public ExtensionRegistry(List<Class<?>> extensionClasses) {
    for (Class<?> extensionClass : extensionClasses) {
      for (Field field : extensionClass.getDeclaredFields()) {
        if (field.getType().equals(Extension.class)) {
          try {
            registerExtension((Extension) field.get(null));
          } catch (IllegalAccessException e) {
            throw new AssertionError(e);
          }
        }
      }
    }
  }

  private <T extends Message<T>, E> void registerExtension(Extension<T, E> extension) {
    Class<? extends Message> messageClass = extension.getExtendedType();
    List<Extension<?, ?>> extensions = messageToExtensions.get(messageClass);
    if (extensions == null) {
      extensions = new ArrayList<>();
      messageToExtensions.put(messageClass, extensions);
    }
    extensions.add(extension);
  }

  @SuppressWarnings("unchecked")
  public List<Extension<?, ?>> extensions(Class<? extends Message> messageClass) {
    List<Extension<?, ?>> map = messageToExtensions.get(messageClass);
    return map != null ? map : Collections.<Extension<?, ?>>emptyList();
  }
}
