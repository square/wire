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
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Field.class })
public final class ProtoMemberTest {

  @Test public void get() throws Exception {
    // given
    ProtoType protoType = ProtoType.get("a.b");

    Field extensionField = mock(Field.class);
    when(extensionField.isExtension()).thenReturn(true);
    when(extensionField.qualifiedName()).thenReturn("member");
    when(extensionField.name()).thenReturn("notAMember");

    Field notExtensionField = mock(Field.class);
    when(notExtensionField.isExtension()).thenReturn(false);
    when(notExtensionField.qualifiedName()).thenReturn("notAMember");
    when(notExtensionField.name()).thenReturn("member");

    Field illegalField = mock(Field.class);
    when(illegalField.isExtension()).thenReturn(true);
    when(illegalField.qualifiedName()).thenReturn("notAMember");
    when(illegalField.name()).thenReturn("member");

    // when
    ProtoMember protoMember = ProtoMember.get("a.b#member");

    // then
    assertThat(protoMember.type()).isEqualTo(protoType);
    assertThat(protoMember.member()).isEqualTo("member");
    assertThat(ProtoMember.get(protoType, "member")).isEqualTo(protoMember);

    assertThat(ProtoMember.get(protoType, extensionField)).isEqualTo(protoMember);
    assertThat(ProtoMember.get(protoType, notExtensionField)).isEqualTo(protoMember);
    assertThat(ProtoMember.get(protoType, illegalField)).isNotEqualTo(protoMember);
  }

  @Test(expected = IllegalArgumentException.class) public void getIllegal() throws Exception {
    ProtoMember.get("testmember");
  }

  @Test public void notEquals() throws Exception {
    ProtoMember protoMember1 = ProtoMember.get("a.b#member");
    ProtoMember protoMember2 = ProtoMember.get("a.b#member2");
    ProtoMember protoMember3 = ProtoMember.get("a.b.c#member");
    ProtoMember protoMember4 = ProtoMember.get("a.b.c#");

    assertThat(protoMember1).isNotEqualTo(protoMember2);
    assertThat(protoMember1).isNotEqualTo(protoMember3);
    assertThat(protoMember2).isNotEqualTo(protoMember3);
    assertThat(protoMember4).isNotEqualTo(ProtoType.get("a.b.c"));
  }

}
