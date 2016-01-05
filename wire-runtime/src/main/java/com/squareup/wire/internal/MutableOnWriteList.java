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
package com.squareup.wire.internal;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

/** A wrapper around an empty/immutable list which only switches to mutable on first mutation. */
final class MutableOnWriteList<T> extends AbstractList<T> implements RandomAccess, Serializable {
  private final List<T> immutableList;
  List<T> mutableList;

  MutableOnWriteList(List<T> immutableList) {
    this.immutableList = immutableList;
    this.mutableList = immutableList;
  }

  @Override public T get(int index) {
    return mutableList.get(index);
  }

  @Override public int size() {
    return mutableList.size();
  }

  @Override public T set(int index, T element) {
    if (mutableList == immutableList) {
      mutableList = new ArrayList<>(immutableList);
    }
    return mutableList.set(index, element);
  }

  @Override public void add(int index, T element) {
    if (mutableList == immutableList) {
      mutableList = new ArrayList<>(immutableList);
    }
    mutableList.add(index, element);
  }

  @Override public T remove(int index) {
    if (mutableList == immutableList) {
      mutableList = new ArrayList<>(immutableList);
    }
    return mutableList.remove(index);
  }

  private Object writeReplace() throws ObjectStreamException {
    return new ArrayList<>(mutableList);
  }
}
