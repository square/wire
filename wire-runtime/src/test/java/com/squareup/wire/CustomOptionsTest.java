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

import com.squareup.wire.protos.custom_options.FooBar;
import com.squareup.wire.protos.custom_options.MessageWithOptions;
import com.squareup.wire.protos.foreign.ForeignMessage;
import java.math.BigInteger;
import org.assertj.core.data.Offset;
import org.junit.Test;

import static com.squareup.wire.protos.custom_options.FooBar.FooBarBazEnum.BAR;
import static com.squareup.wire.protos.custom_options.FooBar.FooBarBazEnum.BAZ;
import static com.squareup.wire.protos.custom_options.FooBar.FooBarBazEnum.FOO;
import static org.assertj.core.api.Assertions.assertThat;

public class CustomOptionsTest {

  @Test
  public void testMessageOptions() {
    FooBar option_one = MessageWithOptions.MESSAGE_OPTIONS.my_message_option_one;
    assertThat(option_one.foo).isEqualTo(new Integer(1234));
    assertThat(option_one.bar).isEqualTo("5678");
    FooBar.Nested baz1 = option_one.baz;
    assertThat(baz1.value).isEqualTo(BAZ);
    long expected = new BigInteger("18446744073709551615").longValue();
    assertThat(expected).isEqualTo(-1L);
    assertThat(option_one.qux).isEqualTo(Long.valueOf(expected));
    assertThat(option_one.fred).hasSize(2);
    assertThat(option_one.fred.get(0)).isEqualTo(123.0F, Offset.offset(0.0000001f));
    assertThat(option_one.fred.get(1)).isEqualTo(321f, Offset.offset(0.0000001f));
    assertThat(option_one.daisy).isEqualTo(456.0, Offset.offset(0.0000001));

    double option_two = MessageWithOptions.MESSAGE_OPTIONS.my_message_option_two;
    assertThat(option_two).isEqualTo(91011.0, Offset.offset(0.0000001));

    FooBar option_three = MessageWithOptions.MESSAGE_OPTIONS.my_message_option_three;
    assertThat(option_three.foo).isEqualTo(new Integer(11));
    assertThat(option_three.bar).isEqualTo("22");
    FooBar.Nested baz3 = option_three.baz;
    assertThat(baz3.value).isEqualTo(BAR);
    assertThat(option_three.fred).hasSize(2);
    assertThat(option_three.fred.get(0)).isEqualTo(444.0F, Offset.offset(0.0000001f));
    assertThat(option_three.fred.get(1)).isEqualTo(555.0F, Offset.offset(0.0000001f));
    assertThat(option_three.nested).isNotNull();
    assertThat(option_three.nested).hasSize(1);
    assertThat(option_three.nested.get(0).foo).isEqualTo(new Integer(33));

    ForeignMessage foreign_option = MessageWithOptions.MESSAGE_OPTIONS.foreign_message_option;
    assertThat(foreign_option.i).isEqualTo(new Integer(9876));

    FooBar.FooBarBazEnum option_four = MessageWithOptions.MESSAGE_OPTIONS.my_message_option_four;
    assertThat(option_four).isEqualTo(FOO);

    FooBar option_five = MessageWithOptions.MESSAGE_OPTIONS.my_message_option_five;
    assertThat(option_five.ext).isEqualTo(BAZ);
    assertThat(option_five.rep).containsExactly(FOO, BAZ);

    assertThat(FooBar.FooBarBazEnum.FOO.enum_value_option).isEqualTo(new Integer(17));
    assertThat(FooBar.FooBarBazEnum.FOO.complex_enum_value_option.serial).containsExactly(99, 199);
    assertThat(FooBar.FooBarBazEnum.FOO.foreign_enum_value_option).isNull();
    assertThat(FooBar.FooBarBazEnum.BAR.enum_value_option).isNull();
    assertThat(FooBar.FooBarBazEnum.BAR.foreign_enum_value_option).isTrue();
    assertThat(FooBar.FooBarBazEnum.BAZ.enum_value_option).isEqualTo(new Integer(18));
    assertThat(FooBar.FooBarBazEnum.BAZ.foreign_enum_value_option).isFalse();

    assertThat(FooBar.FooBarBazEnum.ENUM_OPTIONS.enum_option).isTrue();
  }
}
