/*
 * Copyright 2013 Square Inc.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import okio.ByteString;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ByteStringTest {
  @Test
  public void byteStringBase64() throws Exception {
    ByteString byteString = ByteString.of((byte) 0x0, (byte) 0x1, (byte) 0x2, (byte) 0xf,
        (byte) 0x10, (byte) 0x11, (byte) 0x7f, (byte) 0x80, (byte) 0x81, (byte) 0xfe, (byte) 0xff);
    assertEquals("AAECDxARf4CB/v8=", byteString.base64());
  }

  @Test
  public void byteStringEquals() throws Exception {
    ByteString byteString = ByteString.of((byte) 0x0, (byte) 0x1, (byte) 0x2);
    assertTrue(byteString.equals(byteString));
    assertTrue(byteString.equals(ByteString.of((byte) 0x0, (byte) 0x1, (byte) 0x2)));
    assertFalse(byteString.equals(new Object()));
    assertFalse(byteString.equals(ByteString.of((byte) 0x0, (byte) 0x2, (byte) 0x1)));
  }

  @Test
  public void byteStringHashCode() throws Exception {
    ByteString byteString = ByteString.of((byte) 0x1, (byte) 0x2);
    assertEquals(byteString.hashCode(), byteString.hashCode());
    assertEquals(byteString.hashCode(), ByteString.of((byte) 0x1, (byte) 0x2).hashCode());
  }

  @Test
  public void read() throws Exception {
    InputStream in = new ByteArrayInputStream("abc".getBytes("UTF-8"));
    assertEquals(ByteString.of((byte) 0x61, (byte) 0x62), ByteString.read(in, 2));
    assertEquals(ByteString.of((byte) 0x63), ByteString.read(in, 1));
    assertEquals(ByteString.of(), ByteString.read(in, 0));
  }

  @Test
  public void write() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteString.of((byte) 0x61, (byte) 0x62, (byte) 0x63).write(out);
    assertByteArraysEquals(new byte[] { 0x61, 0x62, 0x63 }, out.toByteArray());
  }

  private static void assertByteArraysEquals(byte[] a, byte[] b) {
    assertEquals(Arrays.toString(a), Arrays.toString(b));
  }
}
