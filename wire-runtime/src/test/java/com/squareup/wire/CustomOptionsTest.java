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
import com.squareup.wire.protos.foreign.Ext_foreign;
import com.squareup.wire.protos.foreign.ForeignMessage;
import java.math.BigInteger;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

import static com.squareup.wire.protos.custom_options.FooBar.FooBarBazEnum.FOO;
import static com.squareup.wire.protos.custom_options.FooBar.FooBarBazEnum.BAR;
import static com.squareup.wire.protos.custom_options.FooBar.FooBarBazEnum.BAZ;

public class CustomOptionsTest {

  @Test
  public void testMessageOptions() {
    FooBar option_one =
        MessageWithOptions.MESSAGE_OPTIONS.getExtension(Ext_custom_options.my_message_option_one);
    Assert.assertEquals(new Integer(1234), option_one.foo);
    Assert.assertEquals("5678", option_one.bar);
    FooBar.Nested baz1 = option_one.baz;
    Assert.assertEquals(BAZ, baz1.value);
    long expected = new BigInteger("18446744073709551615").longValue();
    Assert.assertEquals(-1L, expected); // unsigned value in .proto file is wrapped
    Assert.assertEquals(Long.valueOf(expected),
        option_one.qux);
    Assert.assertEquals(2, option_one.fred.size());
    Assert.assertEquals(123.0F, option_one.fred.get(0), 0.0000001);
    Assert.assertEquals(321, option_one.fred.get(1), 0.0000001);
    Assert.assertEquals(456.0, option_one.daisy, 0.0000001);

    double option_two =
        MessageWithOptions.MESSAGE_OPTIONS.getExtension(Ext_custom_options.my_message_option_two);
    Assert.assertEquals(91011.0F, option_two, 0.0000001);

    FooBar option_three =
        MessageWithOptions.MESSAGE_OPTIONS.getExtension(Ext_custom_options.my_message_option_three);
    Assert.assertEquals(new Integer(11), option_three.foo);
    Assert.assertEquals("22", option_three.bar);
    FooBar.Nested baz3 = option_three.baz;
    Assert.assertEquals(BAR, baz3.value);
    Assert.assertEquals(2, option_three.fred.size());
    Assert.assertEquals(444.0F, option_three.fred.get(0), 0.0000001);
    Assert.assertEquals(555.0F, option_three.fred.get(1), 0.0000001);
    Assert.assertNotNull(option_three.nested);
    Assert.assertEquals(1, option_three.nested.size());
    Assert.assertEquals(new Integer(33), option_three.nested.get(0).foo);

    ForeignMessage foreign_option =
      MessageWithOptions.MESSAGE_OPTIONS.getExtension(Ext_foreign.foreign_message_option);
    Assert.assertEquals(new Integer(9876), foreign_option.i);

    FooBar.FooBarBazEnum option_four =
        MessageWithOptions.MESSAGE_OPTIONS.getExtension(Ext_custom_options.my_message_option_four);
    Assert.assertEquals(FOO, option_four);

    FooBar option_five =
        MessageWithOptions.MESSAGE_OPTIONS.getExtension(Ext_custom_options.my_message_option_five);
    Assert.assertEquals(BAZ, option_five.getExtension(Ext_custom_options.ext));
    Assert.assertEquals(Arrays.asList(FOO, BAZ),
        option_five.getExtension(Ext_custom_options.rep));

    Assert.assertEquals(new Integer(17), FooBar.FooBarBazEnum.FOO.enum_value_option);
    Assert.assertEquals(Arrays.asList(99, 199), FooBar.FooBarBazEnum.FOO.complex_enum_value_option.serial);
    Assert.assertEquals(null, FooBar.FooBarBazEnum.FOO.foreign_enum_value_option);
    Assert.assertEquals(null, FooBar.FooBarBazEnum.BAR.enum_value_option);
    Assert.assertEquals(Boolean.TRUE, FooBar.FooBarBazEnum.BAR.foreign_enum_value_option);
    Assert.assertEquals(new Integer(18), FooBar.FooBarBazEnum.BAZ.enum_value_option);
    Assert.assertEquals(Boolean.FALSE, FooBar.FooBarBazEnum.BAZ.foreign_enum_value_option);

    Assert.assertEquals(Boolean.TRUE,
        FooBar.FooBarBazEnum.ENUM_OPTIONS.getExtension(Ext_custom_options.enum_option));
  }
}
