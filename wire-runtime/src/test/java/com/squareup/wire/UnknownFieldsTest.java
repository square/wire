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

import static org.assertj.core.api.Assertions.assertThat;

public class UnknownFieldsTest {

  private final ProtoAdapter<VersionOne> v1Adapter = VersionOne.ADAPTER;
  private final ProtoAdapter<VersionTwo> v2Adapter = VersionTwo.ADAPTER;

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
    assertThat(v2.i).isEqualTo(new Integer(111));
    // Check v.2 fields
    assertThat(v2.v2_i).isEqualTo(new Integer(12345));
    assertThat(v2.v2_s).isEqualTo("222");
    assertThat(v2.v2_f32).isEqualTo(new Integer(67890));
    assertThat(v2.v2_f64).isEqualTo(new Long(98765L));
    assertThat(v2.v2_rs).containsExactly("1", "2");
    // Serialized
    byte[] v2Bytes = v2Adapter.encode(v2);

    // Parse
    VersionOne v1 = v1Adapter.decode(v2Bytes);
    // v.1 fields are visible, v.2 fields are in unknownFieldSet
    assertThat(v1.i).isEqualTo(new Integer(111));
    // Serialized output should still contain the v.2 fields
    byte[] v1Bytes = v1Adapter.encode(v1);

    // Unknown fields participate in equals() and hashCode()
    VersionOne v1Simple = new VersionOne.Builder().i(111).build();
    assertThat(v1).isNotEqualTo(v1Simple);
    assertThat(v1.hashCode()).isNotEqualTo(v1Simple.hashCode());
    assertThat(v1Adapter.encode(v1)).isNotEqualTo(v1Adapter.encode(v1Simple));

    // Unknown fields can be removed for equals() and hashCode();
    VersionOne v1Known = v1.withoutUnknownFields();
    assertThat(v1Known).isEqualTo(v1Simple);
    assertThat(v1Known.hashCode()).isEqualTo(v1Simple.hashCode());
    assertThat(v1Adapter.encode(v1Known)).isEqualTo(v1Adapter.encode(v1Simple));

    // Re-parse
    VersionTwo v2B = v2Adapter.decode(v1Bytes);
    assertThat(v2B.i).isEqualTo(new Integer(111));
    assertThat(v2B.v2_i).isEqualTo(new Integer(12345));
    assertThat(v2B.v2_s).isEqualTo("222");
    assertThat(v2B.v2_f32).isEqualTo(new Integer(67890));
    assertThat(v2B.v2_f64).isEqualTo(new Long(98765L));
    assertThat(v2B.v2_rs).containsExactly("1", "2");

    // "Modify" v1 via a merged builder, serialize, and re-parse
    VersionOne v1Modified = v1.newBuilder().i(777).build();
    assertThat(v1Modified.i).isEqualTo(new Integer(777));
    byte[] v1ModifiedBytes = v1Adapter.encode(v1Modified);

    VersionTwo v2C = v2Adapter.decode(v1ModifiedBytes);
    assertThat(v2C.i).isEqualTo(new Integer(777));
    assertThat(v2C.v2_i).isEqualTo(new Integer(12345));
    assertThat(v2C.v2_s).isEqualTo("222");
    assertThat(v2C.v2_f32).isEqualTo(new Integer(67890));
    assertThat(v2C.v2_f64).isEqualTo(new Long(98765L));
    assertThat(v2C.v2_rs).containsExactly("1", "2");
  }
}
