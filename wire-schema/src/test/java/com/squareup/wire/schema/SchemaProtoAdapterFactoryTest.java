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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.schema.internal.parser.EnumConstantElement;
import com.squareup.wire.schema.internal.parser.EnumElement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ ProtoType.class, EnumType.class, Type.class, Schema.class,
                  ProtoReader.class, ProtoWriter.class, ProtoAdapter.class })
public class SchemaProtoAdapterFactoryTest {

  private Location location = Location.get("file.proto");

  @Test(expected = IllegalArgumentException.class) public void getIllegalType() throws Exception {
    // given
    ProtoType protoType =
      PowerMockito.constructor( ProtoType.class, boolean.class, String.class ).newInstance(false, "unknownProtoType");

    Schema schemaMock = mock(Schema.class);
    when(schemaMock.getType(Mockito.any(ProtoType.class))).thenReturn(mock(Type.class));

    SchemaProtoAdapterFactory factory = new SchemaProtoAdapterFactory(schemaMock, true);

    // when
    factory.get(protoType);

    // then
    fail("SchemaProtoAdapterFactory should throw IllegalArgumentException when getting unknown type");
  }

  @Test(expected = IllegalArgumentException.class) public void getUnknownType() throws Exception {
    // given
    ProtoType protoType =
      PowerMockito.constructor( ProtoType.class, boolean.class, String.class ).newInstance(false, "unknownProtoType");

    Schema schemaMock = mock(Schema.class);
    when(schemaMock.getType(Mockito.any(ProtoType.class))).thenReturn(null);

    SchemaProtoAdapterFactory factory = new SchemaProtoAdapterFactory(schemaMock, true);

    // when
    factory.get(protoType);

    // then
    fail("SchemaProtoAdapterFactory should throw IllegalArgumentException when getting unknown type");
  }

  @Test public void enumAdapterConstructor() throws Exception {
    // given
    EnumElement enumElement = EnumElement.builder(location).name("E").build();
    EnumType enumType = (EnumType)Type.get("", ProtoType.get("Person"), enumElement);

    // when
    SchemaProtoAdapterFactory.EnumAdapter adapter = new SchemaProtoAdapterFactory.EnumAdapter(enumType);

    // then
    assertEquals(adapter.enumType, enumType);
  }

  @Test public void enumAdapterEncode() throws Exception {
    // given
    ProtoWriter protoWriter = mock(ProtoWriter.class);

    EnumConstantElement one = EnumConstantElement.builder(location).name("ONE").tag(1).build();

    EnumElement enumElement = EnumElement.builder(location)
      .name("E")
      .constants(ImmutableList.of(one))
    .build();

    EnumType enumType = (EnumType)Type.get("", ProtoType.get("Person"), enumElement);
    SchemaProtoAdapterFactory.EnumAdapter adapter = new SchemaProtoAdapterFactory.EnumAdapter(enumType);

    // when
    adapter.encode(protoWriter, "ONE");
    adapter.encode(protoWriter, 1);

    // then
    Mockito.verify(protoWriter, Mockito.times(2)).writeVarint32(1);
  }

  @Test(expected = IllegalArgumentException.class) public void enumAdapterEncodeNull() throws Exception {
    // given
    ProtoWriter protoWriter = mock(ProtoWriter.class);

    EnumConstantElement one = EnumConstantElement.builder(location).name("ONE").tag(1).build();

    EnumElement enumElement = EnumElement.builder(location)
      .name("E")
      .constants(ImmutableList.of(one))
      .build();

    EnumType enumType = (EnumType)Type.get("", ProtoType.get("Person"), enumElement);
    SchemaProtoAdapterFactory.EnumAdapter adapter = new SchemaProtoAdapterFactory.EnumAdapter(enumType);

    // when
    adapter.encode(protoWriter, null);

    // then
    fail("SchemaProtoAdapterFactory.EnumAdapter should throw IllegalArgumentException on encod null value");
  }

