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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class MarkSetTest {

  private MarkSet defaultMarkSet;

  @Before
  public void init() {
    defaultMarkSet = new MarkSet(new IdentifierSet.Builder().build());
  }

  @Test public void containsAllMembers() throws Exception {
    defaultMarkSet.root(ProtoType.get("A"));
    defaultMarkSet.mark(ProtoMember.get("A#a"));
    defaultMarkSet.mark(ProtoMember.get("A#b"));
    defaultMarkSet.mark(ProtoMember.get("C#c"));
    defaultMarkSet.mark(ProtoType.get("B"));

    assertThat(defaultMarkSet.containsAllMembers(ProtoType.get("A"))).isTrue();
    assertThat(defaultMarkSet.containsAllMembers(ProtoType.get("B"))).isTrue();
    assertThat(defaultMarkSet.containsAllMembers(ProtoType.get("C"))).isTrue();
    assertThat(defaultMarkSet.containsAllMembers(ProtoType.get("D"))).isFalse();
  }

  @Test(expected = NullPointerException.class) public void nullContainsAllMembers() throws Exception {
    defaultMarkSet.containsAllMembers(null);
    fail("MarkSet should throw NullPointerException when null is passed to be verified as members container");
  }

  @Test(expected = NullPointerException.class) public void nullProtoTypeRoot() throws Exception {
    defaultMarkSet.root((ProtoType) null);
    fail("MarkSet should throw NullPointerException when null is passed as root");
  }

  @Test(expected = NullPointerException.class) public void nullProtoMemberRoot() throws Exception {
    defaultMarkSet.root((ProtoMember) null);
    fail("MarkSet should throw NullPointerException when null is passed as root");
  }

  @Test(expected = NullPointerException.class) public void nullProtoTypeMark() throws Exception {
    defaultMarkSet.mark((ProtoType) null);
    fail("MarkSet should throw NullPointerException when null is passed to mark");
  }

  @Test(expected = NullPointerException.class) public void nullProtoMemberMark() throws Exception {
    defaultMarkSet.mark((ProtoMember) null);
    fail("MarkSet should throw NullPointerException when null is passed to mark");
  }

  @Test(expected = NullPointerException.class) public void nullProtoTypeContains() throws Exception {
    defaultMarkSet.contains((ProtoType) null);
    fail("MarkSet should throw NullPointerException when null is checked to be contained");
  }

  @Test(expected = NullPointerException.class) public void nullProtoMemberContains() throws Exception {
    defaultMarkSet.contains((ProtoMember) null);
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

    defaultMarkSet.root(member);
    defaultMarkSet.root(member.type());

    fail("MarkSet should throw IllegalArgumentException when registered value is passed as root");
  }

  @Test(expected = IllegalArgumentException.class) public void duplicatedProtoMemberRoot() throws Exception {
    ProtoMember member = ProtoMember.get("A#a");

    defaultMarkSet.root(member.type());
    defaultMarkSet.root(member);

    fail("MarkSet should throw IllegalArgumentException when registered value is passed as root");
  }

}
