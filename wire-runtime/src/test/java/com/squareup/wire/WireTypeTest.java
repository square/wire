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
package com.squareup.wire;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class WireTypeTest {
  @Test public void get() throws Exception {
    assertThat(WireType.get("int32")).isSameAs(WireType.INT32);
    assertThat(WireType.get("Person")).isEqualTo(WireType.get("Person"));
    assertThat(WireType.get("squareup.protos.person", "Person"))
        .isEqualTo(WireType.get("squareup.protos.person.Person"));
  }

  @Test public void simpleName() throws Exception {
    WireType person = WireType.get("squareup.protos.person.Person");
    assertThat(person.simpleName()).isEqualTo("Person");
  }

  @Test public void scalarToString() throws Exception {
    assertThat(WireType.INT32.toString()).isEqualTo("int32");
    assertThat(WireType.STRING.toString()).isEqualTo("string");
    assertThat(WireType.BYTES.toString()).isEqualTo("bytes");
  }

  @Test public void nestedType() throws Exception {
    assertThat(WireType.get("squareup.protos.person.Person").nestedType("PhoneType"))
        .isEqualTo(WireType.get("squareup.protos.person.Person.PhoneType"));
  }

  @Test public void primitivesCannotNest() throws Exception {
    try {
      WireType.INT32.nestedType("PhoneType");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Test public void messageToString() throws Exception {
    WireType person = WireType.get("squareup.protos.person.Person");
    assertThat(person.toString()).isEqualTo("squareup.protos.person.Person");

    WireType phoneType = person.nestedType("PhoneType");
    assertThat(phoneType.toString()).isEqualTo("squareup.protos.person.Person.PhoneType");
  }

  @Test public void enclosingTypeOrPackage() throws Exception {
    assertThat(WireType.STRING.enclosingTypeOrPackage()).isNull();

    WireType person = WireType.get("squareup.protos.person.Person");
    assertThat(person.enclosingTypeOrPackage()).isEqualTo("squareup.protos.person");

    WireType phoneType = person.nestedType("PhoneType");
    assertThat(phoneType.enclosingTypeOrPackage()).isEqualTo("squareup.protos.person.Person");
  }

  @Test public void isScalar() throws Exception {
    assertThat(WireType.INT32.isScalar()).isTrue();
    assertThat(WireType.STRING.isScalar()).isTrue();
    assertThat(WireType.BYTES.isScalar()).isTrue();
    assertThat(WireType.get("squareup.protos.person.Person").isScalar()).isFalse();
  }
}
