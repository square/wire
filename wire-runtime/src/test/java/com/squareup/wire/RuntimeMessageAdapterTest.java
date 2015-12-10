/*
 * Copyright 2015 Square Inc.
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

import com.squareup.wire.protos.roots.C;
import com.squareup.wire.protos.roots.CustomBuilderNameMessage;
import com.squareup.wire.protos.roots.WiredFieldsMessage;
import com.squareup.wire.protos.simple.SimpleMessage;
import okio.Buffer;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RuntimeMessageAdapter.class, ProtoWriter.class, FieldBinding.class, C.Builder.class })
public class RuntimeMessageAdapterTest {
  private RuntimeMessageAdapter<WiredFieldsMessage, WiredFieldsMessage.Builder> adapter;
  private Map<Integer, FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder>> fieldBindings;

  @Before
  public void init() throws Exception {
    fieldBindings = createFieldBindings("val");
    adapter = new RuntimeMessageAdapter<>(WiredFieldsMessage.class, WiredFieldsMessage.Builder.class, fieldBindings);
  }

  @Test public void staticCreate() throws Exception {
    // given
    Map<Integer, FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder>> expectedFieldBindings = createFieldBindings("val", "list", "c");

    // when
    RuntimeMessageAdapter wiredFieldsAdapter = RuntimeMessageAdapter.create(WiredFieldsMessage.class);
    RuntimeMessageAdapter nestedAdapter = RuntimeMessageAdapter.create(SimpleMessage.NestedMessage.class);
    Map<Integer, FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder>> actualFieldBindings =
      Whitebox.getInternalState(wiredFieldsAdapter, "fieldBindings");

    // then
    assertThat(Whitebox.getInternalState(wiredFieldsAdapter, "messageType")).isEqualTo(WiredFieldsMessage.class);
    assertThat(Whitebox.getInternalState(wiredFieldsAdapter, "builderType")).isEqualTo(WiredFieldsMessage.Builder.class);

    assertThat(actualFieldBindings.get(1).name).isEqualTo(expectedFieldBindings.get(1).name);
    assertThat(actualFieldBindings.get(2).name).isEqualTo(expectedFieldBindings.get(2).name);
    assertThat(actualFieldBindings.get(3).name).isEqualTo(expectedFieldBindings.get(3).name);

    assertThat(Whitebox.getInternalState(nestedAdapter, "messageType")).isEqualTo(SimpleMessage.NestedMessage.class);
    assertThat(Whitebox.getInternalState(nestedAdapter, "builderType")).isEqualTo(SimpleMessage.NestedMessage.Builder.class);
    assertThat(Whitebox.getInternalState(nestedAdapter, "fieldBindings")).isEqualTo(Collections.emptyMap());
  }

  @Test(expected = IllegalArgumentException.class) public void staticCreateWrongBuilder() throws Exception {
    // when
    RuntimeMessageAdapter.create(CustomBuilderNameMessage.class);

    // then
    fail("RuntimeMessageAdapter should throw IllegalArgumentException when message type don't have nested Builder class");
  }

  @Test public void staticBuilderType() throws Exception {
    // when
    Class<C.Builder> builderType = Whitebox.invokeMethod(RuntimeMessageAdapter.class, "getBuilderType", C.class);

    // then
    assertThat(builderType).isEqualTo(C.Builder.class);
  }

  @Test public void constructor() throws Exception {
    assertThat(Whitebox.getInternalState(adapter, "messageType")).isEqualTo(WiredFieldsMessage.class);
    assertThat(Whitebox.getInternalState(adapter, "builderType")).isEqualTo(WiredFieldsMessage.Builder.class);
    assertThat(Whitebox.getInternalState(adapter, "fieldBindings")).isEqualTo(fieldBindings);
    assertThat(adapter.fieldBindings()).isEqualTo(fieldBindings);
  }

  @Test public void newBuilder() throws Exception {
    // when
    WiredFieldsMessage.Builder builder = adapter.newBuilder();

    // then
    assertThat(builder).isEqualTo(new WiredFieldsMessage.Builder());
  }

  @Test public void newBuilderInaccessibleConstructor() throws Exception {
    // given
    RuntimeMessageAdapter<CustomBuilderNameMessage, CustomBuilderNameMessage.CustomBuilder> adapter =
      new RuntimeMessageAdapter<>(
        CustomBuilderNameMessage.class,
        CustomBuilderNameMessage.CustomBuilder.class,
        Collections.EMPTY_MAP
      );

    // when
    try {
      adapter.newBuilder();
    }
    catch (AssertionError e) {
      return;
    }

    // then
    fail("RuntimeMessageAdapter should throw AssertionError when builder constructor is inaccessible");
  }

  @Test public void newBuilderErrorThrowingConstructor() throws Exception {
    // given
    RuntimeMessageAdapter adapter =
      new RuntimeMessageAdapter(
        CustomBuilderNameMessage.class,
        CustomBuilderNameMessage.ExtendedCustomBuilder.class,
        Collections.EMPTY_MAP
      );

    // when
    try {
      adapter.newBuilder();
    }
    catch (AssertionError e) {
      return;
    }

    // then
    fail("RuntimeMessageAdapter should throw AssertionError when builder constructor is inaccessible");
  }

  @Test public void encode() throws Exception {
    // given
    WiredFieldsMessage message = new WiredFieldsMessage(15, Arrays.asList(3, 7, 5), new C(51));
    ProtoWriter protoWriter = mock(ProtoWriter.class);

    // when
    adapter.encode(protoWriter, message);

    // then
    Mockito.verify(protoWriter).writeTag(1, FieldEncoding.VARINT);
    Mockito.verify(protoWriter).writeSignedVarint32(15);
    Mockito.verify(protoWriter).writeBytes(ByteString.EMPTY);
  }

  @Test public void encodeNull() throws Exception {
    // given
    WiredFieldsMessage message = new WiredFieldsMessage(null, Arrays.asList(3, 7, 5), new C(51));
    ProtoWriter protoWriter = mock(ProtoWriter.class);

    // when
    adapter.encode(protoWriter, message);

    // then
    Mockito.verify(protoWriter, Mockito.never()).writeTag(1, FieldEncoding.VARINT);
    Mockito.verify(protoWriter, Mockito.never()).writeSignedVarint32(15);
    Mockito.verify(protoWriter).writeBytes(ByteString.EMPTY);
  }

  @Test public void encodeSize() throws Exception {
    // when
    WiredFieldsMessage message = new WiredFieldsMessage(1, Arrays.asList(3, 7, 5), new C(51));
    WiredFieldsMessage nullMessage = new WiredFieldsMessage(null, Arrays.asList(3, 7, 5), new C(51));

    WiredFieldsMessage cachedMessage = new WiredFieldsMessage(1, Arrays.asList(3, 7, 5), new C(51));
    Whitebox.setInternalState(cachedMessage, "cachedSerializedSize", 101);

    // then
    assertThat(adapter.encodedSize(message)).isEqualTo(2);
    assertThat(adapter.encodedSize(nullMessage)).isEqualTo(0);
    assertThat(adapter.encodedSize(cachedMessage)).isEqualTo(101);
  }

  @Test public void toStringRedacted() throws Exception {
    // when
    WiredFieldsMessage message = new WiredFieldsMessage(15, Arrays.asList(3, 7, 5), new C(51));

    // then
    assertThat(adapter.toString(message)).isEqualTo("WiredFieldsMessage{val=██}");
  }


  @Test public void toStringNotRedacted() throws Exception {
    // given
    fieldBindings = createFieldBindings("c");
    adapter = new RuntimeMessageAdapter<>(WiredFieldsMessage.class, WiredFieldsMessage.Builder.class, fieldBindings);

    // when
    WiredFieldsMessage message = new WiredFieldsMessage(15, Arrays.asList(3, 7, 5), new C(51));

    // then
    assertThat(adapter.toString(message)).isEqualTo("WiredFieldsMessage{c=C{i=51}}");
  }

  @Test public void toStringNull() throws Exception {
    // given
    fieldBindings = createFieldBindings("c");
    adapter = new RuntimeMessageAdapter<>(WiredFieldsMessage.class, WiredFieldsMessage.Builder.class, fieldBindings);

    // when
    WiredFieldsMessage message = new WiredFieldsMessage(15, Arrays.asList(3, 7, 5), null);

    // then
    assertThat(adapter.toString(message)).isEqualTo("WiredFieldsMessage{}");
  }

  @Test public void equalsMethod() throws Exception {
    assertThat(adapter.equals(RuntimeMessageAdapter.create(WiredFieldsMessage.class))).isTrue();
    assertThat(adapter.equals(RuntimeMessageAdapter.create(C.class))).isFalse();
    assertThat(adapter.equals(fieldBindings)).isFalse();
  }

  @Test public void hashCodeMethod() throws Exception {
    assertThat(adapter.hashCode()).isEqualTo(WiredFieldsMessage.class.hashCode());
  }

  @Test public void decodeWithWiredFields() throws Exception {
    // given
    fieldBindings = createFieldBindings("val", "list", "c");
    adapter = new RuntimeMessageAdapter<>(WiredFieldsMessage.class, WiredFieldsMessage.Builder.class, fieldBindings);

    ByteString encoded = ByteString.decodeHex("080f1003100710051a020833");

    // when
    WiredFieldsMessage decoded = adapter.decode(new ProtoReader(new Buffer().write(encoded.toByteArray())));

    // then
    assertThat(decoded.val).isEqualTo(15);
    assertThat(decoded.list).isEqualTo(Arrays.asList(3, 7, 5));
    assertThat(decoded.c).isEqualTo(new C(51));
    assertThat(decoded.unknownFields()).isEqualTo(ByteString.EMPTY);
  }

  @Test public void decodeWithoutWiredFields() throws Exception {
    // given
    RuntimeMessageAdapter<C, C.Builder> messageAdapter = RuntimeMessageAdapter.create(C.class);
    ByteString encoded = ByteString.decodeHex("082c");

    // when
    C decoded = messageAdapter.decode(new ProtoReader(new Buffer().write(encoded.toByteArray())));

    // then
    assertThat(decoded.i).isNull();
    assertThat(C.ADAPTER.decode(decoded.unknownFields().toByteArray())).isEqualTo(new C(44));
  }

  @Test public void decodeWithoutEnumConstants() throws Exception {
    // given
    RuntimeMessageAdapter<C, C.Builder> messageAdapter = spy(RuntimeMessageAdapter.create(C.class));
    C.Builder builder = mock(C.Builder.class);

    PowerMockito.when(builder.addUnknownField(1, FieldEncoding.VARINT, 44L))
      .thenThrow(new ProtoAdapter.EnumConstantNotFoundException(101, Integer.TYPE));

    PowerMockito.when(messageAdapter.newBuilder()).thenReturn(builder);
    ByteString encoded = ByteString.decodeHex("082c");

    // when
    messageAdapter.decode(new ProtoReader(new Buffer().write(encoded.toByteArray())));

    // then
    Mockito.verify(builder).addUnknownField(1, FieldEncoding.VARINT, 44L);
    Mockito.verify(builder).addUnknownField(1, FieldEncoding.VARINT, 101L);
  }

  private Map<Integer, FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder>> createFieldBindings(String... fieldNames) throws Exception {

    Map<Integer, FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder>> map = new HashMap<>();

    for (String fieldName : fieldNames)
    {
      Field valField = WiredFieldsMessage.class.getField(fieldName);
      WireField wireField = valField.getAnnotation(WireField.class);

      map.put(wireField.tag(), new FieldBinding<>(wireField, valField, WiredFieldsMessage.Builder.class));
    }

    return map;
  }

}
