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
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ProtocolException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public final class ProtoReaderTest {

  private ProtoReader defaultProtoReader;

  @Before
  public void init() {
    Buffer defaultContent = new Buffer().write("some text".getBytes());
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

  @Test public void endMessageIllegalState() throws IOException {
    try {
      // when
      defaultProtoReader.endMessage(1);

      // then
      fail("ProtoReader should throw IllegalStateException when endMessage call is performed while"
        + " reader is not in STATE_TAG state");
    }
    catch (IllegalStateException e) {
      assertThat(e).hasMessage("Unexpected call to endMessage()");
    }
  }

  @Test public void nextTagIllegalState() throws IOException {
    try {
      // when
      defaultProtoReader.nextTag();

      // then
      fail("ProtoReader should throw IllegalStateException when beginMessage call is performed while"
        + " reader is not in STATE_LENGTH_DELIMITED state");
    }
    catch (IllegalStateException e) {
      assertThat(e).hasMessage("Unexpected call to nextTag()");
    }
  }

  @Test public void unexpectedEndOfGroup() throws IOException {
    // given
    ByteString encoded = ByteString.decodeHex("130a01611c");
    ProtoReader reader = new ProtoReader(new Buffer().write(encoded));

    // when
    reader.beginMessage();

    try {
      reader.nextTag();

      // then
      fail("ProtoReader should throw ProtocolException when read group is not closed properly");
    }
    catch (ProtocolException e) {
      assertThat(e).hasMessage("Unexpected end group");
    }
  }

  @Test public void readVarint32Negative() throws IOException {
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

  @Test public void readVarint32Big1() throws IOException {
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

  @Test public void readVarint32Big2() throws IOException {
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

  @Test public void readVarint32Big3() throws IOException {
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

  @Test public void readVarint32Big4() throws IOException {
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

  @Test public void unexpectedSkip() throws IOException {
    // given
    ByteString encoded = ByteString.of(C.ADAPTER.encode(new C(15)));
    ProtoReader reader = new ProtoReader(new Buffer().write(encoded));

    // when
    reader.beginMessage();

    try {
      reader.skip();

      // then
      fail("ProtoReader should throw IllegalStateException when skip is called in illegal state");
    }
    catch (IllegalStateException e) {
      assertThat(e).hasMessage("Unexpected call to skip()");
    }
  }


  @Test public void unexpectedSkipGroup() throws IOException {
    // given
    ByteString encoded = ByteString.of(C.ADAPTER.encode(new C(0)));
    ProtoReader reader = new ProtoReader(new Buffer().write(encoded));

    // when
    reader.beginMessage();

    try {
      reader.skip();

      // then
      fail("ProtoReader should throw IllegalStateException when skip is called in illegal state");
    }
    catch (IllegalStateException e) {
      assertThat(e).hasMessage("Unexpected call to skip()");
    }
  }
}
