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
import okio.Buffer;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RuntimeMessageAdapterTest {
  private RuntimeMessageAdapter<WiredFieldsMessage, WiredFieldsMessage.Builder> adapter;
  private Map<Integer, FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder>> fieldBindings;

  @Before
  public void init() throws NoSuchFieldException {
    fieldBindings = createFieldBindings("val");
    adapter = new RuntimeMessageAdapter<>(WiredFieldsMessage.class, WiredFieldsMessage.Builder.class, fieldBindings);
  }

  @Test(expected = IllegalArgumentException.class) public void staticCreateWrongBuilder() {
    // when
    RuntimeMessageAdapter.create(CustomBuilderNameMessage.class);

    // then
    fail("RuntimeMessageAdapter should throw IllegalArgumentException when message type don't have nested Builder class");
  }

  @Test public void constructor() {
    assertThat(adapter.fieldBindings()).isEqualTo(fieldBindings);
  }

  @Test public void newBuilder() {
    // when
    WiredFieldsMessage.Builder builder = adapter.newBuilder();

    // then
    assertThat(builder).isEqualTo(new WiredFieldsMessage.Builder());
  }

  @Test public void newBuilderInaccessibleConstructor() {
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

  @Test public void newBuilderErrorThrowingConstructor() {
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

  @Test public void encodeSize() {
    // when
    WiredFieldsMessage message = new WiredFieldsMessage(1, Arrays.asList(3, 7, 5), new C(51));
    WiredFieldsMessage nullMessage = new WiredFieldsMessage(null, Arrays.asList(3, 7, 5), new C(51));

    // then
    assertThat(adapter.encodedSize(message)).isEqualTo(2);
    assertThat(adapter.encodedSize(nullMessage)).isEqualTo(0);
  }

  @Test public void toStringRedacted() {
    // when
    WiredFieldsMessage message = new WiredFieldsMessage(15, Arrays.asList(3, 7, 5), new C(51));

    // then
    assertThat(adapter.toString(message)).isEqualTo("WiredFieldsMessage{val=██}");
  }


  @Test public void toStringNotRedacted() throws NoSuchFieldException {
    // given
    fieldBindings = createFieldBindings("c");
    adapter = new RuntimeMessageAdapter<>(WiredFieldsMessage.class, WiredFieldsMessage.Builder.class, fieldBindings);

    // when
    WiredFieldsMessage message = new WiredFieldsMessage(15, Arrays.asList(3, 7, 5), new C(51));

    // then
    assertThat(adapter.toString(message)).isEqualTo("WiredFieldsMessage{c=C{i=51}}");
  }

  @Test public void toStringNull() throws NoSuchFieldException {
    // given
    fieldBindings = createFieldBindings("c");
    adapter = new RuntimeMessageAdapter<>(WiredFieldsMessage.class, WiredFieldsMessage.Builder.class, fieldBindings);

    // when
    WiredFieldsMessage message = new WiredFieldsMessage(15, Arrays.asList(3, 7, 5), null);

    // then
    assertThat(adapter.toString(message)).isEqualTo("WiredFieldsMessage{}");
  }

  @Test public void equalsMethod() {
    assertThat(adapter.equals(RuntimeMessageAdapter.create(WiredFieldsMessage.class))).isTrue();
    assertThat(adapter.equals(RuntimeMessageAdapter.create(C.class))).isFalse();
    assertThat(adapter.equals(fieldBindings)).isFalse();
  }

  @Test public void hashCodeMethod() {
    assertThat(adapter.hashCode()).isEqualTo(WiredFieldsMessage.class.hashCode());
  }

  @Test public void decodeWithWiredFields() throws IOException, NoSuchFieldException {
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

  @Test public void decodeWithoutWiredFields() throws IOException {
    // given
    RuntimeMessageAdapter<C, C.Builder> messageAdapter = RuntimeMessageAdapter.create(C.class);
    ByteString encoded = ByteString.decodeHex("082c");

    // when
    C decoded = messageAdapter.decode(new ProtoReader(new Buffer().write(encoded.toByteArray())));

    // then
    assertThat(decoded.i).isNull();
    assertThat(C.ADAPTER.decode(decoded.unknownFields().toByteArray())).isEqualTo(new C(44));
  }

  private Map<Integer, FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder>>
  createFieldBindings(String... fieldNames) throws NoSuchFieldException {

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
