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
import com.squareup.wire.protos.roots.RedactFieldsMessage;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ProtoReader.class })
public final class ProtoReaderTest {

  private Buffer defaultContent;
  private ProtoReader defaultProtoReader;

  @Before
  public void init() {
    defaultContent = new Buffer().write("some text".getBytes());
    defaultProtoReader = new ProtoReader(defaultContent);
  }

  @Test public void packedExposedAsRepeated() throws IOException {
    ByteString packedEncoded = ByteString.decodeHex("d20504d904bd05");
    ProtoReader reader = new ProtoReader(new Buffer().write(packedEncoded));
    long token = reader.beginMessage();
    assertThat(reader.nextTag()).isEqualTo(90);
    assertThat(ProtoAdapter.INT32.decode(reader)).isEqualTo(601);
    assertThat(reader.nextTag()).isEqualTo(90);
    assertThat(ProtoAdapter.INT32.decode(reader)).isEqualTo(701);
    assertThat(reader.nextTag()).isEqualTo(-1);
    reader.endMessage(token);
  }

  @Test public void constructor() throws Exception {
    assertThat(Whitebox.getInternalState(defaultProtoReader, "source")).isEqualTo(defaultContent);
  }


  @Test(expected = IllegalStateException.class) public void beginMessageIllegalState() throws Exception {
    // given
    Whitebox.setInternalState(defaultProtoReader, "state", 6);    // STATE_TAG

    // when
    defaultProtoReader.beginMessage();

    // then
    fail("ProtoReader should throw IllegalStateException when beginMessage call is performed while"
      + " reader is not in STATE_LENGTH_DELIMITED state");
  }

  @Test(expected = IllegalStateException.class) public void endMessageIllegalState() throws Exception {
    // when
    defaultProtoReader.endMessage(1);

    // then
    fail("ProtoReader should throw IllegalStateException when endMessage call is performed while"
      + " reader is not in STATE_TAG state");
  }

  @Test(expected = IllegalStateException.class) public void endMessageIllegalRecursionDepth() throws Exception {
    // given
    Whitebox.setInternalState(defaultProtoReader, "state", 6);    // STATE_TAG

    // when
    defaultProtoReader.endMessage(1);

    // then
    fail("ProtoReader should throw IllegalStateException when endMessage call is performed while"
      + " reader recursion depth is exceeded");
  }

  @Test(expected = IllegalStateException.class) public void endMessageIllegalPushedLimit() throws Exception {
    // given
    Whitebox.setInternalState(defaultProtoReader, "state", 6);    // STATE_TAG
    Whitebox.setInternalState(defaultProtoReader, "recursionDepth", 1);
    Whitebox.setInternalState(defaultProtoReader, "pushedLimit", 1);

    // when
    defaultProtoReader.endMessage(1);

    // then
    fail("ProtoReader should throw IllegalStateException when endMessage call is performed while"
      + " reader pushed limit is not -1");
  }

  @Test(expected = IOException.class) public void endMessageIllegalPos() throws Exception {
    // given
    Whitebox.setInternalState(defaultProtoReader, "state", 6);    // STATE_TAG
    Whitebox.setInternalState(defaultProtoReader, "recursionDepth", 2);

    // when
    defaultProtoReader.endMessage(1);

    // then
    fail("ProtoReader should throw IOException when endMessage call is performed with invalid I/O state");
  }

  @Test(expected = IllegalStateException.class) public void nextTagIllegalState() throws Exception {
    // when
    defaultProtoReader.nextTag();

    // then
    fail("ProtoReader should throw IllegalStateException when beginMessage call is performed while"
      + " reader is not in STATE_LENGTH_DELIMITED state");
  }

  @Test(expected = ProtocolException.class) public void unexpectedEndOfGroup() throws Exception {
    // given
    ByteString encoded = ByteString.decodeHex("130a01611c");
    ProtoReader reader = new ProtoReader(new Buffer().write(encoded));

    // when
    reader.beginMessage();
    reader.nextTag();

    // then
    fail("ProtoReader should throw ProtocolException when read group is not closed properly");
  }

  @Test(expected = ProtocolException.class) public void readVarint32IllegalState() throws Exception {
    // given
    Whitebox.setInternalState(defaultProtoReader, "state", 6);    // STATE_TAG

    // when
    defaultProtoReader.readVarint32();

    // then
    fail("ProtoReader should throw ProtocolException when readVarint32 call is performed in illegal state");
  }

  @Test(expected = ProtocolException.class) public void readVarint32MalformedValue() throws Exception {
    // given
    BufferedSource source = mock(BufferedSource.class);
    PowerMockito.when(source.readByte()).thenReturn((byte)-1);
    Whitebox.setInternalState(defaultProtoReader, "source", source);    // STATE_TAG

    // when
    defaultProtoReader.readVarint32();

    // then
    fail("ProtoReader should throw ProtocolException when readVarint32 reads malformed value");
  }

  @Test public void readVarint32Negative() throws Exception {
    // given
    RedactFieldsMessage message = new RedactFieldsMessage(0, 0L, Integer.MIN_VALUE, 0, null, null, null, null);

    ByteString encoded = ByteString.of(RedactFieldsMessage.ADAPTER.encode(message));
    ProtoReader reader = new ProtoReader(new Buffer().write(encoded));

    // when
    reader.beginMessage();
    reader.nextTag();
    reader.skip();
    reader.nextTag();
    reader.skip();
    reader.nextTag();

    // then
    assertThat(reader.readVarint32()).isEqualTo(Integer.MIN_VALUE);
  }

