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

import com.squareup.wire.protos.unknownfields.VersionOne;
import com.squareup.wire.protos.unknownfields.VersionTwo;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class UnknownFieldsTest {

  private final Wire wire = new Wire();
  private final MessageAdapter<VersionOne> v1Adapter = wire.adapter(VersionOne.class);
  private final MessageAdapter<VersionTwo> v2Adapter = wire.adapter(VersionTwo.class);

  @Test
  public void testUnknownFields() throws IOException {
    VersionTwo v2 = new VersionTwo.Builder()
       .i(111)
       .v2_i(12345)
       .v2_s("222")
       .v2_f32(67890)
       .v2_f64(98765L)
       .v2_rs(Arrays.asList("1", "2"))
       .build();
    assertEquals(new Integer(111), v2.i);
    // Check v.2 fields
    assertEquals(new Integer(12345), v2.v2_i);
    assertEquals("222", v2.v2_s);
    assertEquals(new Integer(67890), v2.v2_f32);
    assertEquals(new Long(98765L), v2.v2_f64);
    assertEquals(Arrays.asList("1", "2"), v2.v2_rs);
    // Serialized
    byte[] v2Bytes = v2Adapter.writeBytes(v2);

    // Parse
    VersionOne v1 = v1Adapter.readBytes(v2Bytes);
    // v.1 fields are visible, v.2 fields are in unknownFieldSet
    assertEquals(new Integer(111), v1.i);
    // Serialized output should still contain the v.2 fields
    byte[] v1Bytes = v1Adapter.writeBytes(v1);

    // Unknown fields don't participate in equals() and hashCode()
    VersionOne v1Simple = new VersionOne.Builder().i(111).build();
    assertEquals(v1Simple, v1);
    assertEquals(v1Simple.hashCode(), v1.hashCode());
    assertNotSame(v1Adapter.writeBytes(v1Simple), v1Adapter.writeBytes(v1));

    // Re-parse
    VersionTwo v2B = v2Adapter.readBytes(v1Bytes);
    assertEquals(new Integer(111), v2B.i);
    assertEquals(new Integer(12345), v2B.v2_i);
    assertEquals("222", v2B.v2_s);
    assertEquals(new Integer(67890), v2B.v2_f32);
    assertEquals(new Long(98765L), v2B.v2_f64);
    assertEquals(Arrays.asList("1", "2"), v2B.v2_rs);

    // "Modify" v1 via a merged builder, serialize, and re-parse
    VersionOne v1Modified = new VersionOne.Builder(v1).i(777).build();
    assertEquals(new Integer(777), v1Modified.i);
    byte[] v1ModifiedBytes = v1Adapter.writeBytes(v1Modified);

    VersionTwo v2C = v2Adapter.readBytes(v1ModifiedBytes);
    assertEquals(new Integer(777), v2C.i);
    assertEquals(new Integer(12345), v2C.v2_i);
    assertEquals("222", v2C.v2_s);
    assertEquals(new Integer(67890), v2C.v2_f32);
    assertEquals(new Long(98765L), v2C.v2_f64);
    assertEquals(Arrays.asList("1", "2"), v2C.v2_rs);
  }
}
