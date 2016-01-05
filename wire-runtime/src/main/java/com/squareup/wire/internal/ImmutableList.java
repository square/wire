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
package com.squareup.wire.internal;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

final class ImmutableList<T> extends AbstractList<T> implements RandomAccess, Serializable {
  final List<T> list;

  ImmutableList(List<T> list) {
    this.list = new ArrayList<>(list);
  }

  @Override public int size() {
    return list.size();
  }

  @Override public T get(int i) {
    return list.get(i);
  }

  private Object writeReplace() throws ObjectStreamException {
    return Collections.unmodifiableList(list);
  }
}
