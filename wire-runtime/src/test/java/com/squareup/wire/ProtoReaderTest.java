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

import java.io.IOException;
import okio.Buffer;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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

}
