/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire;

import static org.assertj.core.api.Assertions.assertThat;

import com.squareup.wire.protos.unknownfields.EnumVersionTwo;
import com.squareup.wire.protos.unknownfields.NestedVersionOne;
import com.squareup.wire.protos.unknownfields.NestedVersionTwo;
import com.squareup.wire.protos.unknownfields.VersionOne;
import com.squareup.wire.protos.unknownfields.VersionTwo;
import java.io.IOException;
import java.util.Arrays;
import okio.ByteString;
import org.junit.Test;

public class UnknownFieldsTest {

  private final ProtoAdapter<VersionOne> v1Adapter = VersionOne.ADAPTER;
  private final ProtoAdapter<VersionTwo> v2Adapter = VersionTwo.ADAPTER;

  @Test
  public void testUnknownFields() throws IOException {
    NestedVersionOne v1_obj = new NestedVersionOne.Builder().i(111).build();
    NestedVersionTwo v2_obj =
        new NestedVersionTwo.Builder()
            .i(111)
            .v2_i(12345)
            .v2_s("222")
            .v2_f32(67890)
            .v2_f64(98765L)
            .v2_rs(Arrays.asList("1", "2"))
            .build();

    VersionTwo v2 =
        new VersionTwo.Builder()
            .i(111)
            .v2_i(12345)
            .v2_s("222")
            .v2_f32(67890)
            .v2_f64(98765L)
            .v2_rs(Arrays.asList("1", "2"))
            .obj(v2_obj)
            .build();
    assertThat(v2.i).isEqualTo(new Integer(111));
    assertThat(v2.obj).isEqualTo(v2_obj.newBuilder().build());
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
    assertThat(v1.obj.withoutUnknownFields()).isEqualTo(v1_obj);
    // Serialized output should still contain the v.2 fields
    byte[] v1Bytes = v1Adapter.encode(v1);

    // Unknown fields participate in equals() and hashCode()
    VersionOne v1Simple = new VersionOne.Builder().i(111).obj(v1_obj).build();
    assertThat(v1).isNotEqualTo(v1Simple);
    assertThat(v1.hashCode()).isNotEqualTo(v1Simple.hashCode());
    assertThat(v1Adapter.encode(v1)).isNotEqualTo(v1Adapter.encode(v1Simple));

    // Unknown fields can be removed for equals() and hashCode();
    VersionOne v1Known =
        v1.withoutUnknownFields().newBuilder().obj(v1.obj.withoutUnknownFields()).build();
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
    assertThat(v2B.obj).isEqualTo(v2_obj);

    // "Modify" v1 via a merged builder, serialize, and re-parse
    VersionOne v1Modified = v1.newBuilder().i(777).obj(v1_obj.newBuilder().i(777).build()).build();
    assertThat(v1Modified.i).isEqualTo(new Integer(777));
    assertThat(v1Modified.obj).isEqualTo(v1_obj.newBuilder().i(777).build());
    byte[] v1ModifiedBytes = v1Adapter.encode(v1Modified);

    VersionTwo v2C = v2Adapter.decode(v1ModifiedBytes);
    assertThat(v2C.i).isEqualTo(new Integer(777));
    assertThat(v2C.v2_i).isEqualTo(new Integer(12345));
    assertThat(v2C.v2_s).isEqualTo("222");
    assertThat(v2C.v2_f32).isEqualTo(new Integer(67890));
    assertThat(v2C.v2_f64).isEqualTo(new Long(98765L));
    assertThat(v2C.obj).isEqualTo(new NestedVersionTwo.Builder().i(777).build());
    assertThat(v2C.v2_rs).containsExactly("1", "2");
  }

  @Test
  public void repeatedCallsToBuildRetainUnknownFields() throws IOException {
    VersionTwo v2 =
        new VersionTwo.Builder()
            .i(111)
            .v2_i(12345)
            .v2_s("222")
            .v2_f32(67890)
            .v2_f64(98765L)
            .v2_rs(Arrays.asList("1", "2"))
            .build();

    // Serializes v2 and decodes it as a VersionOne.
    byte[] v2Bytes = v2Adapter.encode(v2);
    VersionOne.Builder v1Builder = v1Adapter.decode(v2Bytes).newBuilder();

    // Builds v1Builder. It should equal to v2.
    VersionOne v1A = v1Builder.build();
    VersionTwo fromV1A = v2Adapter.decode(v1Adapter.encode(v1A));
    assertThat(fromV1A).isEqualTo(v2);

    // Build v1Builder again. It should retain unknown fields.
    VersionOne v1B = v1Builder.build();
    VersionTwo fromV1B = v2Adapter.decode(v1Adapter.encode(v1B));
    assertThat(fromV1B).isEqualTo(v2);
  }

