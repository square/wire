/*
 * Copyright 2014 Square Inc.
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

import com.squareup.wire.protos.foreign.ForeignEnum;
import com.squareup.wire.protos.roots.C;
import com.squareup.wire.protos.roots.G;
import com.squareup.wire.protos.roots.WithoutFromValue;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RuntimeEnumAdapterTest {

  // better solution is to declare fromValue method in WireEnum
  @Test public void constructorIllegalArgument() throws Exception {
    // when
    try {
      new RuntimeEnumAdapter<>(WithoutFromValue.class);
    }
    catch (AssertionError e) {
      return;
    }

    // then
    fail("RuntimeEnumAdapter should throw AssertionError when type without fromValue(int) method is passed as constructor argument");
  }

  @Test public void equalsMethod() throws Exception {
    assertThat(new RuntimeEnumAdapter<>(G.class).equals(G.ADAPTER)).isTrue();
    assertThat(new RuntimeEnumAdapter<>(G.class).equals(new RuntimeEnumAdapter(ForeignEnum.class))).isFalse();
    assertThat(new RuntimeEnumAdapter<>(G.class).equals(C.ADAPTER)).isFalse();
  }

  @Test public void hashCodeMethod() throws Exception {
    assertThat(new RuntimeEnumAdapter<>(G.class).hashCode()).isEqualTo(G.class.hashCode());
    assertThat(new RuntimeEnumAdapter<>(ForeignEnum.class).hashCode()).isEqualTo(ForeignEnum.class.hashCode());
  }
}