  @Test(expected = UnsupportedOperationException.class) public void enumAdapterEncodeSize() throws Exception {
    // given
    EnumType enumType = mock(EnumType.class);
    SchemaProtoAdapterFactory.EnumAdapter adapter = new SchemaProtoAdapterFactory.EnumAdapter(enumType);

    // when
    adapter.encodedSize("test");

    // then
    fail("SchemaProtoAdapterFactory.EnumAdapter should throw UnsupportedOperationException on encodedSize method call");
  }

  @Test public void enumAdapterDecode() throws Exception {
    // given
    EnumConstantElement one = EnumConstantElement.builder(location).name("ONE").tag(1).build();

    EnumElement enumElement = EnumElement.builder(location)
      .name("E")
      .constants(ImmutableList.of(one))
      .build();

    EnumType enumType = (EnumType)Type.get("", ProtoType.get("Person"), enumElement);
    SchemaProtoAdapterFactory.EnumAdapter adapter = new SchemaProtoAdapterFactory.EnumAdapter(enumType);

    ProtoReader validElementProtoReader = mock(ProtoReader.class);
    ProtoReader invalidElementProtoReader = mock(ProtoReader.class);

    when(validElementProtoReader.readVarint32()).thenReturn(1);
    when(invalidElementProtoReader.readVarint32()).thenReturn(2);

    // when
    assertEquals(adapter.decode(validElementProtoReader), "ONE");
    assertEquals(adapter.decode(invalidElementProtoReader), 2);
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

  @Test public void messageEncode() throws Exception {
    // given
    ProtoAdapter protoAdapter = mock(ProtoAdapter.class);
    ProtoWriter protoWriter = mock(ProtoWriter.class);

    SchemaProtoAdapterFactory.MessageAdapter adapter = new SchemaProtoAdapterFactory.MessageAdapter(false);
    adapter.fieldsByName.put("RepeatedField", new SchemaProtoAdapterFactory.Field("", 1, true, protoAdapter));
    adapter.fieldsByName.put("OptionalField", new SchemaProtoAdapterFactory.Field("", 2, false, protoAdapter));

    // when
    adapter.encode(protoWriter, ImmutableMap.<String, Object>of());
    adapter.encode(protoWriter, ImmutableMap.<String, Object>of("UnknownField", "unknown value"));
    adapter.encode(protoWriter, ImmutableMap.<String, Object>of("RepeatedField", Arrays.asList(1, 2, 3)));
    adapter.encode(protoWriter, ImmutableMap.of("UnknownField", "unknown value", "RepeatedField", Arrays.asList(1, 2, 3)));
    adapter.encode(protoWriter, ImmutableMap.<String, Object>of("OptionalField", true));
    adapter.encode(protoWriter, ImmutableMap.<String, Object>of("UnknownField", "unknown value", "OptionalField", false));

    // then
    Mockito.verify(protoAdapter, Mockito.never()).encodeWithTag(Mockito.eq(protoWriter), Mockito.anyInt(), Mockito.eq("unknown value"));
    Mockito.verify(protoAdapter, Mockito.times(2)).encodeWithTag(Mockito.eq(protoWriter), Mockito.anyInt(), Mockito.eq(1));
    Mockito.verify(protoAdapter, Mockito.times(2)).encodeWithTag(Mockito.eq(protoWriter), Mockito.anyInt(), Mockito.eq(2));
    Mockito.verify(protoAdapter, Mockito.times(2)).encodeWithTag(Mockito.eq(protoWriter), Mockito.anyInt(), Mockito.eq(3));
    Mockito.verify(protoAdapter).encodeWithTag(Mockito.eq(protoWriter), Mockito.anyInt(), Mockito.eq(true));
    Mockito.verify(protoAdapter).encodeWithTag(Mockito.eq(protoWriter), Mockito.anyInt(), Mockito.eq(false));
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
