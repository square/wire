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

import org.junit.Test;

import java.io.IOException;
import java.net.ProtocolException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class FieldEncodingTest {

  @Test public void get() throws IOException {
    assertThat(FieldEncoding.get(0)).isEqualTo(FieldEncoding.VARINT);
    assertThat(FieldEncoding.get(5)).isEqualTo(FieldEncoding.FIXED32);
    assertThat(FieldEncoding.get(1)).isEqualTo(FieldEncoding.FIXED64);
    assertThat(FieldEncoding.get(2)).isEqualTo(FieldEncoding.LENGTH_DELIMITED);
  }

  @Test(expected = ProtocolException.class) public void getThrows() throws IOException {
    // when
    FieldEncoding.get(3);

    // then
    fail("FieldEncoding should throw ProtocolException when unknown encoding type is requested");
  }

  @Test public void rawProtoAdapter() {
    assertThat(FieldEncoding.VARINT.rawProtoAdapter()).isEqualTo(ProtoAdapter.UINT64);
    assertThat(FieldEncoding.FIXED32.rawProtoAdapter()).isEqualTo(ProtoAdapter.FIXED32);
    assertThat(FieldEncoding.FIXED64.rawProtoAdapter()).isEqualTo(ProtoAdapter.FIXED64);
    assertThat(FieldEncoding.LENGTH_DELIMITED.rawProtoAdapter()).isEqualTo(ProtoAdapter.BYTES);
  }
}
