/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema;

import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public final class OptionsTest {
  @Test public void structuredAndUnstructuredOptions() throws Exception {
    // From https://developers.google.com/protocol-buffers/docs/proto#options
    Schema schema = new SchemaBuilder()
        .add("foo.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "message FooOptions {\n"
            + "  optional int32 opt1 = 1;\n"
            + "  optional string opt2 = 2;\n"
            + "}\n"
            + "\n"
            + "extend google.protobuf.FieldOptions {\n"
            + "  optional FooOptions foo_options = 1234;\n"
            + "}\n"
            + "\n"
            + "message Bar {\n"
            + "  optional int32 a = 1 [(foo_options).opt1 = 123, (foo_options).opt2 = \"baz\"];\n"
            + "  optional int32 b = 2 [(foo_options) = { opt1: 456 opt2: \"quux\" }];\n"
            + "}\n")
        .build();

    ProtoMember fooOptions = ProtoMember.get(Options.FIELD_OPTIONS, "foo_options");
    ProtoMember opt1 = ProtoMember.get(ProtoType.get("FooOptions"), "opt1");
    ProtoMember opt2 = ProtoMember.get(ProtoType.get("FooOptions"), "opt2");

    MessageType bar = (MessageType) schema.getType("Bar");
    assertThat(bar.field("a").options().map()).isEqualTo(ImmutableMap.of(
        fooOptions, ImmutableMap.of(opt1, "123", opt2, "baz")));
    assertThat(bar.field("b").options().map()).isEqualTo(ImmutableMap.of(
        fooOptions, ImmutableMap.of(opt1, "456", opt2, "quux")));
  }

  @Test public void textFormatCanOmitMapValueSeparator() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("foo.proto", ""
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "message FooOptions {\n"
            + "  optional BarOptions bar = 2;\n"
            + "}\n"
            + "message BarOptions {\n"
            + "  optional int32 baz = 2;\n"
            + "}\n"
            + "\n"
            + "extend google.protobuf.FieldOptions {\n"
            + "  optional FooOptions foo = 1234;\n"
            + "}\n"
            + "\n"
            + "message Message {\n"
            + "  optional int32 b = 2 [(foo) = { bar { baz: 123 } }];\n"
            + "}\n")
        .build();

    ProtoMember foo = ProtoMember.get(Options.FIELD_OPTIONS, "foo");
    ProtoMember bar = ProtoMember.get(ProtoType.get("FooOptions"), "bar");
    ProtoMember baz = ProtoMember.get(ProtoType.get("BarOptions"), "baz");

    MessageType message = (MessageType) schema.getType("Message");
    assertThat(message.field("b").options().map()).isEqualTo(ImmutableMap.of(
        foo, ImmutableMap.of(bar, ImmutableMap.of(baz, "123"))));
  }

  @Test public void fullyQualifiedOptionFields() throws Exception {
    Schema schema = new SchemaBuilder()
        .add("a/b/more_options.proto", ""
            + "syntax = \"proto2\";\n"
            + "package a.b;\n"
            + "\n"
            + "import \"google/protobuf/descriptor.proto\";\n"
            + "\n"
            + "extend google.protobuf.MessageOptions {\n"
            + "  optional MoreOptions more_options = 17000;\n"
            + "}\n"
            + "\n"
            + "message MoreOptions {\n"
            + "  extensions 100 to 200;\n"
            + "}\n")
        .add("a/c/event_more_options.proto", ""
            + "syntax = \"proto2\";\n"
            + "package a.c;\n"
            + "\n"
            + "import \"a/b/more_options.proto\";\n"
            + "\n"
            + "extend a.b.MoreOptions {\n"
            + "  optional EvenMoreOptions even_more_options = 100;\n"
            + "}\n"
            + "\n"
            + "message EvenMoreOptions {\n"
            + "  optional string string_option = 1;\n"
            + "}\n")
        .add("a/d/message.proto", ""
            + "syntax = \"proto2\";\n"
            + "package a.d;\n"
            + "\n"
            + "import \"a/b/more_options.proto\";\n"
            + "import \"a/c/event_more_options.proto\";\n"
            + "\n"
            + "message Message {\n"
            + "  option (a.b.more_options) = {\n"
            + "    [a.c.even_more_options]: {string_option: \"foo\"}\n"
            + "  };\n"
            + "}\n")
        .build();
    ProtoType moreOptionsType = ProtoType.get("a.b.MoreOptions");
    ProtoType evenMoreOptionsType = ProtoType.get("a.c.EvenMoreOptions");
    ProtoMember moreOptions = ProtoMember.get(Options.MESSAGE_OPTIONS, "a.b.more_options");
    ProtoMember evenMoreOptions = ProtoMember.get(moreOptionsType, "a.c.even_more_options");
    ProtoMember stringOption = ProtoMember.get(evenMoreOptionsType, "string_option");
    MessageType message = (MessageType) schema.getType("a.d.Message");
    assertThat(message.options().map()).isEqualTo(
        ImmutableMap.of(moreOptions, ImmutableMap.of(
            evenMoreOptions, ImmutableMap.of(stringOption, "foo"))));
  }

  @Test public void resolveFieldPathMatchesFirstSegment() throws Exception {
    assertThat(asList(Options.resolveFieldPath("a.b.c.d", set("a", "z", "y"))))
        .containsExactly("a", "b", "c", "d");
  }

  @Test public void resolveFieldPathMatchesMultipleSegments() throws Exception {
    assertThat(asList(Options.resolveFieldPath("a.b.c.d", set("a.b", "z.b", "y.b"))))
        .containsExactly("a.b", "c", "d");
  }

  @Test public void resolveFieldPathMatchesAllSegments() throws Exception {
    assertThat(asList(Options.resolveFieldPath("a.b.c.d", set("a.b.c.d", "z.b.c.d"))))
        .containsExactly("a.b.c.d");
  }

  @Test public void resolveFieldPathMatchesOnlySegment() throws Exception {
    assertThat(asList(Options.resolveFieldPath("a", set("a", "b")))).containsExactly("a");
  }

  @Test public void resolveFieldPathDoesntMatch() throws Exception {
    assertThat(Options.resolveFieldPath("a.b", set("c", "d"))).isNull();
  }

  private Set<String> set(String... elements) {
    return new LinkedHashSet<>(asList(elements));
  }
}
