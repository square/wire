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
import static org.junit.Assert.fail;

public class MarkSetTest {

  @Test public void containsAllMembers() throws Exception {
    MarkSet markSet = new MarkSet(new IdentifierSet.Builder().build());
    markSet.root(ProtoType.get("A"));
    markSet.mark(ProtoMember.get("A#a"));
    markSet.mark(ProtoMember.get("A#b"));
    markSet.mark(ProtoMember.get("C#c"));
    markSet.mark(ProtoType.get("B"));

    assertThat(markSet.containsAllMembers(ProtoType.get("A"))).isTrue();
    assertThat(markSet.containsAllMembers(ProtoType.get("B"))).isTrue();
    assertThat(markSet.containsAllMembers(ProtoType.get("C"))).isTrue();
    assertThat(markSet.containsAllMembers(ProtoType.get("D"))).isFalse();
  }

  @Test(expected = NullPointerException.class) public void nullContainsAllMembers() throws Exception {
    new MarkSet(new IdentifierSet.Builder().build()).containsAllMembers(null);
    fail("MarkSet should throw NullPointerException when null is passed to be verified as members container");
  }

  @Test(expected = NullPointerException.class) public void nullProtoTypeRoot() throws Exception {
    new MarkSet(new IdentifierSet.Builder().build()).root((ProtoType)null);
    fail("MarkSet should throw NullPointerException when null is passed as root");
  }

  @Test(expected = NullPointerException.class) public void nullProtoMemberRoot() throws Exception {
    new MarkSet(new IdentifierSet.Builder().build()).root((ProtoMember)null);
    fail("MarkSet should throw NullPointerException when null is passed as root");
  }

  @Test(expected = NullPointerException.class) public void nullProtoTypeMark() throws Exception {
    new MarkSet(new IdentifierSet.Builder().build()).mark((ProtoType) null);
    fail("MarkSet should throw NullPointerException when null is passed to mark");
  }

  @Test(expected = NullPointerException.class) public void nullProtoMemberMark() throws Exception {
    new MarkSet(new IdentifierSet.Builder().build()).mark((ProtoMember) null);
    fail("MarkSet should throw NullPointerException when null is passed to mark");
  }

  @Test(expected = NullPointerException.class) public void nullProtoTypeContains() throws Exception {
    new MarkSet(new IdentifierSet.Builder().build()).contains((ProtoType) null);
    fail("MarkSet should throw NullPointerException when null is checked to be contained");
  }

  @Test(expected = NullPointerException.class) public void nullProtoMemberContains() throws Exception {
    new MarkSet(new IdentifierSet.Builder().build()).contains((ProtoMember) null);
    fail("MarkSet should throw NullPointerException when null is checked to be contained");
  }

  @Test(expected = IllegalArgumentException.class) public void excludedProtoTypeRoot() throws Exception {
    new MarkSet(new IdentifierSet.Builder().exclude("A").build()).root(ProtoType.get("A"));
    fail("MarkSet should throw IllegalArgumentException when excluded value is passed as root");
  }

  @Test(expected = IllegalArgumentException.class) public void excludedProtoMemberRoot() throws Exception {
    new MarkSet(new IdentifierSet.Builder().exclude("A").build()).root(ProtoMember.get("A#a"));
    fail("MarkSet should throw IllegalArgumentException when excluded value is passed as root");
  }

  @Test(expected = IllegalArgumentException.class) public void duplicatedProtoTypeRoot() throws Exception {
    ProtoMember member = ProtoMember.get("A#a");
    MarkSet markSet = new MarkSet(new IdentifierSet.Builder().build());

    markSet.root(member);
    markSet.root(member.type());

    fail("MarkSet should throw IllegalArgumentException when registered value is passed as root");
  }

  @Test(expected = IllegalArgumentException.class) public void duplicatedProtoMemberRoot() throws Exception {
    ProtoMember member = ProtoMember.get("A#a");
    MarkSet markSet = new MarkSet(new IdentifierSet.Builder().build());
    markSet.root(member.type());
    markSet.root(member);

    fail("MarkSet should throw IllegalArgumentException when registered value is passed as root");
  }

}
