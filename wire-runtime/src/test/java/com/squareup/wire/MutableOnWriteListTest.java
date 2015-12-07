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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MutableOnWriteList.class)
public class MutableOnWriteListTest {

  private List<String> initialList;


  @Before
  public void init() {
    initialList = Arrays.asList("one", "two", "three");
  }


  @Test public void constructor() throws Exception {
    // when
    MutableOnWriteList mutableOnWriteList = new MutableOnWriteList(initialList);

    // then
    assertThat(Whitebox.getInternalState(mutableOnWriteList, "immutableList")).isEqualTo(initialList);
    assertThat(Whitebox.getInternalState(mutableOnWriteList, "mutableList")).isEqualTo(initialList);
  }


  @Ignore("Constructor argument parameter should be wrapped to immutable list")
  @Test public void imutabilityViolation() throws Exception {
    // given
    MutableOnWriteList mutableOnWriteList = new MutableOnWriteList(initialList);

    // when
    initialList.add("four");

    // then
    assertThat(Whitebox.getInternalState(mutableOnWriteList, "immutableList")).isNotEqualTo(initialList);
    assertThat(Whitebox.getInternalState(mutableOnWriteList, "mutableList")).isNotEqualTo(initialList);
  }

  @Test public void get() throws Exception {
    // when
    MutableOnWriteList mutableOnWriteList = new MutableOnWriteList(initialList);

    // then
    assertThat(mutableOnWriteList.get(0)).isEqualTo(initialList.get(0));
    assertThat(mutableOnWriteList.get(1)).isEqualTo(initialList.get(1));
    assertThat(mutableOnWriteList.get(2)).isEqualTo(initialList.get(2));
  }

  @Test public void size() throws Exception {
    // when
    MutableOnWriteList mutableOnWriteList = new MutableOnWriteList(initialList);

    // then
    assertThat(mutableOnWriteList.size()).isEqualTo(initialList.size());
  }

  @Test public void add() throws Exception {
    // given
    MutableOnWriteList mutableOnWriteList = new MutableOnWriteList(initialList);

    // when
    mutableOnWriteList.add(0, "zero");

    // then
    assertThat(initialList.get(0)).isEqualTo("one");
    assertThat(mutableOnWriteList.get(0)).isEqualTo("zero");

    assertThat(initialList.size()).isEqualTo(3);
    assertThat(mutableOnWriteList.size()).isEqualTo(4);
  }

  @Test public void set() throws Exception {
    // given
    MutableOnWriteList mutableOnWriteList = new MutableOnWriteList(initialList);

    // when
    mutableOnWriteList.set(0, "zero");

    // then
    assertThat(initialList.get(0)).isEqualTo("one");
    assertThat(mutableOnWriteList.get(0)).isEqualTo("zero");

    assertThat(initialList.size()).isEqualTo(3);
    assertThat(mutableOnWriteList.size()).isEqualTo(3);
  }

  @Test public void remove() throws Exception {
    // given
    MutableOnWriteList mutableOnWriteList = new MutableOnWriteList(initialList);

    // when
    mutableOnWriteList.remove(1);

    // then
    assertThat(initialList.get(1)).isEqualTo("two");
    assertThat(mutableOnWriteList.get(1)).isEqualTo("three");

    assertThat(initialList.size()).isEqualTo(3);
    assertThat(mutableOnWriteList.size()).isEqualTo(2);
  }

  @Test public void repeatableRemove() throws Exception {
    // given
    MutableOnWriteList mutableOnWriteList = new MutableOnWriteList(initialList);

    // when
    mutableOnWriteList.remove(0);
    mutableOnWriteList.remove(0);

    // then
    assertThat(initialList.get(0)).isEqualTo("one");
    assertThat(mutableOnWriteList.get(0)).isEqualTo("three");

    assertThat(initialList.size()).isEqualTo(3);
    assertThat(mutableOnWriteList.size()).isEqualTo(1);
  }

  @Test public void writeReplace() throws Exception {
    // given
    MutableOnWriteList mutableOnWriteList = new MutableOnWriteList(initialList);

    // when
    List<String> newList = Whitebox.invokeMethod(mutableOnWriteList, "writeReplace");
    newList.remove(0);

    // then
    assertThat(initialList.get(0)).isEqualTo("one");
    assertThat(mutableOnWriteList.get(0)).isEqualTo("one");
    assertThat(newList.get(0)).isEqualTo("two");

    assertThat(initialList.size()).isEqualTo(3);
    assertThat(mutableOnWriteList.size()).isEqualTo(3);
    assertThat(newList.size()).isEqualTo(2);
  }

}
