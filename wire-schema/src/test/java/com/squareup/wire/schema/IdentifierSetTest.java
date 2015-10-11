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

public final class IdentifierSetTest {
  @Test public void empty() throws Exception {
    IdentifierSet set = new IdentifierSet();
    assertThat(set.contains(ProtoType.get("a.b.Message"))).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isFalse();
  }

  @Test public void typesOnly() throws Exception {
    IdentifierSet set = new IdentifierSet();
    set.add("a.b.Message");
    set.add("a.b.MessageWithSuffix");

    assertThat(set.contains(ProtoType.get("a.b.Message"))).isTrue();
    assertThat(set.containsAllMembers(ProtoType.get("a.b.Message"))).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isTrue();

    assertThat(set.contains(ProtoType.get("a.b.Other"))).isFalse();
    assertThat(set.containsAllMembers(ProtoType.get("a.b.Other"))).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Other"), "member")).isFalse();
  }

  @Test public void membersOnly() throws Exception {
    IdentifierSet set = new IdentifierSet();
    set.add("a.b.Message#member");

    assertThat(set.contains(ProtoType.get("a.b.Message"))).isTrue();
    assertThat(set.containsAllMembers(ProtoType.get("a.b.Message"))).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "other")).isFalse();

    assertThat(set.contains(ProtoType.get("a.b.Other"))).isFalse();
    assertThat(set.containsAllMembers(ProtoType.get("a.b.Other"))).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Other"), "member")).isFalse();
  }

  @Test public void membersAndMessages() throws Exception {
    IdentifierSet set = new IdentifierSet();
    set.add("a.b.Message");
    set.add("a.b.AnotherMessage#member");

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

  @Test public void addIfAbsentAddsIfAbsent() throws Exception {
    IdentifierSet set = new IdentifierSet();
    set.add("a.b.Message#member");
    assertThat(set.addIfAbsent(ProtoType.get("a.b.Message"), "another")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "another")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "other")).isFalse(); // Omitted.
  }

  @Test public void addIfAbsentDoesNotConstrain() throws Exception {
    IdentifierSet set = new IdentifierSet();
    set.add("a.b.Message");
    assertThat(set.addIfAbsent(ProtoType.get("a.b.Message"), "another")).isFalse();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "another")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "other")).isTrue(); // Retained!
  }

  @Test public void addIfAbsentMultipleMembers() throws Exception {
    IdentifierSet set = new IdentifierSet();
    ProtoType message = ProtoType.get("a.b.Message");
    assertThat(set.add(message)).isTrue();
    assertThat(set.addIfAbsent(message, "member")).isFalse();
    assertThat(set.addIfAbsent(message, "another")).isFalse();
    assertThat(set.contains(message)).isTrue();
    assertThat(set.contains(message, "member")).isTrue();
    assertThat(set.contains(message, "another")).isTrue();
    assertThat(set.contains(message, "other")).isTrue();
  }

  @Test public void addMultipleMembers() throws Exception {
    IdentifierSet set = new IdentifierSet();
    assertThat(set.add("a.b.Message#member")).isTrue();
    assertThat(set.add("a.b.Message#another")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"))).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "member")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "another")).isTrue();
    assertThat(set.contains(ProtoType.get("a.b.Message"), "other")).isFalse();
  }
}
