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
import com.squareup.wire.json.JsonUtils;
import com.squareup.wire.protos.RepeatedPackedAndMap;
import com.squareup.wire.protos.alltypes.AllTypes;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import okio.ByteString;
import org.junit.Test;
import squareup.keywords.KeywordJava;
import squareup.keywords.KeywordKotlin;

import static com.squareup.wire.json.JsonUtils.assertJsonEquals;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

public class MoshiTest {
  private static final String JSON = "{\"opt_int32\":111,"
      + "\"opt_uint32\":112,"
      + "\"opt_sint32\":113,"
      + "\"opt_fixed32\":114,"
      + "\"opt_sfixed32\":115,"
      + "\"opt_int64\":116,"
      + "\"opt_uint64\":117,"
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
      + "\"req_uint64\":117,"
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
      + "\"rep_uint64\":[117,117],"
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
      + "\"pack_uint64\":[117,117],"
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
      + "\"oneof_int32\":4444,"
      + "\"ext_opt_int32\":2147483647,"
      + "\"ext_opt_int64\":-4611686018427387726,"
      + "\"ext_opt_uint64\":13835058055282163890,"
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
      + "\"ext_rep_uint64\":[13835058055282163890,13835058055282163890],"
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
      + "\"ext_pack_uint64\":[13835058055282163890,13835058055282163890],"
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

  // Note: In proto2, all values, including what is considered identity values in proto3 should be
  // emitted.
  private static final String JSON_IDENTITY = "{\n"
      + "\"opt_int32\":0,\n"
      + "\"opt_uint32\":0,\n"
      + "\"opt_sint32\":0,\n"
      + "\"opt_fixed32\":0,\n"
      + "\"opt_sfixed32\":0,\n"
      + "\"opt_int64\":0,\n"
      + "\"opt_uint64\":0,\n"
      + "\"opt_sint64\":0,\n"
      + "\"opt_fixed64\":0,\n"
      + "\"opt_sfixed64\":0,\n"
      + "\"opt_bool\":false,\n"
      + "\"opt_float\":0.0,\n"
      + "\"opt_double\":0.0,\n"
      + "\"opt_string\":\"\",\n"
      + "\"opt_bytes\":\"\",\n"
      + "\"opt_nested_enum\":\"A\",\n"
      + "\"req_int32\":0,\n"
      + "\"req_uint32\":0,\n"
      + "\"req_sint32\":0,\n"
      + "\"req_fixed32\":0,\n"
      + "\"req_sfixed32\":0,\n"
      + "\"req_int64\":0,\n"
      + "\"req_uint64\":0,\n"
      + "\"req_sint64\":0,\n"
      + "\"req_fixed64\":0,\n"
      + "\"req_sfixed64\":0,\n"
      + "\"req_bool\":true,\n"
      + "\"req_float\":0.0,\n"
      + "\"req_double\":0.0,\n"
      + "\"req_string\":\"\",\n"
      + "\"req_bytes\":\"\",\n"
      + "\"req_nested_enum\":\"A\",\n"
      + "\"req_nested_message\":{\"a\":0},\n"
      + "\"rep_int32\":[],\n"
      + "\"rep_uint32\":[],\n"
      + "\"rep_sint32\":[],\n"
      + "\"rep_fixed32\":[],\n"
      + "\"rep_sfixed32\":[],\n"
      + "\"rep_int64\":[],\n"
      + "\"rep_uint64\":[],\n"
      + "\"rep_sint64\":[],\n"
      + "\"rep_fixed64\":[],\n"
      + "\"rep_sfixed64\":[],\n"
      + "\"rep_bool\":[],\n"
      + "\"rep_float\":[],\n"
      + "\"rep_double\":[],\n"
      + "\"rep_string\":[],\n"
      + "\"rep_bytes\":[],\n"
      + "\"rep_nested_enum\":[],\n"
      + "\"rep_nested_message\":[],\n"
      + "\"pack_int32\":[],\n"
      + "\"pack_uint32\":[],\n"
      + "\"pack_sint32\":[],\n"
      + "\"pack_fixed32\":[],\n"
      + "\"pack_sfixed32\":[],\n"
      + "\"pack_int64\":[],\n"
      + "\"pack_uint64\":[],\n"
      + "\"pack_sint64\":[],\n"
      + "\"pack_fixed64\":[],\n"
      + "\"pack_sfixed64\":[],\n"
      + "\"pack_bool\":[],\n"
      + "\"pack_float\":[],\n"
      + "\"pack_double\":[],\n"
      + "\"pack_nested_enum\":[],\n"
      + "\"map_int32_int32\":{},\n"
      + "\"map_string_string\":{},\n"
      + "\"map_string_message\":{},\n"
      + "\"map_string_enum\":{},\n"
      + "\"ext_rep_int32\":[],\n"
      + "\"ext_rep_uint32\":[],\n"
      + "\"ext_rep_sint32\":[],\n"
      + "\"ext_rep_fixed32\":[],\n"
      + "\"ext_rep_sfixed32\":[],\n"
      + "\"ext_rep_int64\":[],\n"
      + "\"ext_rep_uint64\":[],\n"
      + "\"ext_rep_sint64\":[],\n"
      + "\"ext_rep_fixed64\":[],\n"
      + "\"ext_rep_sfixed64\":[],\n"
      + "\"ext_rep_bool\":[],\n"
      + "\"ext_rep_float\":[],\n"
      + "\"ext_rep_double\":[],\n"
      + "\"ext_rep_string\":[],\n"
      + "\"ext_rep_bytes\":[],\n"
      + "\"ext_rep_nested_enum\":[],\n"
      + "\"ext_rep_nested_message\":[],\n"
      + "\"ext_pack_int32\":[],\n"
      + "\"ext_pack_uint32\":[],\n"
      + "\"ext_pack_sint32\":[],\n"
      + "\"ext_pack_fixed32\":[],\n"
      + "\"ext_pack_sfixed32\":[],\n"
      + "\"ext_pack_int64\":[],\n"
      + "\"ext_pack_uint64\":[],\n"
      + "\"ext_pack_sint64\":[],\n"
      + "\"ext_pack_fixed64\":[],\n"
      + "\"ext_pack_sfixed64\":[],\n"
      + "\"ext_pack_bool\":[],\n"
      + "\"ext_pack_float\":[],\n"
      + "\"ext_pack_double\":[],\n"
      + "\"ext_pack_nested_enum\":[],\n"
      + "\"ext_map_int32_int32\":{},\n"
      + "\"ext_map_string_string\":{},\n"
      + "\"ext_map_string_message\":{},\n"
      + "\"ext_map_string_enum\":{}}";

