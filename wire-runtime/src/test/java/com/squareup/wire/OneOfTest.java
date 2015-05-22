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

import com.squareup.wire.protos.oneof.OneOfMessage;

import org.junit.Test;

import java.io.IOException;

import static com.squareup.wire.protos.oneof.OneOfMessage.Choice.BAR;
import static com.squareup.wire.protos.oneof.OneOfMessage.Choice.CHOICE_NOT_SET;
import static com.squareup.wire.protos.oneof.OneOfMessage.Choice.FOO;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class OneOfTest {

  private static final Wire wire = new Wire();

  private static final byte[] INITIAL_BYTES = {};
  // (Tag #1 << 3 | VARINT) = 8.
  private static final byte[] FOO_BYTES = { 8, 17 };
  // (Tag #3 << 3 | LENGTH_DELIMITED) = 26, string length = 6.
  private static final byte[] BAR_BYTES = { 26, 6, 'b', 'a', 'r', 'b', 'a', 'r'};


  @Test
  public void testOneOf() throws Exception {
    OneOfMessage.Builder builder = new OneOfMessage.Builder();
    validate(builder, null, null, CHOICE_NOT_SET, INITIAL_BYTES);

    builder.foo(17);
    validate(builder, 17, null, FOO, FOO_BYTES);

    builder.bar("barbar");
    validate(builder, null, "barbar", BAR, BAR_BYTES);

    builder.bar(null);
    validate(builder, null, null, CHOICE_NOT_SET, INITIAL_BYTES);

    builder.bar("barbar");
    validate(builder, null, "barbar", BAR, BAR_BYTES);

    builder.foo(17);
    validate(builder, 17, null, FOO, FOO_BYTES);

    builder.foo(null);
    validate(builder, null, null, CHOICE_NOT_SET, INITIAL_BYTES);
  }

  private void validate(OneOfMessage.Builder builder, Integer expectedFoo, String expectedBar,
      OneOfMessage.Choice expectedChoice, byte[] expectedBytes) throws IOException {
    // Check builder fields
    assertEquals(expectedFoo, builder.foo);
    assertEquals(expectedBar, builder.bar);
    assertEquals(expectedChoice, builder.choice);

    // Check message fields.
    OneOfMessage message = builder.build();
    assertEquals(expectedFoo, message.foo);
    assertEquals(expectedBar, message.bar);
    assertEquals(expectedChoice, message.choice);

    // Check serialized bytes.
    byte[] bytes = message.toByteArray();
    assertArrayEquals(expectedBytes, bytes);

    // Check result of deserialization.
    OneOfMessage newMessage = wire.parseFrom(bytes, OneOfMessage.class);
    assertEquals(expectedFoo, newMessage.foo);
    assertEquals(expectedBar, newMessage.bar);
    assertEquals(expectedChoice, newMessage.choice);
  }
}