  @Test public void readVarint32Big1() throws Exception {
    // given
    RedactFieldsMessage message = new RedactFieldsMessage(0, 0L, 1024, 0, null, null, null, null);

    ByteString encoded = ByteString.of(RedactFieldsMessage.ADAPTER.encode(message));
    ProtoReader reader = new ProtoReader(new Buffer().write(encoded));

    // when
    reader.beginMessage();
    reader.nextTag();
    reader.skip();
    reader.nextTag();
    reader.skip();
    reader.nextTag();

    // then
    assertThat(reader.readVarint32()).isEqualTo(1024);
  }

  @Test public void readVarint32Big2() throws Exception {
    // given
    RedactFieldsMessage message = new RedactFieldsMessage(0, 0L, 1048575, 0, null, null, null, null);

    ByteString encoded = ByteString.of(RedactFieldsMessage.ADAPTER.encode(message));
    ProtoReader reader = new ProtoReader(new Buffer().write(encoded));

    // when
    reader.beginMessage();
    reader.nextTag();
    reader.skip();
    reader.nextTag();
    reader.skip();
    reader.nextTag();

    // then
    assertThat(reader.readVarint32()).isEqualTo(1048575);
  }

  @Test public void readVarint32Big3() throws Exception {
    // given
    RedactFieldsMessage message = new RedactFieldsMessage(0, 0L, 16777216, 0, null, null, null, null);

    ByteString encoded = ByteString.of(RedactFieldsMessage.ADAPTER.encode(message));
    ProtoReader reader = new ProtoReader(new Buffer().write(encoded));

    // when
    reader.beginMessage();
    reader.nextTag();
    reader.skip();
    reader.nextTag();
    reader.skip();
    reader.nextTag();

    // then
    assertThat(reader.readVarint32()).isEqualTo(16777216);
  }

  @Test public void readVarint32Big4() throws Exception {
    // given
    RedactFieldsMessage message = new RedactFieldsMessage(0, 0L, 1073741824, 0, null, null, null, null);

    ByteString encoded = ByteString.of(RedactFieldsMessage.ADAPTER.encode(message));
    ProtoReader reader = new ProtoReader(new Buffer().write(encoded));

    // when
    reader.beginMessage();
    reader.nextTag();
    reader.skip();
    reader.nextTag();
    reader.skip();
    reader.nextTag();

    // then
    assertThat(reader.readVarint32()).isEqualTo(1073741824);
  }

  @Test(expected = ProtocolException.class) public void readVarint64IllegalState() throws Exception {
    // given
    Whitebox.setInternalState(defaultProtoReader, "state", 6);    // STATE_TAG

    // when
    defaultProtoReader.readVarint64();

    // then
    fail("ProtoReader should throw ProtocolException when readVarint64 call is performed in illegal state");
  }

  @Test(expected = ProtocolException.class) public void readFixed32IllegalState() throws Exception {
    // given
    Whitebox.setInternalState(defaultProtoReader, "state", 6);    // STATE_TAG

    // when
    defaultProtoReader.readFixed32();

    // then
    fail("ProtoReader should throw ProtocolException when readFixed32 call is performed in illegal state");
  }

  @Test(expected = ProtocolException.class) public void readFixed64IllegalState() throws Exception {
    // given
    Whitebox.setInternalState(defaultProtoReader, "state", 6);    // STATE_TAG

    // when
    defaultProtoReader.readFixed64();

    // then
    fail("ProtoReader should throw ProtocolException when readFixed64 call is performed in illegal state");
  }

  @Test public void skipVarint() throws Exception {
    // given
    ByteString encoded = ByteString.of(C.ADAPTER.encode(new C(15)));
    ProtoReader reader = spy(new ProtoReader(new Buffer().write(encoded)));

    // when
    reader.beginMessage();
    reader.nextTag();
    reader.skip();

    // then
    Mockito.verify(reader).readVarint64();
  }

  @Test public void skip() throws Exception {
    RedactFieldsMessage message = new RedactFieldsMessage(10, 20L, null, 45,
      new C(51), new C(61), Arrays.asList(new C(3), new C(7), new C(5)), Arrays.asList(new C(2), new C(4)));

    ByteString encoded = ByteString.of(RedactFieldsMessage.ADAPTER.encode(message));
    ProtoReader reader = spy(new ProtoReader(new Buffer().write(encoded)));

    // when
    reader.beginMessage();
    reader.nextTag();
    reader.skip();
    reader.nextTag();
    reader.skip();
    reader.nextTag();
    reader.skip();
    reader.nextTag();
    reader.skip();

    // then
    Mockito.verify(reader).readFixed32();
    Mockito.verify(reader).readFixed64();
    Mockito.verify(reader, Mockito.never()).readVarint32();
    Mockito.verify(reader).readVarint64();
  }


  @Test(expected = IllegalStateException.class) public void unexpectedSkip() throws Exception {
    // given
    ByteString encoded = ByteString.of(C.ADAPTER.encode(new C(15)));
    ProtoReader reader = new ProtoReader(new Buffer().write(encoded));

    // when
    reader.beginMessage();
    reader.skip();

    // then
    fail("ProtoReader should throw IllegalStateException when skip is called in illegal state");
  }


  @Test(expected = IllegalStateException.class) public void unexpectedSkipGroup() throws Exception {
    // given
    ByteString encoded = ByteString.of(C.ADAPTER.encode(new C(0)));
    ProtoReader reader = new ProtoReader(new Buffer().write(encoded));

    // when
    reader.beginMessage();
    reader.skip();

    // then
    fail("ProtoReader should throw IllegalStateException when skip is called in illegal state");
  }
}
