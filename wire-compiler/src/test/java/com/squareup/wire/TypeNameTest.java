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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class TypeNameTest {
  @Test public void getScalar() throws Exception {
    assertSame(Type.Name.INT32, Type.Name.getScalar("int32"));
  }

  @Test public void scalarToString() throws Exception {
    assertEquals("int32", Type.Name.INT32.toString());
    assertEquals("string", Type.Name.STRING.toString());
    assertEquals("bytes", Type.Name.BYTES.toString());
  }

  @Test public void messageToString() throws Exception {
    Type.Name person = Type.Name.get("squareup.protos.person", "Person");
    assertEquals("squareup.protos.person.Person", person.toString());

    Type.Name phoneType = person.nestedType("PhoneType");
    assertEquals("squareup.protos.person.Person.PhoneType", phoneType.toString());
  }

  @Test public void enclosingTypeName() throws Exception {
    assertEquals(null, Type.Name.STRING.enclosingTypeName());

    Type.Name person = Type.Name.get("squareup.protos.person", "Person");
    assertEquals(null, person.enclosingTypeName());

    Type.Name phoneType = person.nestedType("PhoneType");
    assertEquals(person, phoneType.enclosingTypeName());
  }

  @Test public void isScalar() throws Exception {
    assertTrue(Type.Name.INT32.isScalar());
    assertTrue(Type.Name.STRING.isScalar());
    assertTrue(Type.Name.BYTES.isScalar());
    assertFalse(Type.Name.get("squareup.protos.person", "Person").isScalar());
  }

  @Test public void isPackableScalar() throws Exception {
    assertTrue(Type.Name.INT32.isPackableScalar());
    assertFalse(Type.Name.STRING.isPackableScalar());
    assertFalse(Type.Name.BYTES.isPackableScalar());
    assertFalse(Type.Name.get("squareup.protos.person", "Person").isPackableScalar());
  }
}
