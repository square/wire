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

public final class TypeNameTest {
  @Test public void getScalar() throws Exception {
    assertThat(Type.Name.getScalar("int32")).isSameAs(Type.Name.INT32);
  }

  @Test public void scalarToString() throws Exception {
    assertThat(Type.Name.INT32.toString()).isEqualTo("int32");
    assertThat(Type.Name.STRING.toString()).isEqualTo("string");
    assertThat(Type.Name.BYTES.toString()).isEqualTo("bytes");
  }

  @Test public void messageToString() throws Exception {
    Type.Name person = Type.Name.get("squareup.protos.person", "Person");
    assertThat(person.toString()).isEqualTo("squareup.protos.person.Person");

    Type.Name phoneType = person.nestedType("PhoneType");
    assertThat(phoneType.toString()).isEqualTo("squareup.protos.person.Person.PhoneType");
  }

  @Test public void enclosingTypeName() throws Exception {
    assertThat(Type.Name.STRING.enclosingTypeName()).isNull();

    Type.Name person = Type.Name.get("squareup.protos.person", "Person");
    assertThat(person.enclosingTypeName()).isNull();

    Type.Name phoneType = person.nestedType("PhoneType");
    assertThat(phoneType.enclosingTypeName()).isEqualTo(person);
  }

  @Test public void isScalar() throws Exception {
    assertThat(Type.Name.INT32.isScalar()).isTrue();
    assertThat(Type.Name.STRING.isScalar()).isTrue();
    assertThat(Type.Name.BYTES.isScalar()).isTrue();
    assertThat(Type.Name.get("squareup.protos.person", "Person").isScalar()).isFalse();
  }

  @Test public void isPackableScalar() throws Exception {
    assertThat(Type.Name.INT32.isPackableScalar()).isTrue();
    assertThat(Type.Name.STRING.isPackableScalar()).isFalse();
    assertThat(Type.Name.BYTES.isPackableScalar()).isFalse();
    assertThat(Type.Name.get("squareup.protos.person", "Person").isPackableScalar()).isFalse();
  }
}