  @Test
  public void unknownFieldsCanBeAddedBetweenCallsToBuild() throws IOException {
    VersionTwo v2A = new VersionTwo.Builder().i(111).v2_i(12345).v2_s("222").v2_f32(67890).build();
    VersionTwo v2B = new VersionTwo.Builder().v2_f64(98765L).build();
    VersionTwo v2C = new VersionTwo.Builder().v2_rs(Arrays.asList("1", "2")).build();
    // A combination of v1A and v1B.
    VersionTwo v2AB = v2A.newBuilder().v2_f64(v2B.v2_f64).build();
    // A combination of v1A, v1B and v1C.
    VersionTwo v2All = v2AB.newBuilder().v2_rs(v2C.v2_rs).build();

    // Serializes v2A and decodes it as a VersionOne.
    byte[] v2ABytes = v2Adapter.encode(v2A);
    VersionOne v1 = v1Adapter.decode(v2ABytes);
    VersionOne.Builder v1Builder = v1.newBuilder();

    // Serializes v2B and decodes it as a VersionOne.
    byte[] v2BBytes = v2Adapter.encode(v2B);
    VersionOne v1B = v1Adapter.decode(v2BBytes);

    // Serializes v2C and decodes it as a VersionOne.
    byte[] v2CBytes = v2Adapter.encode(v2C);
    VersionOne v1C = v1Adapter.decode(v2CBytes);

    // Adds the unknown fields of v1B to v1Builder. The built message should equal to v2AB.
    VersionOne v1AB = v1Builder.addUnknownFields(v1B.unknownFields()).build();
    VersionTwo fromV1AB = v2Adapter.decode(v1Adapter.encode(v1AB));
    assertThat(fromV1AB).isEqualTo(v2AB);
    assertThat(fromV1AB.i).isEqualTo(new Integer(111));
    assertThat(fromV1AB.v2_i).isEqualTo(new Integer(12345));
    assertThat(fromV1AB.v2_s).isEqualTo("222");
    assertThat(fromV1AB.v2_f32).isEqualTo(new Integer(67890));
    assertThat(fromV1AB.v2_f64).isEqualTo(new Long(98765L));
    assertThat(fromV1AB.v2_rs).isEmpty();

    // Also Adds the unknown fields of v1C to v1Builder. The built message should equals to v2All.
    VersionOne v1All = v1Builder.addUnknownFields(v1C.unknownFields()).build();
    VersionTwo fromV1All = v2Adapter.decode(v1Adapter.encode(v1All));
    assertThat(fromV1All).isEqualTo(v2All);
    assertThat(fromV1All.i).isEqualTo(new Integer(111));
    assertThat(fromV1All.v2_i).isEqualTo(new Integer(12345));
    assertThat(fromV1All.v2_s).isEqualTo("222");
    assertThat(fromV1All.v2_f32).isEqualTo(new Integer(67890));
    assertThat(fromV1All.v2_f64).isEqualTo(new Long(98765L));
    assertThat(fromV1All.v2_rs).containsExactly("1", "2");
  }

  @Test
  public void unknownFieldsCanBeAddedAfterClearingUnknownFields() throws IOException {
    VersionTwo v2 =
        new VersionTwo.Builder()
            .i(111)
            .v2_i(12345)
            .v2_s("222")
            .v2_f32(67890)
            .v2_f64(98765L)
            .v2_rs(Arrays.asList("1", "2"))
            .build();

    // Serializes v2 and decodes it as a VersionOne.
    byte[] v2Bytes = v2Adapter.encode(v2);
    VersionOne v1 = v1Adapter.decode(v2Bytes);
    VersionOne.Builder v1Builder = v1.newBuilder();

    // Clears the unknown fields from v1Builder.
    VersionOne v1Known = v1Builder.clearUnknownFields().build();
    assertThat(v1Known.unknownFields()).isEqualTo(ByteString.EMPTY);

    // Adds unknown fields of v1 to v1Builder.
    VersionOne addedUnknown = v1Builder.addUnknownFields(v1.unknownFields()).build();
    assertThat(addedUnknown.unknownFields()).isEqualTo(v1.unknownFields());
  }

  @Test
  public void addedUnknownFieldsCanBeClearedFromBuilder() throws IOException {
    VersionTwo v2 = new VersionTwo.Builder().i(111).v2_i(12345).v2_s("222").v2_f32(67890).build();

    // Serializes v2 and decodes it as a VersionOne.
    byte[] v2Bytes = v2Adapter.encode(v2);
    VersionOne fromV2 = v1Adapter.decode(v2Bytes);

    // Adds unknown fields to an empty builder and clears them again.
    VersionOne emptyV1 =
        new VersionOne.Builder()
            .addUnknownFields(fromV2.unknownFields())
            .clearUnknownFields()
            .build();
    assertThat(emptyV1.unknownFields()).isEqualTo(ByteString.EMPTY);
  }

  @Test
  public void unknownEnumFields() throws IOException {
    VersionTwo v2 = new VersionTwo.Builder().en(EnumVersionTwo.PUSS_IN_BOOTS_V2).i(100).build();
    byte[] v2Serialized = VersionTwo.ADAPTER.encode(v2);
    VersionOne v1 = VersionOne.ADAPTER.decode(v2Serialized);
    assertThat(v1.i).isEqualTo(100);
    assertThat(v1.en).isNull();
    // 40 = 8 << 3 | 0 (tag: 8, field encoding: VARINT(0))
    // 04 = PUSS_IN_BOOTS(4)
    assertThat(v1.unknownFields()).isEqualTo(ByteString.decodeHex("4004"));
  }
}
