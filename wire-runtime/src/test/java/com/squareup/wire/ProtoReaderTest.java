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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class ProtoReaderTest {
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
}
