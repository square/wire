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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class MarkSetTest {
  @Test public void emptyIncludesContainsEverything() throws Exception {
    MarkSet set = new MarkSet(new IdentifierSet.Builder().build());
    assertThat(set.contains(ProtoType.get("a.b.Message"))).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isTrue();
  }

  @Test public void includeTypesOnly() throws Exception {
    MarkSet set = new MarkSet(new IdentifierSet.Builder()
        .include("a.b.Message")
        .include("a.b.MessageWithSuffix")
        .build());

    assertThat(set.contains(ProtoType.get("a.b.Message"))).isTrue();
    assertThat(set.containsAllMembers(ProtoType.get("a.b.Message"))).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isTrue();

    assertThat(set.contains(ProtoType.get("a.b.Other"))).isFalse();
    assertThat(set.containsAllMembers(ProtoType.get("a.b.Other"))).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Other"), "member")).isFalse();
  }

  @Test public void includeMembersOnly() throws Exception {
    MarkSet set = new MarkSet(new IdentifierSet.Builder()
        .include("a.b.Message#member")
        .build());

    assertThat(set.contains(ProtoType.get("a.b.Message"))).isTrue();
    assertThat(set.containsAllMembers(ProtoType.get("a.b.Message"))).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "other")).isFalse();

    assertThat(set.contains(ProtoType.get("a.b.Other"))).isFalse();
    assertThat(set.containsAllMembers(ProtoType.get("a.b.Other"))).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Other"), "member")).isFalse();
  }

  @Test public void includeTypeAndUnrelatedMember() throws Exception {
    MarkSet set = new MarkSet(new IdentifierSet.Builder()
        .include("a.b.Message")
        .include("a.b.AnotherMessage#member")
        .build());

    assertThat(set.contains(ProtoType.get("a.b.Message"))).isTrue();
    assertThat(set.containsAllMembers(ProtoType.get("a.b.Message"))).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isTrue();

    assertThat(set.contains(ProtoType.get("a.b.AnotherMessage"))).isTrue();
    assertThat(set.containsAllMembers(ProtoType.get("a.b.AnotherMessage"))).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.AnotherMessage"), "member")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.AnotherMessage"), "other")).isFalse();

    assertThat(set.contains(ProtoType.get("a.b.Other"))).isFalse();
    assertThat(set.containsAllMembers(ProtoType.get("a.b.Other"))).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Other"), "member")).isFalse();
  }

  @Test public void markAddsIfAbsent() throws Exception {
    MarkSet set = new MarkSet(new IdentifierSet.Builder()
        .include("a.b.Message#member")
        .build());
    assertThat(set.mark(ProtoType.get("a.b.Message"), "another")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "another")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "other")).isFalse(); // Omitted.
  }

  @Test public void markDoesNotConstrain() throws Exception {
    MarkSet set = new MarkSet(new IdentifierSet.Builder()
        .include("a.b.Message")
        .build());
    assertThat(set.mark(ProtoType.get("a.b.Message"), "another")).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "another")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "other")).isTrue(); // Retained!
  }

  @Test public void markMultipleMembers() throws Exception {
    MarkSet set = new MarkSet(new IdentifierSet.Builder().build());
    ProtoType message = ProtoType.get("a.b.Message");
    assertThat(set.mark(message)).isTrue();
    assertThat(set.mark(message, "member")).isFalse();
    assertThat(set.mark(message, "another")).isFalse();
    assertThat(set.contains(message)).isTrue();
    assertThat(set.contains(message, "member")).isTrue();
    assertThat(set.contains(message, "another")).isTrue();
    assertThat(set.contains(message, "other")).isTrue();
  }

  @Test public void includeMultipleMembers() throws Exception {
    MarkSet set = new MarkSet(new IdentifierSet.Builder()
        .include("a.b.Message#member")
        .include("a.b.Message#another")
        .build());
    assertThat(set.contains(ProtoType.get("a.b.Message"))).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "another")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "other")).isFalse();
  }

  @Test public void excludeTypeMarkType() throws Exception {
    MarkSet set = new MarkSet(new IdentifierSet.Builder()
        .exclude("a.b.Message")
        .build());
    assertThat(set.mark(ProtoType.get("a.b.Message"))).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Message"))).isFalse();
  }

  @Test public void excludeTypeMarkMember() throws Exception {
    MarkSet set = new MarkSet(new IdentifierSet.Builder()
        .exclude("a.b.Message")
        .build());
    assertThat(set.mark(ProtoType.get("a.b.Message"), "member")).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Message"))).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isFalse();
  }
}
