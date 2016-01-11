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

public class ImmutableListTest {

  private List<String> initialList;
  private ImmutableList immutableList;

  @Before public void init() {
    initialList = new ArrayList<>(Arrays.asList("one", "two", "three"));
    immutableList = new ImmutableList<>(initialList);
  }


  @Test public void get() {
    assertThat(immutableList.get(0)).isEqualTo(initialList.get(0));
    assertThat(immutableList.get(1)).isEqualTo(initialList.get(1));
    assertThat(immutableList.get(2)).isEqualTo(initialList.get(2));
  }

  @Test public void size() {
    assertThat(immutableList.size()).isEqualTo(initialList.size());
  }
}
