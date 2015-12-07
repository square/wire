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
import okio.BufferedSink;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProtoWriter.class, Buffer.class})
public final class ProtoWriterTest {
  @Test public void utf8() throws IOException {
    // 0 byte strings.
    assertUtf8("", "");

    // 1 byte code points.
    assertUtf8("\u0000", "00");
    assertUtf8("A", "41");

    // 2 byte code points.
    assertUtf8("\u0080", "c280");
    assertUtf8("\u07ff", "dfbf");

    // 3 byte code points.
    assertUtf8("\u0800", "e0a080");
    assertUtf8("\ud7ff", "ed9fbf");
    assertUtf8("\ue000", "ee8080");
    assertUtf8("\uffff", "efbfbf");

    // 4 byte code points, in Java as a high surrogate followed by low surrogate.
    assertUtf8("\ud800\udc00", "f0908080");

    // Malformed UTF-16.
    assertUtf8("\ud800", "3f"); // Dangling high surrogate.
    assertUtf8("\ud800A", "3f41"); // High surrogate followed by a 1 byte code point.
    assertUtf8("\ud800\ue000", "3fee8080"); // High surrogate followed by a 3 byte code point.
    assertUtf8("\ud800\ud800\udc00", "3ff0908080"); // High surrogate followed by surrogate pair.
    assertUtf8("\udc00A", "3f41"); // Unexpected low surrogate.
    assertUtf8("\udc00", "3f"); // Unexpected, dangling low surrogate.
  }

  @Test public void staticInt32Size() throws Exception {
    assertThat(ProtoWriter.int32Size(0)).isEqualTo(1);
    assertThat(ProtoWriter.int32Size(127)).isEqualTo(1);
    assertThat(ProtoWriter.int32Size(128)).isEqualTo(2);
    assertThat(ProtoWriter.int32Size(16383)).isEqualTo(2);
    assertThat(ProtoWriter.int32Size(16384)).isEqualTo(3);
    assertThat(ProtoWriter.int32Size(2097151)).isEqualTo(3);
    assertThat(ProtoWriter.int32Size(2097152)).isEqualTo(4);
    assertThat(ProtoWriter.int32Size(268435455)).isEqualTo(4);
    assertThat(ProtoWriter.int32Size(268435456)).isEqualTo(5);
    assertThat(ProtoWriter.int32Size(Integer.MAX_VALUE)).isEqualTo(5);
    assertThat(ProtoWriter.int32Size(Integer.MIN_VALUE)).isEqualTo(10);
    assertThat(ProtoWriter.int32Size(-1)).isEqualTo(10);
  }

  @Test public void staticVarInt32Size() throws Exception {
    assertThat(ProtoWriter.varint32Size(0)).isEqualTo(1);
    assertThat(ProtoWriter.varint32Size(127)).isEqualTo(1);
    assertThat(ProtoWriter.varint32Size(128)).isEqualTo(2);
    assertThat(ProtoWriter.varint32Size(16383)).isEqualTo(2);
    assertThat(ProtoWriter.varint32Size(16384)).isEqualTo(3);
    assertThat(ProtoWriter.varint32Size(2097151)).isEqualTo(3);
    assertThat(ProtoWriter.varint32Size(2097152)).isEqualTo(4);
    assertThat(ProtoWriter.varint32Size(268435455)).isEqualTo(4);
    assertThat(ProtoWriter.varint32Size(268435456)).isEqualTo(5);
  }

  @Ignore("Negative numbers processing must be supported in varint32Size")
  @Test(expected = IllegalArgumentException.class) public void staticVarint32SizeNegative() throws Exception {
    ProtoWriter.varint32Size(-1);
    fail("ProtoWriter should throw IllegalArgumentException when argument is negative");
  }

  @Ignore("Negative numbers processing must be supported in varint32Size")
  @Test(expected = IllegalArgumentException.class) public void staticVarint32SizeMinValue() throws Exception {
    ProtoWriter.varint32Size(Integer.MIN_VALUE);
    fail("ProtoWriter should throw IllegalArgumentException when argument is negative");
  }

