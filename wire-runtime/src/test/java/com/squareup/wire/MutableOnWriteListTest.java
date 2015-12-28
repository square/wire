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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MutableOnWriteListTest {

  private List<String> initialList;
  private MutableOnWriteList mutableOnWriteList;

  @Before
  public void init() {
    initialList = new ArrayList<>(Arrays.asList("one", "two", "three"));
    mutableOnWriteList = new MutableOnWriteList<>(initialList);
  }

  @Test public void get() {
    assertThat(mutableOnWriteList.get(0)).isEqualTo(initialList.get(0));
    assertThat(mutableOnWriteList.get(1)).isEqualTo(initialList.get(1));
    assertThat(mutableOnWriteList.get(2)).isEqualTo(initialList.get(2));
  }

  @Test public void size() {
    assertThat(mutableOnWriteList.size()).isEqualTo(initialList.size());
  }

  @Test public void add() {
    // when
    mutableOnWriteList.add(0, "zero");

    // then
    assertThat(initialList.get(0)).isEqualTo("one");
    assertThat(mutableOnWriteList.get(0)).isEqualTo("zero");
    assertThat(initialList.size()).isEqualTo(3);
    assertThat(mutableOnWriteList.size()).isEqualTo(4);
  }

  @Test public void set() {
    // when
    mutableOnWriteList.set(0, "zero");

    // then
    assertThat(initialList.get(0)).isEqualTo("one");
    assertThat(mutableOnWriteList.get(0)).isEqualTo("zero");
    assertThat(initialList.size()).isEqualTo(3);
    assertThat(mutableOnWriteList.size()).isEqualTo(3);
  }

  @Test public void remove() {
    // when
    mutableOnWriteList.remove(1);

    // then
    assertThat(initialList.get(1)).isEqualTo("two");
    assertThat(mutableOnWriteList.get(1)).isEqualTo("three");
    assertThat(initialList.size()).isEqualTo(3);
    assertThat(mutableOnWriteList.size()).isEqualTo(2);
  }

  @Test public void repeatableRemove() {
    // when
    mutableOnWriteList.remove(0);
    mutableOnWriteList.remove(0);

    // then
    assertThat(initialList.get(0)).isEqualTo("one");
    assertThat(mutableOnWriteList.get(0)).isEqualTo("three");
    assertThat(initialList.size()).isEqualTo(3);
    assertThat(mutableOnWriteList.size()).isEqualTo(1);
  }
}
