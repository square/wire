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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ExtensionRegistry {
  private final Map<Class<? extends Message>, List<Extension<?, ?>>> messageToExtensions
      = new LinkedHashMap<Class<? extends Message>, List<Extension<?, ?>>>();

  public <T extends ExtendableMessage<T>, E> void add(Extension<T, E> extension) {
    Class<? extends Message> messageClass = extension.getExtendedType();
    List<Extension<?, ?>> extensions = messageToExtensions.get(messageClass);
    if (extensions == null) {
      extensions = new ArrayList<Extension<?, ?>>();
      messageToExtensions.put(messageClass, extensions);
    }
    extensions.add(extension);
  }

  @SuppressWarnings("unchecked")
  public List<Extension<?, ?>> getExtensions(Class<? extends Message> messageClass) {
    List<Extension<?, ?>> map = messageToExtensions.get(messageClass);
    return map != null ? map : Collections.<Extension<?, ?>>emptyList();
  }
}
