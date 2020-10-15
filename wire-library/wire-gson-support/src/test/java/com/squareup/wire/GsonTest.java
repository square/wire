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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.moshi.JsonAdapter;
import com.squareup.wire.json.JsonUtils;
import com.squareup.wire.proto2.RepeatedPackedAndMap;
import com.squareup.wire.proto2.alltypes.AllTypes;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import okio.Okio;
import org.junit.Ignore;
import org.junit.Test;
import squareup.proto2.keywords.KeywordJava;
import squareup.proto2.keywords.KeywordJava.KeywordJavaEnum;
import squareup.proto2.keywords.KeywordKotlin;
import squareup.proto2.keywords.KeywordKotlin.KeywordKotlinEnum;

import static org.assertj.core.api.Assertions.assertThat;

public class GsonTest {
  private static final String ALL_TYPES_JSON;

  static {
    try {
      ALL_TYPES_JSON =
          Okio.buffer(
              Okio.source(
                  new File("../wire-tests/src/commonTest/shared/json", "all_types_proto2.json"))
          ).readUtf8();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private final Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(new WireTypeAdapterFactory())
      .disableHtmlEscaping()
      .create();

  @Test public void deserializationOfNullValues() {
    // Modifying JSON string manually, cause serialization omits fields with null values.
    String json = ALL_TYPES_JSON.replace("\"opt_string\": \"124\",", "\"opt_string\": null,");
    AllTypes deserializedAllTypesWithNulls = gson.fromJson(json, AllTypes.class);
    assertThat(deserializedAllTypesWithNulls.opt_string).isNull();
  }

  @Test public void nullRepeatedField() {
    RepeatedPackedAndMap parsed =
        gson.fromJson("{rep_int32=null,pack_int32=null,map_int32_int32=null}",
            RepeatedPackedAndMap.class);
    assertThat(parsed.rep_int32).isEmpty();
    assertThat(parsed.pack_int32).isEmpty();
    assertThat(parsed.map_int32_int32).isEmpty();
  }

  @Test public void usedKeywordsInKotlin() {
    KeywordKotlin keyword = new KeywordKotlin.Builder()
        .object_("object")
        .when_(1)
        .enums(
            Arrays.asList(
                KeywordKotlinEnum.object_,
                KeywordKotlinEnum.when_,
                KeywordKotlinEnum.fun_,
                KeywordKotlinEnum.return_,
                KeywordKotlinEnum.open_
            )
        )
        .build();
    String json = gson.toJson(keyword);
    JsonUtils.assertJsonEquals(
        "{\"object\":\"object\",\"when\":1, \"fun\":{}, \"return\":[], \"enums\":[\"object\", "
            + "\"when\", \"fun\", \"return\", \"open\"]}",
        json);
    KeywordKotlin parseKeyword = gson.fromJson(json, KeywordKotlin.class);
    assertThat(parseKeyword).isEqualTo(keyword);

    String generatedNamedJson = "{\"object_\":\"object\",\"when_\":1, \"fun_\":{}, \"return_\":[], "
        + "\"enums\":[\"object_\", \"when_\", \"fun_\", \"return_\", \"open_\"]}";
    assertThat(gson.fromJson(generatedNamedJson, KeywordKotlin.class)).isEqualTo(keyword);
  }

  @Test public void usedKeywordsInJava() {
    KeywordJava keyword = new KeywordJava.Builder()
        .final_("final")
        .public_(true)
        .enums(
            Arrays.asList(
                KeywordJavaEnum.final_,
                KeywordJavaEnum.public_,
                KeywordJavaEnum.package_,
                KeywordJavaEnum.return_
            )
        )
        .build();
    String json = gson.toJson(keyword);
    JsonUtils.assertJsonEquals(
        "{\"final\":\"final\", \"public\":true, \"package\":{}, \"return\":[], "
            + "\"enums\":[\"final\", \"public\", \"package\", \"return\"]}",
        json);
    KeywordJava parseKeyword = gson.fromJson(json, KeywordJava.class);
    assertThat(parseKeyword).isEqualTo(keyword);

    String generatedNamedJson = "{\"final_\":\"final\", \"public_\":true, \"package_\":{}, "
        + "\"return_\":[], \"enums\":[\"final_\", \"public_\", \"package_\", \"return_\"]}";
    assertThat(gson.fromJson(generatedNamedJson, KeywordJava.class)).isEqualTo(keyword);
  }

  @Test public void enumKeywordsAtRootInKotlin() {
    KeywordKotlinEnum constant = KeywordKotlinEnum.object_;
    String json = gson.toJson(constant);
    JsonUtils.assertJsonEquals("\"object\"", json);
    KeywordKotlinEnum parseKeyword = gson.fromJson(json, KeywordKotlinEnum.class);
    assertThat(parseKeyword).isEqualTo(constant);

    String generatedNamedJson = "\"object_\"";
    assertThat(gson.fromJson(generatedNamedJson, KeywordKotlinEnum.class)).isEqualTo(constant);
  }

  @Test public void enumKeywordsAtRootInJava() {
    KeywordJavaEnum constant = KeywordJavaEnum.final_;
    String json = gson.toJson(constant);
    JsonUtils.assertJsonEquals("\"final\"", json);
    KeywordJavaEnum parseKeyword = gson.fromJson(json, KeywordJavaEnum.class);
    assertThat(parseKeyword).isEqualTo(constant);

    String generatedNamedJson = "\"final_\"";
    assertThat(gson.fromJson(generatedNamedJson, KeywordJavaEnum.class)).isEqualTo(constant);
  }
}
