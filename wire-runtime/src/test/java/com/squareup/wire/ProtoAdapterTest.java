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

  @Test(expected = NullPointerException.class) public void staticNewMessageAdapterThrown() {
    ProtoAdapter.newMessageAdapter(null);
    fail("ProtoAdapter should throw NullPointerException when null is passed as parameter");
  }

  @Test public void staticGet() {
    // when
    ProtoAdapter protoAdapter = ProtoAdapter.get("com.squareup.wire.protos.roots.C#ADAPTER");

    // then
    assertThat(protoAdapter.javaType).isEqualTo(C.class);
    assertThat(protoAdapter.getClass()).isEqualTo(C.ADAPTER.getClass());
  }

  @Test(expected = NullPointerException.class) public void staticGetNull() {
    ProtoAdapter.get((String) null);
    fail("ProtoAdapter should throw NullPointerException when null is passed as parameter");
  }

  @Test(expected = IllegalArgumentException.class) public void staticGetEmpty() {
    ProtoAdapter.get("");
    fail("ProtoAdapter should throw IllegalArgumentException when empty string is passed as parameter");
  }

  @Test(expected = IllegalArgumentException.class) public void staticGetIncorrectPath() {
    ProtoAdapter.get("IncorrectPath");
    fail("ProtoAdapter should throw IllegalArgumentException when string without # is passed as parameter");
  }

  @Test(expected = IllegalArgumentException.class) public void staticGetIncorrectClass() {
    ProtoAdapter.get("C#ADAPTER");
    fail("ProtoAdapter should throw IllegalArgumentException when path contains wrong class");
  }

  @Test(expected = IllegalArgumentException.class) public void staticGetIncorrectField() {
    ProtoAdapter.get("com.squareup.wire.protos.roots.C#protoAdapter");
    fail("ProtoAdapter should throw IllegalArgumentException when path contains wrong field");
  }

  @Test(expected = IllegalArgumentException.class) public void staticGetIncorrectFieldAccess() {
    ProtoAdapter.get("com.squareup.wire.protos.roots.C#serialVersionUID");
    fail("ProtoAdapter should throw IllegalArgumentException when path contains inaccessible field");
  }

  @Test(expected = ClassCastException.class) public void staticGetIncorrectFieldType() {
    ProtoAdapter.get("com.squareup.wire.protos.roots.C#DEFAULT_I");
    fail("ProtoAdapter should throw ClassCastException when path contains field with incorrect type");
  }

  @Test public void redacted() {
    assertThat(ProtoAdapter.INT32.redact(null)).isNull();
    assertThat(ProtoAdapter.INT32.redact(20)).isNull();
  }

  @Test(expected = NullPointerException.class) public void encodeNull() {
    // given
    ProtoAdapter<C> protoAdapter = createErrorProtoAdapter();

    // when
    protoAdapter.encode(null);

    // then
    fail("ProtoAdapter should throw NullPointerException when null value is passed to encode");
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
