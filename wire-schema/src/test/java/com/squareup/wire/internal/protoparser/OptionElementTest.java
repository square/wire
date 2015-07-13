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
package com.squareup.wire.internal.protoparser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.squareup.wire.internal.protoparser.OptionElement.Kind.BOOLEAN;
import static com.squareup.wire.internal.protoparser.OptionElement.Kind.LIST;
import static com.squareup.wire.internal.protoparser.OptionElement.Kind.MAP;
import static com.squareup.wire.internal.protoparser.OptionElement.Kind.OPTION;
import static com.squareup.wire.internal.protoparser.OptionElement.Kind.STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assert.fail;

public class OptionElementTest {
  @Test public void nullNameThrows() {
    try {
      OptionElement.create(null, STRING, "Test");
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
  }

  @Test public void nullValueThrows() {
    try {
      OptionElement.create("test", STRING, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("value");
    }
  }

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

  @Test public void optionListToMap() {
    List<OptionElement> options = ImmutableList.of(
        OptionElement.create("foo", STRING, "bar"),
        OptionElement.create("ping", LIST, ImmutableList.of(
            OptionElement.create("kit", STRING, "kat"),
            OptionElement.create("tic", STRING, "tac"),
            OptionElement.create("up", STRING, "down")
        )),
        OptionElement.create("wire", MAP, ImmutableMap.of(
            "omar", "little",
            "proposition", "joe"
        )),
        OptionElement.create("nested.option", OPTION, OptionElement.create("one", STRING, "two")),
        OptionElement.create("nested.option", OPTION, OptionElement.create("three", STRING, "four"))
    );
    Map<String, Object> optionMap = OptionElement.optionsAsMap(options);
    assertThat(optionMap).contains(
        entry("foo", "bar"),
        entry("ping", ImmutableList.of(
            OptionElement.create("kit", STRING, "kat"),
            OptionElement.create("tic", STRING, "tac"),
            OptionElement.create("up", STRING, "down")
        )),
        entry("wire", ImmutableMap.of(
            "omar", "little",
            "proposition", "joe"
        )),
        entry("nested.option", ImmutableMap.of(
            "one", "two",
            "three", "four"
        ))
    );
  }

  @Test public void findInList() {
    OptionElement one = OptionElement.create("one", STRING, "1");
    OptionElement two = OptionElement.create("two", STRING, "2");
    OptionElement three = OptionElement.create("three", STRING, "3");
    List<OptionElement> options = ImmutableList.of(one, two, three);
    assertThat(OptionElement.findByName(options, "one")).isSameAs(one);
    assertThat(OptionElement.findByName(options, "two")).isSameAs(two);
    assertThat(OptionElement.findByName(options, "three")).isSameAs(three);
  }

  @Test public void findInListMissing() {
    OptionElement one = OptionElement.create("one", STRING, "1");
    OptionElement two = OptionElement.create("two", STRING, "2");
    List<OptionElement> options = ImmutableList.of(one, two);
    assertThat(OptionElement.findByName(options, "three")).isNull();
  }

  @Test public void findInListDuplicate() {
    OptionElement one = OptionElement.create("one", STRING, "1");
    OptionElement two = OptionElement.create("two", STRING, "2");
    List<OptionElement> options = ImmutableList.of(one, two, one);
    try {
      OptionElement.findByName(options, "one");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Multiple options match name: one");
    }
  }
}
