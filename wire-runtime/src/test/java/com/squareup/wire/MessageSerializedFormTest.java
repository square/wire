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

import com.squareup.wire.protos.roots.C;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.StreamCorruptedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MessageSerializedForm.class, ProtoAdapter.class})
public class MessageSerializedFormTest {

  @Test public void constructor() throws Exception {
    // when
    MessageSerializedForm messageSerializedForm = new MessageSerializedForm(new C(5), C.class);

    // then
    assertThat(((byte[])Whitebox.getInternalState(messageSerializedForm, "bytes"))[1]).isEqualTo((byte) 5);
    assertThat(Whitebox.getInternalState(messageSerializedForm, "messageClass")).isEqualTo(C.class);

  }

  @Test public void readResolve() throws Exception {
    // when
    C message = new C(5);
    MessageSerializedForm messageSerializedForm = new MessageSerializedForm(message, message.getClass());

    // then
    assertThat(messageSerializedForm.readResolve()).isEqualTo(message);
  }

  @Test(expected = StreamCorruptedException.class) public void readResolveThrown() throws Exception {
    // given
    ProtoAdapter protoAdapterMock = mock(ProtoAdapter.class);
    when(protoAdapterMock.decode(Mockito.any(byte[].class))).thenThrow(new IOException());

    mockStatic(ProtoAdapter.class);
    when(ProtoAdapter.get(Mockito.any(Class.class))).thenReturn(protoAdapterMock);

    C message = new C(5);
    MessageSerializedForm messageSerializedForm = new MessageSerializedForm(message, message.getClass());

    // when
    messageSerializedForm.readResolve();

    // then
    fail("MessageSerializedForm should throw StreamCorruptedException when decoding caused IO error");
  }
}
