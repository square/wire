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

public final class ProtoTypeTest {
  @Test public void get() throws Exception {
    assertThat(ProtoType.get("int32")).isSameAs(ProtoType.INT32);
    assertThat(ProtoType.get("Person")).isEqualTo(ProtoType.get("Person"));
    assertThat(ProtoType.get("squareup.protos.person", "Person"))
      .isEqualTo(ProtoType.get("squareup.protos.person.Person"));
    assertThat(ProtoType.get("Person")).isNotEqualTo(ProtoType.get("squareup.protos.person.Person"));
    assertThat(ProtoType.get("int32")).isNotEqualTo("int32");
  }

  @Test public void simpleName() throws Exception {
    ProtoType person = ProtoType.get("squareup.protos.person.Person");
    assertThat(person.simpleName()).isEqualTo("Person");
  }

  @Test public void nullName() throws Exception {
    try {
      // when
      ProtoType.get(null);

      // then
      fail("ProtoType should throw IllegalArgumentException when null is passed as type name");
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("unexpected name: null");
    }
  }

  @Test public void emptyName() throws Exception {
    try {
      // when
      ProtoType.get("");

      // then
      fail("ProtoType should throw IllegalArgumentException when empty string is passed as type name");
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("unexpected name: ");
    }
  }

  @Test public void illegalSymbolsName() throws Exception {
    try {
      // when
      ProtoType.get("illegal#Name");

      // then
      fail("ProtoType should throw IllegalArgumentException when string with illegal symbols is passed as type name");
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("unexpected name: illegal#Name");
    }
  }

  @Test public void scalarToString() throws Exception {
    assertThat(ProtoType.INT32.toString()).isEqualTo("int32");
    assertThat(ProtoType.STRING.toString()).isEqualTo("string");
    assertThat(ProtoType.BYTES.toString()).isEqualTo("bytes");
  }

  @Test public void nestedType() throws Exception {
    assertThat(ProtoType.get("squareup.protos.person.Person").nestedType("PhoneType"))
      .isEqualTo(ProtoType.get("squareup.protos.person.Person.PhoneType"));
  }

  @Test public void nullNestedType() throws Exception {
    try {
      // when
      ProtoType.get("squareup.protos.person.Person").nestedType(null);

      // then
      fail("ProtoType should throw IllegalArgumentException when null is passed as nested type");
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("unexpected name: null");
    }
  }

  @Test public void emptyNestedType() throws Exception {
    try {
      // when
      ProtoType.get("squareup.protos.person.Person").nestedType("");

      // then
      fail("ProtoType should throw IllegalArgumentException when empty string is passed as nested type");
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("unexpected name: ");
    }
  }

  @Test public void illegalSymbolsNestedType() throws Exception {
    try {
      // when
      ProtoType.get("squareup.protos.person.Person").nestedType("Nested.Type");

      // then
      fail("ProtoType should throw IllegalArgumentException when string with illegal symbols is passed as nested type");
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("unexpected name: Nested.Type");
    }
  }

  @Test public void primitivesCannotNest() throws Exception {
    try {
      ProtoType.INT32.nestedType("PhoneType");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Test public void messageToString() throws Exception {
    ProtoType person = ProtoType.get("squareup.protos.person.Person");
    assertThat(person.toString()).isEqualTo("squareup.protos.person.Person");

    ProtoType phoneType = person.nestedType("PhoneType");
    assertThat(phoneType.toString()).isEqualTo("squareup.protos.person.Person.PhoneType");
  }

  @Test public void enclosingTypeOrPackage() throws Exception {
    assertThat(ProtoType.STRING.enclosingTypeOrPackage()).isNull();

    ProtoType person = ProtoType.get("squareup.protos.person.Person");
    assertThat(person.enclosingTypeOrPackage()).isEqualTo("squareup.protos.person");

    ProtoType phoneType = person.nestedType("PhoneType");
    assertThat(phoneType.enclosingTypeOrPackage()).isEqualTo("squareup.protos.person.Person");
  }

  @Test public void isScalar() throws Exception {
    assertThat(ProtoType.INT32.isScalar()).isTrue();
    assertThat(ProtoType.STRING.isScalar()).isTrue();
    assertThat(ProtoType.BYTES.isScalar()).isTrue();
    assertThat(ProtoType.get("squareup.protos.person.Person").isScalar()).isFalse();
  }
}