  @Test public void staticVarInt64Size() throws Exception {
    assertThat(ProtoWriter.varint64Size(0L)).isEqualTo(1);
    assertThat(ProtoWriter.varint64Size(127L)).isEqualTo(1);
    assertThat(ProtoWriter.varint64Size(128L)).isEqualTo(2);
    assertThat(ProtoWriter.varint64Size(16383L)).isEqualTo(2);
    assertThat(ProtoWriter.varint64Size(16384L)).isEqualTo(3);
    assertThat(ProtoWriter.varint64Size(2097151L)).isEqualTo(3);
    assertThat(ProtoWriter.varint64Size(2097152L)).isEqualTo(4);
    assertThat(ProtoWriter.varint64Size(268435455L)).isEqualTo(4);
    assertThat(ProtoWriter.varint64Size(268435456L)).isEqualTo(5);
    assertThat(ProtoWriter.varint64Size(34359738367L)).isEqualTo(5);
    assertThat(ProtoWriter.varint64Size(34359738368L)).isEqualTo(6);
    assertThat(ProtoWriter.varint64Size(4398046511103L)).isEqualTo(6);
    assertThat(ProtoWriter.varint64Size(4398046511104L)).isEqualTo(7);
    assertThat(ProtoWriter.varint64Size(562949953421311L)).isEqualTo(7);
    assertThat(ProtoWriter.varint64Size(562949953421312L)).isEqualTo(8);
    assertThat(ProtoWriter.varint64Size(72057594037927935L)).isEqualTo(8);
    assertThat(ProtoWriter.varint64Size(72057594037927936L)).isEqualTo(9);
    assertThat(ProtoWriter.varint64Size(9223372036854775807L)).isEqualTo(9);
    assertThat(ProtoWriter.varint64Size(Long.MAX_VALUE)).isEqualTo(9);
  }

  @Ignore("Negative numbers processing must be supported in staticVarInt64Size")
  @Test(expected = IllegalArgumentException.class) public void staticVarInt64SizeNegative() throws Exception {
    ProtoWriter.varint64Size(-1L);
    fail("ProtoWriter should throw IllegalArgumentException when argument is negative");
  }

  @Ignore("Negative numbers processing must be supported in staticVarInt64Size")
  @Test(expected = IllegalArgumentException.class) public void staticVarInt64SizeMinValue() throws Exception {
    ProtoWriter.varint64Size(Long.MIN_VALUE);
    fail("ProtoWriter should throw IllegalArgumentException when argument is negative");
  }

  @Test public void constructor() throws Exception {
    // given
    BufferedSink sink = new Buffer();

    // when
    ProtoWriter protoWriter = new ProtoWriter(sink);

    // then
    assertThat(Whitebox.getInternalState(protoWriter, "sink")).isEqualTo(sink);
  }

  @Test public void writeSignedVarint32Case1() throws Exception {
    // given
    BufferedSink sink = mock(Buffer.class);
    ProtoWriter protoWriter = new ProtoWriter(sink);

    // when
    protoWriter.writeSignedVarint32(1);

    // then
    Mockito.verify(sink, Mockito.never()).writeByte(0);
    Mockito.verify(sink, Mockito.times(1)).writeByte(1);
    Mockito.verify(sink, Mockito.never()).writeByte(7);
    Mockito.verify(sink, Mockito.never()).writeByte(255);
  }

  @Test public void writeSignedVarint32Case2() throws Exception {
    // given
    BufferedSink sink = mock(Buffer.class);
    ProtoWriter protoWriter = new ProtoWriter(sink);

    // when
    protoWriter.writeSignedVarint32(0);

    // then
    Mockito.verify(sink, Mockito.times(1)).writeByte(0);
    Mockito.verify(sink, Mockito.never()).writeByte(1);
    Mockito.verify(sink, Mockito.never()).writeByte(7);
    Mockito.verify(sink, Mockito.never()).writeByte(255);
  }

  @Test public void writeSignedVarint32Case3() throws Exception {
    // given
    BufferedSink sink = mock(Buffer.class);
    ProtoWriter protoWriter = new ProtoWriter(sink);

    // when
    protoWriter.writeSignedVarint32(Integer.MAX_VALUE);

    // then
    Mockito.verify(sink, Mockito.never()).writeByte(0);
    Mockito.verify(sink, Mockito.never()).writeByte(1);
    Mockito.verify(sink, Mockito.times(1)).writeByte(7);
    Mockito.verify(sink, Mockito.times(4)).writeByte(255);
  }

  @Test public void writeSignedVarint32Case4() throws Exception {
    // given
    BufferedSink sink = mock(Buffer.class);
    ProtoWriter protoWriter = new ProtoWriter(sink);

    // when
    protoWriter.writeSignedVarint32(-1);

    // then
    Mockito.verify(sink, Mockito.never()).writeByte(0);
    Mockito.verify(sink, Mockito.times(1)).writeByte(1);
    Mockito.verify(sink, Mockito.never()).writeByte(7);
    Mockito.verify(sink, Mockito.times(9)).writeByte(255);
  }

  private void assertUtf8(String string, String expectedHex) throws IOException {
    Buffer buffer = new Buffer();
    ProtoWriter writer = new ProtoWriter(buffer);
    writer.writeString(string);
    assertThat(buffer.readByteString().hex()).isEqualTo(expectedHex);
    assertThat(ProtoWriter.utf8Length(string)).isEqualTo(expectedHex.length() / 2);
  }
}