  // Return a two-element list with a given repeated value
  @SuppressWarnings("unchecked")
  private static <T> List<T> list(T x) {
    return Arrays.asList(x, x);
  }

  private static AllTypes.Builder createIdentityBuilder() {
    return new AllTypes.Builder()
        .opt_int32(0)
        .opt_uint32(0)
        .opt_sint32(0)
        .opt_fixed32(0)
        .opt_sfixed32(0)
        .opt_int64(0L)
        .opt_uint64(0L)
        .opt_sint64(0L)
        .opt_fixed64(0L)
        .opt_sfixed64(0L)
        .opt_bool(false)
        .opt_float(0F)
        .opt_double(0.0)
        .opt_string("")
        .opt_bytes(ByteString.EMPTY)
        .opt_nested_enum(AllTypes.NestedEnum.A)
        .opt_nested_message(null)
        .req_int32(0)
        .req_uint32(0)
        .req_sint32(0)
        .req_fixed32(0)
        .req_sfixed32(0)
        .req_int64(0L)
        .req_uint64(0L)
        .req_sint64(0L)
        .req_fixed64(0L)
        .req_sfixed64(0L)
        .req_bool(true)
        .req_float(0F)
        .req_double(0.0)
        .req_string("")
        .req_bytes(ByteString.EMPTY)
        .req_nested_enum(AllTypes.NestedEnum.A)
        .req_nested_message(new AllTypes.NestedMessage.Builder().a(0).build());
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
        .oneof_int32(4444)
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

  @Test public void serializationOfIdentityAllTypes() {
    AllTypes allTypes = createIdentityBuilder().build();
    assertJsonEquals(moshi.adapter(AllTypes.class).toJson(allTypes), JSON_IDENTITY);
  }

  @SuppressWarnings("ConstantConditions")
  @Test public void deserializationOfIdentityAllTypes() throws IOException {
    JsonAdapter<AllTypes> allTypesAdapter = moshi.adapter(AllTypes.class);

    AllTypes allTypes = createIdentityBuilder().build();
    AllTypes parsed = allTypesAdapter.fromJson(JSON_IDENTITY);
    assertThat(parsed).isEqualTo(allTypes);
    assertThat(parsed.toString()).isEqualTo(allTypes.toString());
    assertJsonEquals(allTypesAdapter.toJson(parsed), allTypesAdapter.toJson(allTypes));
  }

  @SuppressWarnings("ConstantConditions")
  @Test public void notIdentityOneOf() throws IOException {
    JsonAdapter<AllTypes> allTypesAdapter = moshi.adapter(AllTypes.class);

    AllTypes allTypes = createIdentityBuilder().oneof_int32(0).build();
    assertThat(allTypesAdapter.toJson(allTypes)).contains("\"oneof_int32\":0");

    AllTypes parsed = allTypesAdapter.fromJson(JSON_IDENTITY);
    assertThat(parsed.oneof_int32).isNull();
    String json = JSON_IDENTITY.substring(0, JSON_IDENTITY.length() - 1) + ",\"oneof_int32\":0}";
    parsed = allTypesAdapter.fromJson(json);
    assertThat(parsed.oneof_int32).isEqualTo(0);
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

  @Test public void usedKeywordsInKotlin() throws IOException {
    JsonAdapter<KeywordKotlin> adapter = moshi.adapter(KeywordKotlin.class);

    KeywordKotlin keyword = new KeywordKotlin.Builder().object_("object").when_(1).build();
    String json = adapter.toJson(keyword);
    JsonUtils.assertJsonEquals(json, "{\"object\":\"object\",\"when\":1, \"fun\":{}, \"return\":[]}");
    KeywordKotlin parseKeyword = adapter.fromJson(json);
    assertThat(parseKeyword).isEqualTo(keyword);
  }

  @Test public void usedKeywordsInJava() throws IOException {
    JsonAdapter<KeywordJava> adapter = moshi.adapter(KeywordJava.class);

    KeywordJava keyword = new KeywordJava.Builder().public_(true).final_("final").build();
    String json = adapter.toJson(keyword);
    JsonUtils.assertJsonEquals(json, "{\"final\":\"final\", \"public\":true, \"package\":{}, \"return\":[]}");
    KeywordJava parseKeyword = adapter.fromJson(json);
    assertThat(parseKeyword).isEqualTo(keyword);
  }
}
