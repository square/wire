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
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Message.class)
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

//  @Test public void toStringTest() throws Exception {
//    // when
//    // todo Message message = new C(12, defaultUnknownFields);
//
//    // then
//  }

//  @Test public void staticBuilderCopyOf() throws Exception {
//
//    // when
//    List list = Whitebox.invokeMethod(Message.Builder.class, "copyOf", new Class[]{String.class, List.class}, "test", Arrays.asList(1, 3, 5, 7));
//    List emptyList = Whitebox.invokeMethod(Message.Builder.class, "copyOf", "test", Arrays.asList(1, 3, 5, 7));
//    List immutableList = Whitebox.invokeMethod(Message.Builder.class, "copyOf", "test",
//      new ImmutableList<>(Arrays.asList("one", "two", "three")));
//
//    // then
//    assertThat(list instanceof ArrayList).isTrue();
//    assertThat(list).isEqualTo(Arrays.asList(1, 3, 5, 7));
//  }

  //countNonNull
}
