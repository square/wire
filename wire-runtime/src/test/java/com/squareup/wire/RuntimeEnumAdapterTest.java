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
import com.squareup.wire.protos.roots.PrivateFromValue;
import com.squareup.wire.protos.roots.UnsupportedFromValue;
import com.squareup.wire.protos.roots.WithoutFromValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ProtoReader.class)
public class RuntimeEnumAdapterTest {

  @Test public void constructor() throws Exception {
    // when
    RuntimeEnumAdapter runtimeEnumAdapter = new RuntimeEnumAdapter<>(G.class);

    // then
    assertThat(Whitebox.getInternalState(runtimeEnumAdapter, "type")).isEqualTo(G.class);
    assertThat(Whitebox.getInternalState(runtimeEnumAdapter, "fromValueMethod")).isEqualTo(G.class.getMethod("fromValue", int.class));
  }

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

  // better solution is to declare fromValue method in WireEnum
  @Test public void decodePrivateFromValueMethod() throws Exception {
    // given
    ProtoReader protoReader = mock(ProtoReader.class);
    RuntimeEnumAdapter runtimeEnumAdapter = new RuntimeEnumAdapter<>(PrivateFromValue.class);

    PowerMockito.when(protoReader.readVarint32()).thenReturn(1);

    // when
    try {
      runtimeEnumAdapter.decode(protoReader);
    }
    catch (AssertionError e) {
      return;
    }

    // then
    fail("RuntimeEnumAdapter should throw AssertionError when type with private fromValue(int) method is used");
  }

  @Test public void decodeFromValueMethodWithException() throws Exception {
    // given
    ProtoReader protoReader = mock(ProtoReader.class);
    RuntimeEnumAdapter runtimeEnumAdapter = new RuntimeEnumAdapter<>(UnsupportedFromValue.class);

    PowerMockito.when(protoReader.readVarint32()).thenReturn(1);

    // when
    try {
      runtimeEnumAdapter.decode(protoReader);
    }
    catch (AssertionError e) {
      return;
    }

    // then
    fail("RuntimeEnumAdapter should throw AssertionError when type with fromValue(int) throws exception while execution");
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
