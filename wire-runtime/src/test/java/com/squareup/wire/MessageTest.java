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
import okio.Buffer;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Message.class, Message.Builder.class, ProtoWriter.class })
public class MessageTest {

  private ByteString defaultUnknownFields;

  @Before
  public void init() {
    defaultUnknownFields = ByteString.of("test".getBytes());
  }

  @Test public void constructor() throws Exception {
    // when
    Message message = new C(5, defaultUnknownFields);

    // then
    assertThat(Whitebox.getInternalState(message, "unknownFields")).isEqualTo(defaultUnknownFields);
  }

  @Test(expected = NullPointerException.class) public void constructorThrown() throws Exception {
    // when
    new C(5, null);

    // then
    fail("Message should throw NullPointerException when try to create new instance with null as unknown fields");
  }

  @Test public void unknownFields() throws Exception {
    // when
    Message message = new C(5, defaultUnknownFields);
    Message nullFieldsMessage = new C(15, defaultUnknownFields);
    Whitebox.setInternalState(nullFieldsMessage, "unknownFields", (ByteString)null);

    // then
    assertThat(message.unknownFields()).isEqualTo(defaultUnknownFields);
    assertThat(nullFieldsMessage.unknownFields()).isEqualTo(ByteString.EMPTY);
  }

  @Test public void staticCopyOf() throws Exception {
    // when
    List list = Whitebox.invokeMethod(Message.class, "copyOf", "test", Arrays.asList(1, 3, 5, 7));
    List emptyList = Whitebox.invokeMethod(Message.class, "copyOf", "test", Collections.EMPTY_LIST);
    List immutableList = Whitebox.invokeMethod(Message.class, "copyOf", "test",
      new ImmutableList<>(Arrays.asList("one", "two", "three")));

    // then
    assertThat(list instanceof ArrayList).isTrue();
    assertThat(list).isEqualTo(Arrays.asList(1, 3, 5, 7));

    assertThat(emptyList instanceof MutableOnWriteList).isTrue();
    assertThat(emptyList).isEmpty();

    assertThat(immutableList instanceof MutableOnWriteList).isTrue();
    assertThat(immutableList).isEqualTo(Arrays.asList("one", "two", "three"));
  }


  @Test(expected = NullPointerException.class) public void staticCopyOfNull() throws Exception {
    // when
    Whitebox.invokeMethod(Message.class, "copyOf", "test", null);

    // then
    fail("Message.Builder should throw NullPointerException when copy null list");
  }

  @Test public void builderAddUnknownFieldsRepeatable() throws Exception {
    // given
    ByteString byteString = ByteString.encodeUtf8("test");
    ProtoWriter protoWriter = mock(ProtoWriter.class);
    whenNew(ProtoWriter.class).withArguments(Mockito.any(Buffer.class)).thenReturn(protoWriter);

    C.Builder builder = new C.Builder();

    // when
    builder.addUnknownFields(byteString);
    builder.addUnknownFields(byteString);

    // then
    Mockito.verify(protoWriter, Mockito.times(2)).writeBytes(byteString);
  }

  @Test public void staticEquals() throws Exception {
    assertThat(Whitebox.invokeMethod(Message.class, "equals", 1, 1)).isEqualTo(true);
    assertThat(Whitebox.invokeMethod(Message.class, "equals", new Object(), new Object())).isEqualTo(false);
    assertThat(Whitebox.invokeMethod(Message.class, "equals", "boo", "boo")).isEqualTo(true);
    assertThat(Whitebox.invokeMethod(Message.class, "equals", "boo", new String("boo"))).isEqualTo(true);
    assertThat(Whitebox.invokeMethod(Message.class, "equals", (Object)null, null)).isEqualTo(true);
    assertThat(Whitebox.invokeMethod(Message.class, "equals", (Object)null, 0)).isEqualTo(false);
    assertThat(Whitebox.invokeMethod(Message.class, "equals", 0, null)).isEqualTo(false);
  }


  @Test public void staticCountNonNull() throws Exception {
    assertThat(Whitebox.invokeMethod(Message.class, "countNonNull", (Object)null, null, null, null)).isEqualTo(0);
    assertThat(Whitebox.invokeMethod(Message.class, "countNonNull", (Object)null, null, null, new Object())).isEqualTo(1);
    assertThat(Whitebox.invokeMethod(Message.class, "countNonNull", (Object)null, null, 1, null)).isEqualTo(1);
    assertThat(Whitebox.invokeMethod(Message.class, "countNonNull", (Object)null, 1, null, null)).isEqualTo(1);
    assertThat(Whitebox.invokeMethod(Message.class, "countNonNull", 1, null, null, null)).isEqualTo(1);
    assertThat(Whitebox.invokeMethod(Message.class, "countNonNull", 1, 0, Integer.MAX_VALUE, new Object())).isEqualTo(4);
    assertThat(Whitebox.invokeMethod(Message.class, "countNonNull", (Object)null, null, null, null, null, null)).isEqualTo(0);
    assertThat(Whitebox.invokeMethod(Message.class, "countNonNull", (Object)null, null, null, null, null, new Object())).isEqualTo(1);
    assertThat(Whitebox.invokeMethod(Message.class, "countNonNull", 1, 0, Integer.MAX_VALUE, Integer.MIN_VALUE, "test", new Object())).isEqualTo(6);
  }

  @Test public void builderAddUnknownFieldsIOError() throws Exception {
    // given
    ByteString byteString = ByteString.encodeUtf8("test");

    ProtoWriter protoWriter = mock(ProtoWriter.class);
    whenNew(ProtoWriter.class).withArguments(Mockito.any(Buffer.class)).thenReturn(protoWriter);
    PowerMockito.when(protoWriter, "writeBytes", byteString).thenThrow(new IOException());

      // when
    try {
      new C.Builder().addUnknownFields(byteString);
    }
    catch (AssertionError e) {
      return;
    }

    // then
    fail("Message.Builder should throw AssertionError when error occurs during I/O operations");
  }


  @Test public void builderAddUnknownFieldIOError() throws Exception {
    // given
    ProtoWriter protoWriter = mock(ProtoWriter.class);
    whenNew(ProtoWriter.class).withArguments(Mockito.any(Buffer.class)).thenReturn(protoWriter);
    PowerMockito.when(protoWriter, "writeTag", 1, FieldEncoding.FIXED32).thenThrow(new IOException());

    // when
    try {
      new C.Builder().addUnknownField(1, FieldEncoding.FIXED32, 10);
    }
    catch (AssertionError e) {
      return;
    }

    // then
    fail("Message.Builder should throw AssertionError when error occurs during I/O operations");
  }

}
