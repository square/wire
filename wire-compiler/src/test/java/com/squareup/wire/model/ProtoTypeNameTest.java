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
package com.squareup.wire.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class ProtoTypeNameTest {
  @Test public void getScalar() throws Exception {
    assertSame(ProtoTypeName.INT32, ProtoTypeName.getScalar("int32"));
  }

  @Test public void scalarToString() throws Exception {
    assertEquals("int32", ProtoTypeName.INT32.toString());
    assertEquals("string", ProtoTypeName.STRING.toString());
    assertEquals("bytes", ProtoTypeName.BYTES.toString());
  }

  @Test public void messageToString() throws Exception {
    ProtoTypeName person = ProtoTypeName.get("squareup.protos.person", "Person");
    assertEquals("squareup.protos.person.Person", person.toString());

    ProtoTypeName phoneType = person.nestedType("PhoneType");
    assertEquals("squareup.protos.person.Person.PhoneType", phoneType.toString());
  }

  @Test public void enclosingTypeName() throws Exception {
    assertEquals(null, ProtoTypeName.STRING.enclosingTypeName());

    ProtoTypeName person = ProtoTypeName.get("squareup.protos.person", "Person");
    assertEquals(null, person.enclosingTypeName());

    ProtoTypeName phoneType = person.nestedType("PhoneType");
    assertEquals(person, phoneType.enclosingTypeName());
  }

  @Test public void isScalar() throws Exception {
    assertTrue(ProtoTypeName.INT32.isScalar());
    assertTrue(ProtoTypeName.STRING.isScalar());
    assertTrue(ProtoTypeName.BYTES.isScalar());
    assertFalse(ProtoTypeName.get("squareup.protos.person", "Person").isScalar());
  }

  @Test public void isPackableScalar() throws Exception {
    assertTrue(ProtoTypeName.INT32.isPackableScalar());
    assertFalse(ProtoTypeName.STRING.isPackableScalar());
    assertFalse(ProtoTypeName.BYTES.isPackableScalar());
    assertFalse(ProtoTypeName.get("squareup.protos.person", "Person").isPackableScalar());
  }
}
