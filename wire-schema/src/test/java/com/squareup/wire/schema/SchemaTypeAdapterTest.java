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
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Map;
import okio.Buffer;
import okio.ByteString;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

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
    MessageAdapter<Map<String, Object>> adapter = typeAdapterFactory.get(cafeDrink.name());
    assertThat(adapter.read(new Buffer().write(dansCoffeeEncoded))).isEqualTo(dansCoffee);
    assertThat(adapter.read(new Buffer().write(jessesCoffeeEncoded))).isEqualTo(jessesCoffee);
  }

  @Test public void groupsIgnored() throws IOException {
    MessageAdapter<Map<String, Object>> adapter = new SchemaBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  optional string a = 1;\n"
            + "  // repeated group Group1 = 2 {\n"
            + "  //   optional SomeMessage a = 11;\n"
            + "  // }\n"
            + "  // repeated group Group2 = 3 {\n"
            + "  //   optional SomeMessage b = 21;\n"
            + "  // }\n"
            + "  optional string b = 4;\n"
            + "}\n")
        .buildTypeAdapter("Message");
    ByteString encoded = ByteString.decodeHex("0a0161135a02080114135a02100214135a090803720568656c6c"
        + "6f141baa010208011c1baa010210021c1baa01090803720568656c6c6f1c220162");
    ImmutableMap<String, Object> expected = ImmutableMap.<String, Object>of(
        "a", "a",
        "b", "b");
    assertThat(adapter.read(new Buffer().write(encoded))).isEqualTo(expected);
  }

  @Test public void startGroupWithoutEndGroup() throws IOException {
    MessageAdapter<Map<String, Object>> adapter = new SchemaBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  optional string a = 1;\n"
            + "}\n")
        .buildTypeAdapter("Message");
    ByteString encoded = ByteString.decodeHex("130a0161");
    try {
      adapter.read(new Buffer().write(encoded));
      fail();
    } catch (ProtocolException expected) {
      assertThat(expected).hasMessage("Protocol message group is truncated.");
    }
  }

  @Test public void unexpectedEndGroup() throws IOException {
    MessageAdapter<Map<String, Object>> adapter = new SchemaBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  optional string a = 1;\n"
            + "}\n")
        .buildTypeAdapter("Message");
    ByteString encoded = ByteString.decodeHex("0a01611c");
    try {
      adapter.read(new Buffer().write(encoded));
      fail();
    } catch (ProtocolException expected) {
      assertThat(expected).hasMessage("Unexpected end group in protocol message.");
    }
  }

  @Test public void endGroupDoesntMatchStartGroup() throws IOException {
    MessageAdapter<Map<String, Object>> adapter = new SchemaBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  optional string a = 1;\n"
            + "}\n")
        .buildTypeAdapter("Message");
    ByteString encoded = ByteString.decodeHex("130a01611c");
    try {
      adapter.read(new Buffer().write(encoded));
      fail();
    } catch (ProtocolException expected) {
      assertThat(expected).hasMessage("Unexpected end group in protocol message.");
    }
  }
}
