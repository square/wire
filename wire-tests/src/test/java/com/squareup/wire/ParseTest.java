/*
 * Copyright 2014 Square Inc.
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

import com.squareup.wire.protos.edgecases.OneBytesField;
import com.squareup.wire.protos.edgecases.OneField;
import com.squareup.wire.protos.edgecases.Recursive;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import okio.ByteString;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class ParseTest {
  @Test public void unknownTagIgnored() throws Exception {
    // tag 1 / type 0: 456
    // tag 2 / type 0: 789
    ByteString data = ByteString.decodeHex("08c803109506");
    OneField oneField = OneField.ADAPTER.decode(data.toByteArray());
    OneField expected = new OneField.Builder().opt_int32(456).build();
    assertThat(oneField).isNotEqualTo(expected);
    assertThat(oneField.withoutUnknownFields()).isEqualTo(expected);
  }

  @Test public void unknownTypeThrowsIOException() throws Exception {
    // tag 1 / type 0: 456
    // tag 2 / type 7: 789
    ByteString data = ByteString.decodeHex("08c803179506");
    try {
      OneField.ADAPTER.decode(data.toByteArray());
      fail();
    } catch (ProtocolException expected) {
      assertThat(expected).hasMessage("Unexpected field encoding: 7");
    }
  }

  @Ignore("TODO(jwilson)")
  @Test public void typeMismatchHonorsWireDeclaredType() throws Exception {
    // tag 1 / 3-byte length-delimited string: 0x109506
    // (0x109506 is a well-formed proto message that sets tag 2 to 456).
    ByteString data = ByteString.decodeHex("0a03109506");
    OneField oneField = OneField.ADAPTER.decode(data.toByteArray());
    assertThat(oneField).isEqualTo(new OneField.Builder().opt_int32(3).build());
  }

  @Test public void truncatedMessageThrowsEOFException() throws Exception {
    // tag 1 / 4-byte length delimited string: 0x000000 (3 bytes)
    ByteString data = ByteString.decodeHex("0a04000000");
    try {
      OneBytesField.ADAPTER.decode(data.toByteArray());
      fail();
    } catch (EOFException expected) {
    }
  }

  @Ignore("we no longer enforce this constraint")
  @Test public void repeatedUnknownValueWithDifferentTypesThrowsIOException() throws Exception {
    // tag 2 / 3-byte length-delimited string: 0x109506
    // tag 2 / type 0: 456
    ByteString data = ByteString.decodeHex("120300000010c803");
    try {
      OneField.ADAPTER.decode(data.toByteArray());
      fail();
    } catch (ProtocolException expected) {
      assertThat(expected).hasMessage(
          "Wire type VARINT differs from previous type LENGTH_DELIMITED for tag 2");
    }
  }

  @Test public void lastValueWinsForRepeatedValueOfNonrepeatedField() throws Exception {
    // tag 1 / type 0: 456
    // tag 1 / type 0: 789
    ByteString data = ByteString.decodeHex("08c803089506");
    OneField oneField = OneField.ADAPTER.decode(data.toByteArray());
    assertThat(new OneField.Builder().opt_int32(789).build()).isEqualTo(oneField);
  }

  @Test public void upToRecursionLimit() throws Exception {
    // tag 2: nested message (64 times)
    // tag 1: signed varint32 456
    ByteString data = ByteString.decodeHex("127e127c127a12781276127412721270126e126c126a12681266126"
        + "412621260125e125c125a12581256125412521250124e124c124a12481246124412421240123e123c123a123"
        + "81236123412321230122e122c122a12281226122412221220121e121c121a12181216121412121210120e120"
        + "c120a1208120612041202120008c803");
    Recursive recursive = Recursive.ADAPTER.decode(data.toByteArray());
    assertThat(recursive.value.intValue()).isEqualTo(456);
  }

  @Test public void overRecursionLimitThrowsIOException() throws Exception {
    // tag 2: nested message (65 times)
    // tag 1: signed varint32 456
    ByteString data = ByteString.decodeHex("128001127e127c127a12781276127412721270126e126c126a12681"
        + "266126412621260125e125c125a12581256125412521250124e124c124a12481246124412421240123e123c1"
        + "23a12381236123412321230122e122c122a12281226122412221220121e121c121a121812161214121212101"
        + "20e120c120a1208120612041202120008c803");
    try {
      Recursive.ADAPTER.decode(data.toByteArray());
      fail();
    } catch (IOException expected) {
      assertThat(expected).hasMessage("Wire recursion limit exceeded");
    }
  }
}
