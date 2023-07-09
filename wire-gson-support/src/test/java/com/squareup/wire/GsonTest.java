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

import static com.squareup.wire.json.JsonUtils.assertJsonEquals;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.wire.proto2.RepeatedPackedAndMap;
import com.squareup.wire.proto2.alltypes.AllTypes;
import com.squareup.wire.proto2.kotlin.Getters;
import com.squareup.wire.proto2.person.kotlin.Person;
import com.squareup.wire.proto2.person.kotlin.Person.PhoneNumber;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import okio.ByteString;
import okio.Okio;
import org.junit.Test;
import squareup.proto2.keywords.KeywordJava;
import squareup.proto2.keywords.KeywordJava.KeywordJavaEnum;
import squareup.proto2.keywords.KeywordKotlin;
import squareup.proto2.keywords.KeywordKotlin.KeywordKotlinEnum;

public class GsonTest {
  private static final String ALL_TYPES_JSON;

  static {
    try {
      ALL_TYPES_JSON =
          Okio.buffer(
                  Okio.source(
                      new File(
                          "../wire-tests/src/commonTest/shared/json", "all_types_proto2.json")))
              .readUtf8();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private final Gson gson =
      new GsonBuilder()
          .registerTypeAdapterFactory(new WireTypeAdapterFactory())
          .disableHtmlEscaping()
          .create();

  @Test
  public void deserializationOfNullValues() {
    // Modifying JSON string manually, cause serialization omits fields with null values.
    String json = ALL_TYPES_JSON.replace("\"opt_string\": \"124\",", "\"opt_string\": null,");
    AllTypes deserializedAllTypesWithNulls = gson.fromJson(json, AllTypes.class);
    assertThat(deserializedAllTypesWithNulls.opt_string).isNull();
  }

  @Test
  public void nullRepeatedField() {
    RepeatedPackedAndMap parsed =
        gson.fromJson(
            "{rep_int32=null,pack_int32=null,map_int32_int32=null}", RepeatedPackedAndMap.class);
    assertThat(parsed.rep_int32).isEmpty();
    assertThat(parsed.pack_int32).isEmpty();
    assertThat(parsed.map_int32_int32).isEmpty();
  }

  @Test
  public void usedKeywordsInKotlin() {
    KeywordKotlin keyword =
        new KeywordKotlin.Builder()
            .object_("object")
            .when_(1)
            .enums(
                Arrays.asList(
                    KeywordKotlinEnum.object_,
                    KeywordKotlinEnum.when_,
                    KeywordKotlinEnum.fun_,
                    KeywordKotlinEnum.return_,
                    KeywordKotlinEnum.open_,
                    KeywordKotlinEnum.name_,
                    KeywordKotlinEnum.ordinal_))
            .build();
    String json = gson.toJson(keyword);
    assertJsonEquals(
        "{\"object\":\"object\",\"when\":1, \"fun\":{}, \"return\":[], \"enums\":[\"object\", "
            + "\"when\", \"fun\", \"return\", \"open\", \"name\", \"ordinal\"]}",
        json);
    KeywordKotlin parseKeyword = gson.fromJson(json, KeywordKotlin.class);
    assertThat(parseKeyword).isEqualTo(keyword);

    String generatedNamedJson =
        "{\"object_\":\"object\",\"when_\":1, \"fun_\":{}, \"return_\":[], "
            + "\"enums\":[\"object_\", \"when_\", \"fun_\", \"return_\", \"open_\", \"name_\", "
            + "\"ordinal_\"]}";
    assertThat(gson.fromJson(generatedNamedJson, KeywordKotlin.class)).isEqualTo(keyword);
  }

  @Test
  public void usedKeywordsInJava() {
    KeywordJava keyword =
        new KeywordJava.Builder()
            .final_("final")
            .public_(true)
            .enums(
                Arrays.asList(
                    KeywordJavaEnum.final_,
                    KeywordJavaEnum.public_,
                    KeywordJavaEnum.package_,
                    KeywordJavaEnum.return_))
            .build();
    String json = gson.toJson(keyword);
    assertJsonEquals(
        "{\"final\":\"final\", \"public\":true, \"package\":{}, \"return\":[], "
            + "\"enums\":[\"final\", \"public\", \"package\", \"return\"]}",
        json);
    KeywordJava parseKeyword = gson.fromJson(json, KeywordJava.class);
    assertThat(parseKeyword).isEqualTo(keyword);

    String generatedNamedJson =
        "{\"final_\":\"final\", \"public_\":true, \"package_\":{}, "
            + "\"return_\":[], \"enums\":[\"final_\", \"public_\", \"package_\", \"return_\"]}";
    assertThat(gson.fromJson(generatedNamedJson, KeywordJava.class)).isEqualTo(keyword);
  }

  @Test
  public void enumKeywordsAtRootInKotlin() {
    KeywordKotlinEnum constant = KeywordKotlinEnum.object_;
    String json = gson.toJson(constant);
    assertJsonEquals("\"object\"", json);
    KeywordKotlinEnum parseKeyword = gson.fromJson(json, KeywordKotlinEnum.class);
    assertThat(parseKeyword).isEqualTo(constant);

    String generatedNamedJson = "\"object_\"";
    assertThat(gson.fromJson(generatedNamedJson, KeywordKotlinEnum.class)).isEqualTo(constant);
  }

  @Test
  public void enumKeywordsAtRootInJava() {
    KeywordJavaEnum constant = KeywordJavaEnum.final_;
    String json = gson.toJson(constant);
    assertJsonEquals("\"final\"", json);
    KeywordJavaEnum parseKeyword = gson.fromJson(json, KeywordJavaEnum.class);
    assertThat(parseKeyword).isEqualTo(constant);

    String generatedNamedJson = "\"final_\"";
    assertThat(gson.fromJson(generatedNamedJson, KeywordJavaEnum.class)).isEqualTo(constant);
  }

  @Test
  public void kotlinWithoutBuilderFromJson() {
    Person person =
        gson.fromJson(
            "{"
                + "\"id\":1,"
                + "\"name\":\"Jo\","
                + "\"email\":\"foo@square.com\","
                + "\"phone\":[{\"number\": \"555-555-5555\"}, {\"number\": \"444-444-4444\"}],"
                + "\"favorite_numbers\":[1, 2, 3],"
                + "\"area_numbers\":{\"519\":\"555-5555\"},"
                + "\"is_canadian\":true"
                + "}",
            Person.class);
    assertThat(person)
        .isEqualTo(
            new Person(
                "Jo",
                1,
                "foo@square.com",
                Arrays.asList(
                    new PhoneNumber("555-555-5555", null, ByteString.EMPTY),
                    new PhoneNumber("444-444-4444", null, ByteString.EMPTY)),
                Arrays.asList(1, 2, 3),
                ImmutableMap.<Integer, String>builder().put(519, "555-5555").build(),
                true,
                ByteString.EMPTY));
  }

  @Test
  public void kotlinWithoutBuilderToJson() {
    Person person =
        new Person(
            "Jo",
            1,
            "foo@square.com",
            Arrays.asList(
                new PhoneNumber("555-555-5555", null, ByteString.EMPTY),
                new PhoneNumber("444-444-4444", null, ByteString.EMPTY)),
            Arrays.asList(1, 2, 3),
            ImmutableMap.<Integer, String>builder().put(519, "555-5555").build(),
            false,
            ByteString.EMPTY);
    String json = gson.toJson(person);
    assertJsonEquals(
        "{"
            + "\"id\":1,"
            + "\"name\":\"Jo\","
            + "\"email\":\"foo@square.com\","
            + "\"phone\":[{\"number\": \"555-555-5555\"}, {\"number\": \"444-444-4444\"}],"
            + "\"favorite_numbers\":[1, 2, 3],"
            + "\"area_numbers\":{\"519\":\"555-5555\"},"
            + "\"is_canadian\":false"
            + "}",
        json);
  }

  @Test
  public void kotlinGettersFromJson() {
    Getters getters =
        gson.fromJson("{\"isa\":1,\"isA\":2,\"is_a\":3,\"is32\":32,\"isb\":true}", Getters.class);
    assertThat(getters).isEqualTo(new Getters(1, 2, 3, 32, true, ByteString.EMPTY));
  }

  @Test
  public void kotlinGettersToJson() {
    Getters getters = new Getters(1, 2, 3, 32, true, ByteString.EMPTY);
    String json = gson.toJson(getters);
    assertJsonEquals("{\"isa\":1,\"isA\":2,\"is_a\":3,\"is32\":32,\"isb\":true}", json);
  }
}
