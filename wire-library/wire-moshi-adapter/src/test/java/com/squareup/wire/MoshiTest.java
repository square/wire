/*
 * Copyright 2018 Square Inc.
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

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.wire.protos.RepeatedPackedAndMap;
import com.squareup.wire.protos.alltypes.AllTypes;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import okio.ByteString;
import org.junit.Test;

import static com.squareup.wire.json.JsonKt.assertJsonEquals;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

public class MoshiTest {
  private static final String JSON = "{\"opt_int32\":111,"
      + "\"opt_uint32\":112,"
      + "\"opt_sint32\":113,"
      + "\"opt_fixed32\":114,"
      + "\"opt_sfixed32\":115,"
      + "\"opt_int64\":116,"
      + "\"opt_uint64\":\"117\","
      + "\"opt_sint64\":118,"
      + "\"opt_fixed64\":119,"
      + "\"opt_sfixed64\":120,"
      + "\"opt_bool\":true,"
      + "\"opt_float\":122.0,"
      + "\"opt_double\":123.0,"
      + "\"opt_string\":\"124\","
      + "\"opt_bytes\":\"e30=\","
      + "\"opt_nested_enum\":\"A\","
      + "\"opt_nested_message\":{\"a\":999},"
      + "\"req_int32\":111,"
      + "\"req_uint32\":112,"
      + "\"req_sint32\":113,"
      + "\"req_fixed32\":114,"
      + "\"req_sfixed32\":115,"
      + "\"req_int64\":116,"
      + "\"req_uint64\":\"117\","
      + "\"req_sint64\":118,"
      + "\"req_fixed64\":119,"
      + "\"req_sfixed64\":120,"
      + "\"req_bool\":true,"
      + "\"req_float\":122.0,"
      + "\"req_double\":123.0,"
      + "\"req_string\":\"124\","
      + "\"req_bytes\":\"e30=\","
      + "\"req_nested_enum\":\"A\","
      + "\"req_nested_message\":{\"a\":999},"
      + "\"rep_int32\":[111,111],"
      + "\"rep_uint32\":[112,112],"
      + "\"rep_sint32\":[113,113],"
      + "\"rep_fixed32\":[114,114],"
      + "\"rep_sfixed32\":[115,115],"
      + "\"rep_int64\":[116,116],"
      + "\"rep_uint64\":[\"117\",\"117\"],"
      + "\"rep_sint64\":[118,118],"
      + "\"rep_fixed64\":[119,119],"
      + "\"rep_sfixed64\":[120,120],"
      + "\"rep_bool\":[true,true],"
      + "\"rep_float\":[122.0,122.0],"
      + "\"rep_double\":[123.0,123.0],"
      + "\"rep_string\":[\"124\",\"124\"],"
      + "\"rep_bytes\":[\"e30=\",\"e30=\"],"
      + "\"rep_nested_enum\":[\"A\",\"A\"],"
      + "\"rep_nested_message\":[{\"a\":999},{\"a\":999}],"
      + "\"pack_int32\":[111,111],"
      + "\"pack_uint32\":[112,112],"
      + "\"pack_sint32\":[113,113],"
      + "\"pack_fixed32\":[114,114],"
      + "\"pack_sfixed32\":[115,115],"
      + "\"pack_int64\":[116,116],"
      + "\"pack_uint64\":[\"117\",\"117\"],"
      + "\"pack_sint64\":[118,118],"
      + "\"pack_fixed64\":[119,119],"
      + "\"pack_sfixed64\":[120,120],"
      + "\"pack_bool\":[true,true],"
      + "\"pack_float\":[122.0,122.0],"
      + "\"pack_double\":[123.0,123.0],"
      + "\"pack_nested_enum\":[\"A\",\"A\"],"
      + "\"map_int32_int32\":{\"1\":2},"
      + "\"map_string_string\":{\"key\":\"value\"},"
      + "\"map_string_message\":{\"message\":{\"a\":1}},"
      + "\"map_string_enum\":{\"enum\":\"A\"},"
      + "\"ext_opt_int32\":2147483647,"
      + "\"ext_opt_int64\":-4611686018427387726,"
      + "\"ext_opt_uint64\":\"13835058055282163890\","
      + "\"ext_opt_sint64\":-4611686018427387726,"
      + "\"ext_opt_bool\":true,"
      + "\"ext_opt_float\":1234500.0,"
      + "\"ext_opt_double\":1.2345E67,"
      + "\"ext_opt_nested_enum\":\"A\","
      + "\"ext_opt_nested_message\":{\"a\":999},"
      + "\"ext_rep_int32\":[2147483647,2147483647],"
      + "\"ext_rep_uint32\":[],"
      + "\"ext_rep_sint32\":[],"
      + "\"ext_rep_fixed32\":[],"
      + "\"ext_rep_sfixed32\":[],"
      + "\"ext_rep_int64\":[],"
      + "\"ext_rep_uint64\":[\"13835058055282163890\",\"13835058055282163890\"],"
      + "\"ext_rep_sint64\":[-4611686018427387726,-4611686018427387726],"
      + "\"ext_rep_fixed64\":[],"
      + "\"ext_rep_sfixed64\":[],"
      + "\"ext_rep_bool\":[true,true],"
      + "\"ext_rep_float\":[1234500.0,1234500.0],"
      + "\"ext_rep_double\":[1.2345E67,1.2345E67],"
      + "\"ext_rep_string\":[],"
      + "\"ext_rep_bytes\":[],"
      + "\"ext_rep_nested_enum\":[\"A\",\"A\"],"
      + "\"ext_rep_nested_message\":[{\"a\":999},{\"a\":999}],"
      + "\"ext_pack_int32\":[2147483647,2147483647],"
      + "\"ext_pack_uint32\":[],"
      + "\"ext_pack_sint32\":[],"
      + "\"ext_pack_fixed32\":[],"
      + "\"ext_pack_sfixed32\":[],"
      + "\"ext_pack_int64\":[],"
      + "\"ext_pack_uint64\":[\"13835058055282163890\",\"13835058055282163890\"],"
      + "\"ext_pack_sint64\":[-4611686018427387726,-4611686018427387726],"
      + "\"ext_pack_fixed64\":[],"
      + "\"ext_pack_sfixed64\":[],"
      + "\"ext_pack_bool\":[true,true],"
      + "\"ext_pack_float\":[1234500.0,1234500.0],"
      + "\"ext_pack_double\":[1.2345E67,1.2345E67],"
      + "\"ext_pack_nested_enum\":[\"A\",\"A\"],"
      + "\"ext_map_int32_int32\":{\"1\":2},"
      + "\"ext_map_string_string\":{\"key\":\"value\"},"
      + "\"ext_map_string_message\":{\"message\":{\"a\":1}},"
      + "\"ext_map_string_enum\":{\"enum\":\"A\"}}";

  // Return a two-element list with a given repeated value
  @SuppressWarnings("unchecked")
  private static <T> List<T> list(T x) {
    return Arrays.asList(x, x);
  }

  private static AllTypes.Builder createBuilder() {
    ByteString bytes = ByteString.of((byte) 123, (byte) 125);
    AllTypes.NestedMessage nestedMessage = new AllTypes.NestedMessage.Builder().a(999).build();
    return new AllTypes.Builder()
        .opt_int32(111)
        .opt_uint32(112)
        .opt_sint32(113)
        .opt_fixed32(114)
        .opt_sfixed32(115)
        .opt_int64(116L)
        .opt_uint64(117L)
        .opt_sint64(118L)
        .opt_fixed64(119L)
        .opt_sfixed64(120L)
        .opt_bool(true)
        .opt_float(122.0F)
        .opt_double(123.0)
        .opt_string("124")
        .opt_bytes(bytes)
        .opt_nested_enum(AllTypes.NestedEnum.A)
        .opt_nested_message(nestedMessage)
        .req_int32(111)
        .req_uint32(112)
        .req_sint32(113)
        .req_fixed32(114)
        .req_sfixed32(115)
        .req_int64(116L)
        .req_uint64(117L)
        .req_sint64(118L)
        .req_fixed64(119L)
        .req_sfixed64(120L)
        .req_bool(true)
        .req_float(122.0F)
        .req_double(123.0)
        .req_string("124")
        .req_bytes(bytes)
        .req_nested_enum(AllTypes.NestedEnum.A)
        .req_nested_message(nestedMessage)
        .rep_int32(list(111))
        .rep_uint32(list(112))
        .rep_sint32(list(113))
        .rep_fixed32(list(114))
        .rep_sfixed32(list(115))
        .rep_int64(list(116L))
        .rep_uint64(list(117L))
        .rep_sint64(list(118L))
        .rep_fixed64(list(119L))
        .rep_sfixed64(list(120L))
        .rep_bool(list(true))
        .rep_float(list(122.0F))
        .rep_double(list(123.0))
        .rep_string(list("124"))
        .rep_bytes(list(bytes))
        .rep_nested_enum(list(AllTypes.NestedEnum.A))
        .rep_nested_message(list(nestedMessage))
        .pack_int32(list(111))
        .pack_uint32(list(112))
        .pack_sint32(list(113))
        .pack_fixed32(list(114))
        .pack_sfixed32(list(115))
        .pack_int64(list(116L))
        .pack_uint64(list(117L))
        .pack_sint64(list(118L))
        .pack_fixed64(list(119L))
        .pack_sfixed64(list(120L))
        .pack_bool(list(true))
        .pack_float(list(122.0F))
        .pack_double(list(123.0))
        .pack_nested_enum(list(AllTypes.NestedEnum.A))
        .map_int32_int32(singletonMap(1, 2))
        .map_string_string(singletonMap("key", "value"))
        .map_string_message(singletonMap("message", new AllTypes.NestedMessage(1)))
        .map_string_enum(singletonMap("enum", AllTypes.NestedEnum.A))
        .ext_opt_int32(Integer.MAX_VALUE)
        .ext_opt_int64(Long.MIN_VALUE / 2 + 178)
        .ext_opt_uint64(Long.MIN_VALUE / 2 + 178)
        .ext_opt_sint64(Long.MIN_VALUE / 2 + 178)
        .ext_opt_bool(true)
        .ext_opt_float(1.2345e6F)
        .ext_opt_double(1.2345e67)
        .ext_opt_nested_enum(AllTypes.NestedEnum.A)
        .ext_opt_nested_message(nestedMessage)
        .ext_rep_int32(list(Integer.MAX_VALUE))
        .ext_rep_uint64(list(Long.MIN_VALUE / 2 + 178))
        .ext_rep_sint64(list(Long.MIN_VALUE / 2 + 178))
        .ext_rep_bool(list(true))
        .ext_rep_float(list(1.2345e6F))
        .ext_rep_double(list(1.2345e67))
        .ext_rep_nested_enum(list(AllTypes.NestedEnum.A))
        .ext_rep_nested_message(list(nestedMessage))
        .ext_pack_int32(list(Integer.MAX_VALUE))
        .ext_pack_uint64(list(Long.MIN_VALUE / 2 + 178))
        .ext_pack_sint64(list(Long.MIN_VALUE / 2 + 178))
        .ext_pack_bool(list(true))
        .ext_pack_float(list(1.2345e6F))
        .ext_pack_double(list(1.2345e67))
        .ext_pack_nested_enum(list(AllTypes.NestedEnum.A))
        .ext_map_int32_int32(singletonMap(1, 2))
        .ext_map_string_string(singletonMap("key", "value"))
        .ext_map_string_message(singletonMap("message", new AllTypes.NestedMessage(1)))
        .ext_map_string_enum(singletonMap("enum", AllTypes.NestedEnum.A));
  }

  private final Moshi moshi = new Moshi.Builder()
      .add(new WireJsonAdapterFactory())
      .build();

  @Test public void serializationOfAllTypes() {
    AllTypes allTypes = createBuilder().build();
    assertJsonEquals(moshi.adapter(AllTypes.class).toJson(allTypes), JSON);
  }

  @SuppressWarnings("ConstantConditions")
  @Test public void deserializationOfAllTypes() throws IOException {
    JsonAdapter<AllTypes> allTypesAdapter = moshi.adapter(AllTypes.class);

    AllTypes allTypes = createBuilder().build();
    AllTypes parsed = allTypesAdapter.fromJson(JSON);
    assertThat(parsed).isEqualTo(allTypes);
    assertThat(parsed.toString()).isEqualTo(allTypes.toString());
    assertJsonEquals(allTypesAdapter.toJson(parsed), allTypesAdapter.toJson(allTypes));
  }

  @Test public void omitsUnknownFields() {
    AllTypes.Builder builder = createBuilder();
    builder.addUnknownField(9000, FieldEncoding.FIXED32, 9000);
    builder.addUnknownField(9001, FieldEncoding.FIXED64, 9001L);
    builder.addUnknownField(9002, FieldEncoding.LENGTH_DELIMITED,
        ByteString.of((byte) '9', (byte) '0', (byte) '0', (byte) '2'));
    builder.addUnknownField(9003, FieldEncoding.VARINT, 9003L);

    AllTypes allTypes = builder.build();
    assertJsonEquals(moshi.adapter(AllTypes.class).toJson(allTypes), JSON);
  }

  @SuppressWarnings("ConstantConditions")
  @Test public void nullRepeatedField() throws IOException {
    RepeatedPackedAndMap parsed = moshi.adapter(RepeatedPackedAndMap.class)
        .fromJson("{\"rep_int32\":null,\"pack_int32\":null,\"map_int32_int32\":null}");
    assertThat(parsed.rep_int32).isEmpty();
    assertThat(parsed.pack_int32).isEmpty();
    assertThat(parsed.map_int32_int32).isEmpty();
  }
}
