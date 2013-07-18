// Copyright 2013 Square, Inc.
package com.squareup.wire;

import com.squareup.wire.protos.unknownfields.VersionOne;
import com.squareup.wire.protos.unknownfields.VersionTwo;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

@Ignore
public class UnknownFieldsTest {

  private final Wire wire = new Wire();

  @Test
  public void testUnknownFields() throws IOException {
    VersionTwo v2 = new VersionTwo.Builder()
       .i(111)
       .v2_i(12345)
       .v2_s("222")
       .v2_f32(67890)
       .v2_f64(98765L)
       .build();
    assertEquals(new Integer(111), v2.i);
    // Check v.2 fields
    assertEquals(new Integer(12345), v2.v2_i);
    assertEquals("222", v2.v2_s);
    assertEquals(new Integer(67890), v2.v2_f32);
    assertEquals(new Long(98765L), v2.v2_f64);
    // Serialized
    byte[] v2Bytes = wire.toByteArray(v2);

    // Parse
    VersionOne v1 = wire.parseFrom(VersionOne.class, v2Bytes);
    // v.1 fields are visible, v.2 fields are in unknownFieldSet
    assertEquals(new Integer(111), v1.i);
    // Serialized output should still contain the v.2 fields
    byte[] v1Bytes = wire.toByteArray(v1);

    // Unknown fields don't participate in equals() and hashCode()
    VersionOne v1Simple = new VersionOne.Builder().i(111).build();
    assertEquals(v1Simple, v1);
    assertEquals(v1Simple.hashCode(), v1.hashCode());
    assertNotSame(wire.toByteArray(v1Simple), wire.toByteArray(v1));

    // Re-parse
    VersionTwo v2B = wire.parseFrom(VersionTwo.class, v1Bytes);
    assertEquals(new Integer(111), v2B.i);
    assertEquals(new Integer(12345), v2B.v2_i);
    assertEquals("222", v2B.v2_s);
    assertEquals(new Integer(67890), v2B.v2_f32);
    assertEquals(new Long(98765L), v2B.v2_f64);

    // "Modify" v1 via a merged builder, serialize, and re-parse
    VersionOne v1Modified = new VersionOne.Builder(v1).i(777).build();
    assertEquals(new Integer(777), v1Modified.i);
    byte[] v1ModifiedBytes = wire.toByteArray(v1Modified);

    VersionTwo v2C = wire.parseFrom(VersionTwo.class, v1ModifiedBytes);
    assertEquals(new Integer(777), v2C.i);
    assertEquals(new Integer(12345), v2C.v2_i);
    assertEquals("222", v2C.v2_s);
    assertEquals(new Integer(67890), v2C.v2_f32);
    assertEquals(new Long(98765L), v2C.v2_f64);
  }
}
