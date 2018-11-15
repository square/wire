/*
 * Copyright (C) 2014 Square, Inc.
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
package com.squareup.wire.schema.internal.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.squareup.wire.schema.internal.parser.OptionElement.Kind.BOOLEAN;
import static com.squareup.wire.schema.internal.parser.OptionElement.Kind.LIST;
import static com.squareup.wire.schema.internal.parser.OptionElement.Kind.MAP;
import static com.squareup.wire.schema.internal.parser.OptionElement.Kind.OPTION;
import static com.squareup.wire.schema.internal.parser.OptionElement.Kind.STRING;
import static org.assertj.core.api.Assertions.assertThat;

public final class OptionElementTest {
  @Test public void simpleToSchema() {
    OptionElement option = OptionElement.create("foo", STRING, "bar");
    String expected = "foo = \"bar\"";
    assertThat(option.toSchema()).isEqualTo(expected);
  }

  @Test public void nestedToSchema() {
    OptionElement option =
        OptionElement.create("foo.boo", OPTION, OptionElement.create("bar", STRING, "baz"), true);
    String expected = "(foo.boo).bar = \"baz\"";
    assertThat(option.toSchema()).isEqualTo(expected);
  }

  @Test public void listToSchema() {
    OptionElement option = OptionElement.create("foo", LIST,
        ImmutableList.of(OptionElement.create("ping", STRING, "pong", true),
            OptionElement.create("kit", STRING, "kat")), true);
    String expected = ""
        + "(foo) = [\n"
        + "  (ping) = \"pong\",\n"
        + "  kit = \"kat\"\n"
        + "]";
    assertThat(option.toSchema()).isEqualTo(expected);
  }

  @Test public void mapToSchema() {
    OptionElement option = OptionElement.create("foo", MAP,
        ImmutableMap.of("ping", "pong", "kit", ImmutableList.of("kat", "kot")));
    String expected = ""
        + "foo = {\n"
        + "  ping: \"pong\",\n"
        + "  kit: [\n"
        + "    \"kat\",\n"
        + "    \"kot\"\n"
        + "  ]\n"
        + "}";
    assertThat(option.toSchema()).isEqualTo(expected);
  }

  @Test public void booleanToSchema() {
    OptionElement option = OptionElement.create("foo", BOOLEAN, "false");
    String expected = "foo = false";
    assertThat(option.toSchema()).isEqualTo(expected);
  }
}
