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

import java.net.ProtocolException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class MessageTest {

  private ByteString defaultUnknownFields;

  @Before
  public void init() {
    defaultUnknownFields = ByteString.of("test".getBytes());
  }

  @Test public void constructorThrown() {
    try {
      // when
      new C(5, null);

      // then
      fail("Message should throw NullPointerException when try to create new instance with null as unknown fields");
    }
    catch (NullPointerException e) {
      assertThat(e).hasMessage("unknownFields == null");
    }
  }

  @Test public void unknownFields() {
    // when
    Message message = new C(5, defaultUnknownFields);

    // then
    assertThat(message.unknownFields()).isEqualTo(defaultUnknownFields);
  }
}
