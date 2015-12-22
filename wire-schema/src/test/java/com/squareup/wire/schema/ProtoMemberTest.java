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
import static org.assertj.core.api.Assertions.fail;

public final class ProtoMemberTest {

  @Test(expected = IllegalArgumentException.class) public void getIllegal() throws Exception {
    // when
    ProtoMember.get("testmember");

    // then
    fail("Can't get ProtoMember without '#' in path");
  }

  @Test public void notEquals() throws Exception {
    // when
    ProtoMember protoMember1 = ProtoMember.get("a.b#member");
    ProtoMember protoMember2 = ProtoMember.get("a.b#member2");
    ProtoMember protoMember3 = ProtoMember.get("a.b.c#member");
    ProtoMember protoMember4 = ProtoMember.get("a.b.c#");

    // then
    assertThat(protoMember1).isNotEqualTo(protoMember2);
    assertThat(protoMember1).isNotEqualTo(protoMember3);
    assertThat(protoMember2).isNotEqualTo(protoMember3);
    assertThat(protoMember4).isNotEqualTo(ProtoType.get("a.b.c"));
  }

}
