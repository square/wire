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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ProtoReader.class, ProtoWriter.class, WireField.Label.class})
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

  @Test(expected = NullPointerException.class) public void staticNewMessageAdapterThrown() throws Exception {
    ProtoAdapter.newMessageAdapter(null);
    fail("ProtoAdapter should throw NullPointerException when null is passed as parameter");
  }

  @Test public void staticGet() throws Exception {
    // when
    ProtoAdapter protoAdapter = ProtoAdapter.get("com.squareup.wire.protos.roots.C#ADAPTER");

    // then
    assertThat(protoAdapter.javaType).isEqualTo(C.class);
    assertThat(protoAdapter.getClass()).isEqualTo(C.ADAPTER.getClass());
  }

  @Test(expected = NullPointerException.class) public void staticGetNull() throws Exception {
    ProtoAdapter.get((String) null);
    fail("ProtoAdapter should throw NullPointerException when null is passed as parameter");
  }

  @Test(expected = IllegalArgumentException.class) public void staticGetEmpty() throws Exception {
    ProtoAdapter.get("");
    fail("ProtoAdapter should throw IllegalArgumentException when empty string is passed as parameter");
  }

  @Test(expected = IllegalArgumentException.class) public void staticGetIncorrectPath() throws Exception {
    ProtoAdapter.get("IncorrectPath");
    fail("ProtoAdapter should throw IllegalArgumentException when string without # is passed as parameter");
  }

  @Test(expected = IllegalArgumentException.class) public void staticGetIncorrectClass() throws Exception {
    ProtoAdapter.get("C#ADAPTER");
    fail("ProtoAdapter should throw IllegalArgumentException when path contains wrong class");
  }

  @Test(expected = IllegalArgumentException.class) public void staticGetIncorrectField() throws Exception {
    ProtoAdapter.get("com.squareup.wire.protos.roots.C#protoAdapter");
    fail("ProtoAdapter should throw IllegalArgumentException when path contains wrong field");
  }

  @Test(expected = IllegalArgumentException.class) public void staticGetIncorrectFieldAccess() throws Exception {
    ProtoAdapter.get("com.squareup.wire.protos.roots.C#serialVersionUID");
    fail("ProtoAdapter should throw IllegalArgumentException when path contains inaccessible field");
  }

  @Test(expected = ClassCastException.class) public void staticGetIncorrectFieldType() throws Exception {
    ProtoAdapter.get("com.squareup.wire.protos.roots.C#DEFAULT_I");
    fail("ProtoAdapter should throw ClassCastException when path contains field with incorrect type");
  }

  @Test public void redacted() throws Exception {
    assertThat(ProtoAdapter.INT32.redact(null)).isNull();
    assertThat(ProtoAdapter.INT32.redact(20)).isNull();
  }

  @Test(expected = NullPointerException.class) public void encodeNull() throws Exception {
    // given
    ProtoAdapter<C> protoAdapter = createErrorProtoAdapter();

    // when
    protoAdapter.encode(null);

    // then
    fail("ProtoAdapter should throw NullPointerException when null value is passed to encode");
  }

  @Test public void encodeIOError() throws Exception {
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

  @Test public void boolEncode() throws Exception {
    // given
    ProtoWriter protoWriterTrue = mock(ProtoWriter.class);
    ProtoWriter protoWriterFalse = mock(ProtoWriter.class);

    // when
    ProtoAdapter.BOOL.encode(protoWriterTrue, true);
    ProtoAdapter.BOOL.encode(protoWriterFalse, false);

    // then
    Mockito.verify(protoWriterTrue).writeVarint32(1);
    Mockito.verify(protoWriterFalse).writeVarint32(0);
  }

  @Test public void boolDecode() throws Exception {
    // given
    ProtoReader protoReader0 = mock(ProtoReader.class);
    ProtoReader protoReader1 = mock(ProtoReader.class);

    // when
    when(protoReader0.readVarint32()).thenReturn(0);
    when(protoReader1.readVarint32()).thenReturn(1);

    // then
    assertThat(ProtoAdapter.BOOL.decode(protoReader0)).isEqualTo(Boolean.FALSE);
    assertThat(ProtoAdapter.BOOL.decode(protoReader1)).isEqualTo(Boolean.TRUE);
  }

  @Test(expected = IOException.class) public void boolDecodeUnknown() throws Exception {
    // given
    ProtoReader protoReader = mock(ProtoReader.class);

    // when
    when(protoReader.readVarint32()).thenReturn(-1);
    ProtoAdapter.BOOL.decode(protoReader);

    // then
    fail("ProtoAdapter should throw IOException when non bit value is returned by reader");
  }


  @Test public void withLabel() throws Exception {
    // given
    WireField.Label repeatedPackedLabel = mock(WireField.Label.class);
    WireField.Label repeatedNotPackedLabel = mock(WireField.Label.class);
    WireField.Label notRepeatedPackedLabel = mock(WireField.Label.class);
    WireField.Label notRepeatedNotPackedLabel = mock(WireField.Label.class);

    // when
    when(repeatedPackedLabel.isRepeated()).thenReturn(true);
    when(repeatedPackedLabel.isPacked()).thenReturn(true);

    when(repeatedNotPackedLabel.isRepeated()).thenReturn(true);
    when(repeatedNotPackedLabel.isPacked()).thenReturn(false);

    when(notRepeatedPackedLabel.isRepeated()).thenReturn(false);
    when(notRepeatedPackedLabel.isPacked()).thenReturn(true);

    when(notRepeatedNotPackedLabel.isRepeated()).thenReturn(false);
    when(notRepeatedNotPackedLabel.isPacked()).thenReturn(false);

    // then
    assertThat(ProtoAdapter.BOOL.withLabel(repeatedPackedLabel)).isEqualTo(ProtoAdapter.BOOL.asPacked());
    assertThat(ProtoAdapter.BOOL.withLabel(repeatedNotPackedLabel)).isEqualTo(ProtoAdapter.BOOL.asRepeated());
    assertThat(ProtoAdapter.BOOL.withLabel(notRepeatedPackedLabel)).isEqualTo(ProtoAdapter.BOOL);
    assertThat(ProtoAdapter.BOOL.withLabel(notRepeatedNotPackedLabel)).isEqualTo(ProtoAdapter.BOOL);
  }

  @Test(expected = IllegalArgumentException.class) public void createPackedForNonPackableFieldAdapter() throws Exception {
    // when
    Whitebox.invokeMethod(ProtoAdapter.STRING, "createPacked");

    // then
    fail("ProtoAdapter should throw IllegalArgumentException when non packable field adapter tries to create packed adapter");
  }

  @Test public void createPackedAdapterDecode() throws Exception {
    // given
    ProtoAdapter<List> packedAdapter = Whitebox.invokeMethod(ProtoAdapter.BOOL, "createPacked");
    ProtoReader protoReader = mock(ProtoReader.class);

    // when
    when(protoReader.readVarint32()).thenReturn(1);

    // then
    assertThat(packedAdapter.decode(protoReader)).isEqualTo(Collections.singletonList(Boolean.TRUE));
  }

  @Test public void createPackedAdapterRedacted() throws Exception {
    // when
    ProtoAdapter<List> packedAdapter = Whitebox.invokeMethod(ProtoAdapter.BOOL, "createPacked");

    // then
    assertThat(packedAdapter.redact(Arrays.asList(true, false))).isEqualTo(Collections.emptyList());
  }

  @Test(expected = UnsupportedOperationException.class) public void createRepeatedAdapterEncodedSize() throws Exception {
    // given
    ProtoAdapter<List<Boolean>> repeatedAdapter = Whitebox.invokeMethod(ProtoAdapter.BOOL, "createRepeated");

    // when
    repeatedAdapter.encodedSize(Arrays.asList(true, false));

    // then
    fail("Repeated ProtoAdapter should throw UnsupportedOperationException on encodedSize call");
  }

  @Test(expected = UnsupportedOperationException.class) public void createRepeatedAdapterEncode() throws Exception {
    // given
    ProtoAdapter<List<Boolean>> repeatedAdapter = Whitebox.invokeMethod(ProtoAdapter.BOOL, "createRepeated");
    ProtoWriter protoWriter = mock(ProtoWriter.class);

    // when
    repeatedAdapter.encode(protoWriter, Arrays.asList(true, false));

    // then
    fail("Repeated ProtoAdapter should throw UnsupportedOperationException on encode call");
  }


  @Test public void createRepeatedAdapterDecode() throws Exception {
    // given
    ProtoAdapter<List> repeatedAdapter = Whitebox.invokeMethod(ProtoAdapter.BOOL, "createRepeated");
    ProtoReader protoReader = mock(ProtoReader.class);

    // when
    when(protoReader.readVarint32()).thenReturn(1);

    // then
    assertThat(repeatedAdapter.decode(protoReader)).isEqualTo(Collections.singletonList(Boolean.TRUE));
  }

  @Test public void createRepeatedAdapterRedacted() throws Exception {
    // when
    ProtoAdapter<List> repeatedAdapter = Whitebox.invokeMethod(ProtoAdapter.BOOL, "createRepeated");

    // then
    assertThat(repeatedAdapter.redact(Arrays.asList(true, false))).isEqualTo(Collections.emptyList());
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
