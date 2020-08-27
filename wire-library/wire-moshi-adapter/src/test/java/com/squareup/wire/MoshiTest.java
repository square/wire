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
import com.squareup.wire.proto2.RepeatedPackedAndMap;
import com.squareup.wire.proto2.alltypes.AllTypes;
import java.io.File;
import java.io.IOException;
import okio.ByteString;
import okio.Okio;
import org.junit.Test;
import squareup.proto2.keywords.KeywordJava;
import squareup.proto2.keywords.KeywordKotlin;

import static org.assertj.core.api.Assertions.assertThat;

public class MoshiTest {
  private static final String ALL_TYPES_IDENTITY_JSON;

  static {
    try {
      ALL_TYPES_IDENTITY_JSON =
          Okio.buffer(
              Okio.source(
                  new File("../wire-tests/src/commonTest/shared/json",
                      "all_types_identity_proto2.json"))
          ).readUtf8();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  private final Moshi moshi = new Moshi.Builder()
      .add(new WireJsonAdapterFactory())
      .build();

  @SuppressWarnings("ConstantConditions")
  @Test public void notIdentityOneOf() throws IOException {
    JsonAdapter<AllTypes> allTypesAdapter = moshi.adapter(AllTypes.class);

    AllTypes allTypes = createIdentityBuilder().oneof_int32(0).build();
    assertThat(allTypesAdapter.toJson(allTypes)).contains("\"oneof_int32\":0");

    AllTypes parsed = allTypesAdapter.fromJson(ALL_TYPES_IDENTITY_JSON);
    assertThat(parsed.oneof_int32).isNull();
    String json = ALL_TYPES_IDENTITY_JSON
        .substring(0, ALL_TYPES_IDENTITY_JSON.length() - 2) + ",\"oneof_int32\":0}";
    parsed = allTypesAdapter.fromJson(json);
    assertThat(parsed.oneof_int32).isEqualTo(0);
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
    JsonUtils.assertJsonEquals(json,
        "{\"object\":\"object\",\"when\":1, \"fun\":{}, \"return\":[]}");
    assertThat(adapter.fromJson(json)).isEqualTo(keyword);

    String generatedNamedJson = "{\"object_\":\"object\",\"when_\":1, \"fun_\":{}, \"return_\":[]}";
    assertThat(adapter.fromJson(generatedNamedJson)).isEqualTo(keyword);
  }

  @Test public void usedKeywordsInJava() throws IOException {
    JsonAdapter<KeywordJava> adapter = moshi.adapter(KeywordJava.class);

    KeywordJava keyword = new KeywordJava.Builder().public_(true).final_("final").build();
    String json = adapter.toJson(keyword);
    JsonUtils.assertJsonEquals(json,
        "{\"final\":\"final\", \"public\":true, \"package\":{}, \"return\":[]}");
    assertThat(adapter.fromJson(json)).isEqualTo(keyword);

    String generatedNamedJson = "{\"final_\":\"final\", \"public_\":true, \"package_\":{}, "
        + "\"return_\":[]}";
    assertThat(adapter.fromJson(generatedNamedJson)).isEqualTo(keyword);
  }
}
