/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.wire.MessageAdapter;
import java.util.Map;
import okio.Buffer;
import okio.ByteString;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class SchemaTypeAdapterTest {
  final Schema coffeeSchema = new SchemaBuilder()
      .add("coffee.proto", ""
          + "message CafeDrink {\n"
          + "  optional string customer_name = 1;\n"
          + "  repeated EspressoShot shots = 2;\n"
          + "  optional Foam foam = 3;\n"
          + "  optional int32 size_ounces = 14;\n"
          + "  optional Dairy dairy = 15;\n"
          + "\n"
          + "  enum Foam {\n"
          + "    NOT_FOAMY_AND_QUITE_BORING = 1;\n"
          + "    ZOMG_SO_FOAMY = 3;\n"
          + "  }\n"
          + "}\n"
          + "\n"
          + "message Dairy {\n"
          + "  optional int32 count = 2;\n"
          + "  optional string type = 1;\n"
          + "}\n"
          + "\n"
          + "message EspressoShot {\n"
          + "  optional string bean_type = 1;\n"
          + "  optional double caffeine_level = 2;\n"
          + "}\n")
      .build();

  final SchemaTypeAdapterFactory typeAdapterFactory = new SchemaTypeAdapterFactory(coffeeSchema);

  // Golden data emitted by protoc using the schema above.
  final ImmutableMap<String, Object> dansCoffee = ImmutableMap.<String, Object>of(
      "customer_name", "Dan",
      "shots", ImmutableList.of(
          ImmutableMap.of("caffeine_level", 0.5d)
      ),
      "size_ounces", 16,
      "dairy", ImmutableMap.of("count", 1));
  final ByteString dansCoffeeEncoded = ByteString.decodeBase64("CgNEYW4SCREAAAAAAADgP3AQegIQAQ==");

  final ImmutableMap<String, Object> jessesCoffee = ImmutableMap.<String, Object>of(
      "customer_name", "Jesse",
      "shots", ImmutableList.of(
          ImmutableMap.of("bean_type", "colombian", "caffeine_level", 1.0d),
          ImmutableMap.of("bean_type", "colombian", "caffeine_level", 1.0d)
      ),
      "foam", "ZOMG_SO_FOAMY",
      "size_ounces", 24);
  final ByteString jessesCoffeeEncoded = ByteString.decodeBase64(
      "CgVKZXNzZRIUCgljb2xvbWJpYW4RAAAAAAAA8D8SFAoJY29sb21iaWFuEQAAAAAAAPA/GANwGA==");

  @Test public void typeAdapterDecode() throws Exception {
    MessageType cafeDrink = (MessageType) coffeeSchema.getType("CafeDrink");
    MessageAdapter<Map<String, Object>> adapter = typeAdapterFactory.get(cafeDrink);
    assertThat(adapter.read(new Buffer().write(dansCoffeeEncoded))).isEqualTo(dansCoffee);
    assertThat(adapter.read(new Buffer().write(jessesCoffeeEncoded))).isEqualTo(jessesCoffee);
  }
}
