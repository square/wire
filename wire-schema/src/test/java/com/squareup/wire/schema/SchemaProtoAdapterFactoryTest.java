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

import com.google.common.collect.ImmutableMap;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.schema.internal.parser.EnumElement;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class SchemaProtoAdapterFactoryTest {

  private Location location = Location.get("file.proto");


  @Test public void enumAdapterConstructor() throws Exception {
    // given
    EnumElement enumElement = EnumElement.builder(location).name("E").build();
    EnumType enumType = (EnumType)Type.get("", ProtoType.get("Person"), enumElement);

    // when
    SchemaProtoAdapterFactory.EnumAdapter adapter = new SchemaProtoAdapterFactory.EnumAdapter(enumType);

    // then
    assertEquals(adapter.enumType, enumType);
  }

  @Test public void messageAdapterConstructor() throws Exception {
    assertEquals(new SchemaProtoAdapterFactory.MessageAdapter(true).includeUnknown, true);
    assertEquals(new SchemaProtoAdapterFactory.MessageAdapter(false).includeUnknown, false);
  }

  @Test(expected = UnsupportedOperationException.class) public void messageAdapterRedact() throws Exception {
    // given
    SchemaProtoAdapterFactory.MessageAdapter adapter = new SchemaProtoAdapterFactory.MessageAdapter(false);

    // when
    adapter.redact(ImmutableMap.<String, Object>of());

    // then
    fail("SchemaProtoAdapterFactory.MessageAdapter should throw UnsupportedOperationException on redact method call");
  }

  @Test public void messageEncodeSize() throws Exception {

    SchemaProtoAdapterFactory.MessageAdapter adapter = new SchemaProtoAdapterFactory.MessageAdapter(false);
    adapter.fieldsByName.put("RepeatedField", new SchemaProtoAdapterFactory.Field("", 1, true, ProtoAdapter.INT32));
    adapter.fieldsByName.put("OptionalField", new SchemaProtoAdapterFactory.Field("", 2, false, ProtoAdapter.BOOL));

    assertEquals(adapter.encodedSize(ImmutableMap.<String, Object>of()), 0);
    assertEquals(adapter.encodedSize(ImmutableMap.<String, Object>of("UnknownField", "unknown value")), 0);
    assertEquals(adapter.encodedSize(ImmutableMap.<String, Object>of("RepeatedField", Arrays.asList(1, 2, 3))), 6);
    assertEquals(adapter.encodedSize(ImmutableMap.of("UnknownField", "unknown value", "RepeatedField", Arrays.asList(1, 2, 3))), 6);
    assertEquals(adapter.encodedSize(ImmutableMap.<String, Object>of("OptionalField", true)), 2);
    assertEquals(adapter.encodedSize(ImmutableMap.<String, Object>of("UnknownField", "unknown value", "OptionalField", false)), 2);
  }

  @Test(expected = UnsupportedOperationException.class) public void messageAdapterToString() throws Exception {
    // given
    SchemaProtoAdapterFactory.MessageAdapter adapter = new SchemaProtoAdapterFactory.MessageAdapter(false);

    // when
    adapter.toString(ImmutableMap.<String, Object>of());

    // then
    fail("SchemaProtoAdapterFactory.MessageAdapter should throw UnsupportedOperationException on toString method call");
  }

}
