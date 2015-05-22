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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OneOfTest {

  @Test
  public void testOneOf() throws Exception {
    OneOfMessage.Builder builder = new OneOfMessage.Builder();
    assertNull(builder.foo);
    assertNull(builder.bar);
    assertEquals(OneOfMessage.Choice.CHOICE_NOT_SET, builder.choice);

    builder.foo(17);
    assertEquals(Integer.valueOf(17), builder.foo);
    assertNull(builder.bar);
    assertEquals(OneOfMessage.Choice.FOO, builder.choice);

    OneOfMessage fooMessage = builder.build();
    assertEquals(Integer.valueOf(17), fooMessage.foo);
    assertNull(fooMessage.bar);
    assertEquals(OneOfMessage.Choice.FOO, fooMessage.choice);

    builder.bar("barbar");
    assertNull(builder.foo);
    assertEquals("barbar", builder.bar);
    assertEquals(OneOfMessage.Choice.BAR, builder.choice);

    OneOfMessage barMessage = builder.build();
    assertNull(barMessage.foo);
    assertEquals("barbar", barMessage.bar);
    assertEquals(OneOfMessage.Choice.BAR, barMessage.choice);

    builder.bar(null);
    assertNull(builder.foo);
    assertNull(builder.bar);
    assertEquals(OneOfMessage.Choice.CHOICE_NOT_SET, builder.choice);

    OneOfMessage noneMessage = builder.build();
    assertNull(noneMessage.foo);
    assertNull(noneMessage.bar);
    assertEquals(OneOfMessage.Choice.CHOICE_NOT_SET, noneMessage.choice);
  }
}
