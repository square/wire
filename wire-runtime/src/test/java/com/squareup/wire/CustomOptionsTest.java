/*
 * Copyright 2013 Square Inc.
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

import com.squareup.wire.protos.custom_options.Ext_custom_options;
import com.squareup.wire.protos.custom_options.FooBar;
import com.squareup.wire.protos.custom_options.MessageWithOptions;
import java.math.BigInteger;
import org.junit.Assert;
import org.junit.Test;

public class CustomOptionsTest {

  @Test
  public void testMessageOptions() {
    FooBar option_one =
        MessageWithOptions.MESSAGE_OPTIONS.getExtension(Ext_custom_options.my_message_option_one);
    Assert.assertEquals(new Integer(1234), option_one.foo);
    Assert.assertEquals("5678", option_one.bar);
    FooBar.Nested baz1 = option_one.baz;
    Assert.assertEquals(FooBar.FooBarBazEnum.BAZ, baz1.value);
    long expected = new BigInteger("18446744073709551615").longValue();
    Assert.assertEquals(-1L, expected); // unsigned value in .proto file is wrapped
    Assert.assertEquals(Long.valueOf(expected),
        option_one.qux);
    Assert.assertEquals(123.0F, option_one.fred, 0.0000001);
    Assert.assertEquals(456.0, option_one.daisy, 0.0000001);

    double option_two =
        MessageWithOptions.MESSAGE_OPTIONS.getExtension(Ext_custom_options.my_message_option_two);
    Assert.assertEquals(91011.0F, option_two, 0.0000001);

    FooBar option_three =
        MessageWithOptions.MESSAGE_OPTIONS.getExtension(Ext_custom_options.my_message_option_three);
    Assert.assertEquals(new Integer(11), option_three.foo);
    Assert.assertEquals("22", option_three.bar);
    FooBar.Nested baz3 = option_three.baz;
    Assert.assertEquals(FooBar.FooBarBazEnum.BAR, baz3.value);
  }
}
