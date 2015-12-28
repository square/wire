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

import com.squareup.wire.protos.person.Person;
import com.squareup.wire.protos.roots.C;
import com.squareup.wire.protos.simple.SimpleMessage;
import okio.ByteString;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class ProtoAdapterTest {

  @Test public void getFromClass() throws Exception {
    Person person = new Person.Builder()
        .id(99)
        .name("Omar Little")
        .build();
    ByteString encoded = ByteString.decodeHex("0a0b4f6d6172204c6974746c651063");

    ProtoAdapter<Person> personAdapter = ProtoAdapter.get(Person.class);
    assertThat(ByteString.of(personAdapter.encode(person))).isEqualTo(encoded);
    assertThat(personAdapter.decode(encoded.toByteArray())).isEqualTo(person);
  }

  @Test public void getFromClassWrongType() throws Exception {
    Message nonGeneratedMessage = new Message(ByteString.EMPTY) {
      @Override public Builder newBuilder() {
        throw new AssertionError();
      }
    };
    try {
      ProtoAdapter.get(nonGeneratedMessage.getClass());
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageStartingWith("failed to access ");
    }
  }

  @Test public void repeatedHelpersCacheInstances() throws Exception {
    ProtoAdapter<?> adapter = ProtoAdapter.UINT64;
    assertThat(adapter.asRepeated()).isSameAs(adapter.asRepeated());
    assertThat(adapter.asPacked()).isSameAs(adapter.asPacked());
  }

  @Test public void staticNewMessageAdapter() throws Exception {
    assertThat(ProtoAdapter.newMessageAdapter(SimpleMessage.NestedMessage.class)).isEqualTo(
      RuntimeMessageAdapter.create(SimpleMessage.NestedMessage.class));
  }

  @Test public void staticNewMessageAdapterThrown() {
    try {
      // when
      ProtoAdapter.newMessageAdapter(null);

      // then
      fail("ProtoAdapter should throw NullPointerException when null is passed as parameter");
    }
    catch (NullPointerException e) {
      assertThat(e.getMessage()).isNull();
    }
  }

  @Test public void staticGet() {
    // when
    ProtoAdapter protoAdapter = ProtoAdapter.get("com.squareup.wire.protos.roots.C#ADAPTER");

    // then
    assertThat(protoAdapter.javaType).isEqualTo(C.class);
    assertThat(protoAdapter.getClass()).isEqualTo(C.ADAPTER.getClass());
  }

  @Test public void staticGetNull() {
    try {
      // when
      ProtoAdapter.get((String) null);

      // then
      fail("ProtoAdapter should throw NullPointerException when null is passed as parameter");
    }
    catch (NullPointerException e) {
      assertThat(e.getMessage()).isNull();
    }
  }

  @Test public void staticGetEmpty() {
    try {
      // when
      ProtoAdapter.get("");

      // then
      fail("ProtoAdapter should throw IllegalArgumentException when empty string is passed as parameter");
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("adapterString must contain #");
    }
  }

  @Test public void staticGetIncorrectPath() {
    try {
      // when
      ProtoAdapter.get("IncorrectPath");

      // then
      fail("ProtoAdapter should throw IllegalArgumentException when string without # is passed as parameter");
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("adapterString must contain #");
    }
  }

  @Test public void staticGetIncorrectClass() {
    try {
      // when
      ProtoAdapter.get("C#ADAPTER");

      // then
      fail("ProtoAdapter should throw IllegalArgumentException when path contains wrong class");
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("failed to access C#ADAPTER");
    }
  }

  @Test public void staticGetIncorrectField() {
    try {
      // when
      ProtoAdapter.get("com.squareup.wire.protos.roots.C#protoAdapter");

      // then
      fail("ProtoAdapter should throw IllegalArgumentException when path contains wrong field");
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("failed to access com.squareup.wire.protos.roots.C#protoAdapter");
    }
  }

  @Test public void staticGetIncorrectFieldAccess() {
    try {
      // when
      ProtoAdapter.get("com.squareup.wire.protos.roots.C#serialVersionUID");

      // then
      fail("ProtoAdapter should throw IllegalArgumentException when path contains inaccessible field");
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("failed to access com.squareup.wire.protos.roots.C#serialVersionUID");
    }
  }

  @Test public void staticGetIncorrectFieldType() {
    try {
      // when
      ProtoAdapter.get("com.squareup.wire.protos.roots.C#DEFAULT_I");

      // then
      fail("ProtoAdapter should throw ClassCastException when path contains field with incorrect type");
    }
    catch (ClassCastException e) {
      assertThat(e).hasMessage("java.lang.Integer cannot be cast to com.squareup.wire.ProtoAdapter");
    }
  }

  @Test public void redacted() {
    assertThat(ProtoAdapter.INT32.redact(null)).isNull();
    assertThat(ProtoAdapter.INT32.redact(20)).isNull();
  }

  @Test public void encodeNull() {
    // given
    ProtoAdapter<C> protoAdapter = createErrorProtoAdapter();

    try {
      // when
      protoAdapter.encode(null);

      // then
      fail("ProtoAdapter should throw NullPointerException when null value is passed to encode");
    }
    catch (NullPointerException e) {
      assertThat(e).hasMessage("value == null");
    }
  }

  @Test public void encodeIOError() {
    // given
    ProtoAdapter<C> protoAdapter = createErrorProtoAdapter();

    // when
    try {
      protoAdapter.encode(new C(20));
    }
    catch (AssertionError e) {
      return;
    }

    // then
    fail("ProtoAdapter should throw AssertionError when IO error occur during encoding");
  }

  private ProtoAdapter<C> createErrorProtoAdapter() {
    return new ProtoAdapter<C>(FieldEncoding.LENGTH_DELIMITED, C.class) {
      @Override public int encodedSize(C value) {
        return 0;
      }

      @Override public void encode(ProtoWriter writer, C value) throws IOException {
        throw new IOException("Unsupported operation");
      }

      @Override public C decode(ProtoReader reader) throws IOException {
        throw new IOException("Unsupported operation");
      }
    };
  }
}
