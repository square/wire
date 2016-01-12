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
    // when
    defaultMarkSet.root(ProtoType.get("A"));
    defaultMarkSet.mark(ProtoMember.get("A#a"));
    defaultMarkSet.mark(ProtoMember.get("A#b"));
    defaultMarkSet.mark(ProtoMember.get("C#c"));
    defaultMarkSet.mark(ProtoType.get("B"));

    // then
    assertThat(defaultMarkSet.containsAllMembers(ProtoType.get("A"))).isTrue();
    assertThat(defaultMarkSet.containsAllMembers(ProtoType.get("B"))).isTrue();
    assertThat(defaultMarkSet.containsAllMembers(ProtoType.get("C"))).isTrue();
    assertThat(defaultMarkSet.containsAllMembers(ProtoType.get("D"))).isFalse();
  }

  @Test public void nullContainsAllMembers() throws Exception {
    try {
      // when
      defaultMarkSet.containsAllMembers(null);

      // then
      fail("MarkSet should throw NullPointerException when null is passed to be verified as members container");
    }
    catch (NullPointerException e) {
      assertThat(e).hasMessage("type == null");
    }
  }

  @Test public void nullProtoTypeRoot() throws Exception {
    try {
      // when
      defaultMarkSet.root((ProtoType) null);

      // then
      fail("MarkSet should throw NullPointerException when null is passed as root");
    }
    catch (NullPointerException e) {
      assertThat(e).hasMessage("type == null");
    }
  }

  @Test public void nullProtoMemberRoot() throws Exception {
    try {
      // when
      defaultMarkSet.root((ProtoMember) null);

      // then
      fail("MarkSet should throw NullPointerException when null is passed as root");
    }
    catch (NullPointerException e) {
      assertThat(e).hasMessage("protoMember == null");
    }
  }

  @Test public void nullProtoTypeMark() throws Exception {
    try {
      // when
      defaultMarkSet.mark((ProtoType) null);

      // then
      fail("MarkSet should throw NullPointerException when null is passed to mark");
    }
    catch (NullPointerException e) {
      assertThat(e).hasMessage("type == null");
    }
  }

  @Test public void nullProtoMemberMark() throws Exception {
    try {
      // when
      defaultMarkSet.mark((ProtoMember) null);

      // then
      fail("MarkSet should throw NullPointerException when null is passed to mark");
    }
    catch (NullPointerException e) {
      assertThat(e).hasMessage("type == null");
    }
  }

  @Test public void nullProtoTypeContains() throws Exception {
    try {
      // when
      defaultMarkSet.contains((ProtoType) null);

      // then
      fail("MarkSet should throw NullPointerException when null is checked to be contained");
    }
    catch (NullPointerException e) {
      assertThat(e).hasMessage("type == null");
    }
  }

  @Test public void nullProtoMemberContains() throws Exception {
    try {
      // when
      defaultMarkSet.contains((ProtoMember) null);

      // then
      fail("MarkSet should throw NullPointerException when null is checked to be contained");
    }
    catch (NullPointerException e) {
      assertThat(e).hasMessage("protoMember == null");
    }
  }

  @Test public void excludedProtoTypeRoot() throws Exception {
    try {
      // when
      new MarkSet(new IdentifierSet.Builder().exclude("A").build()).root(ProtoType.get("A"));

      // then
      fail("MarkSet should throw IllegalArgumentException when excluded value is passed as root");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isNull();
    }
  }

  @Test public void excludedProtoMemberRoot() throws Exception {
    try {
      // when
      new MarkSet(new IdentifierSet.Builder().exclude("A").build()).root(ProtoMember.get("A#a"));

      // then
      fail("MarkSet should throw IllegalArgumentException when excluded value is passed as root");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isNull();
    }
  }

  @Test public void duplicatedProtoTypeRoot() throws Exception {
    // given
    ProtoMember member = ProtoMember.get("A#a");

    try {
      // when
      defaultMarkSet.root(member);
      defaultMarkSet.root(member.type());

      // then
      fail("MarkSet should throw IllegalArgumentException when registered value is passed as root");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isNull();
    }
  }

  @Test public void duplicatedProtoMemberRoot() throws Exception {
    // given
    ProtoMember member = ProtoMember.get("A#a");

    try {
      // when
      defaultMarkSet.root(member.type());
      defaultMarkSet.root(member);

      // then
      fail("MarkSet should throw IllegalArgumentException when registered value is passed as root");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isNull();
    }
  }
}
