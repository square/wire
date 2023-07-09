/*
 * Copyright (C) 2019 Square, Inc.
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.wire.proto2.dinosaurs.java.Dinosaur;
import com.squareup.wire.proto2.geology.java.Period;
import java.util.Arrays;
import java.util.Collections;
import okio.ByteString;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Using Gson without the adapter is quite fragile: it leaks implementation details of how Gson
 * works, and it breaks on absent collection types. Nevertheless this is sufficiently commonplace
 * that we support it like it were a public API.
 */
public class GsonNoAdapterTest {
  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  @Test
  public void javaFullObject() {
    Dinosaur value =
        new Dinosaur.Builder()
            .name("Stegosaurus")
            .period(Period.JURASSIC)
            .length_meters(9.0)
            .mass_kilograms(5_000.0)
            .picture_urls(Arrays.asList("http://goo.gl/LD5KY5", "http://goo.gl/VYRM67"))
            .build();
    String json =
        "{"
            + "\"name\":\"Stegosaurus\","
            + "\"picture_urls\":[\"http://goo.gl/LD5KY5\",\"http://goo.gl/VYRM67\"],"
            + "\"length_meters\":9.0,"
            + "\"mass_kilograms\":5000.0,"
            + "\"period\":\"JURASSIC\""
            + "}";
    assertThat(gson.toJson(value)).isEqualTo(json);
    Dinosaur decoded = gson.fromJson(json, Dinosaur.class);
    assertThat(decoded).isEqualTo(value);
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode());
    assertThat(decoded.toString()).isEqualTo(value.toString());
  }

  @Test
  public void javaListsOnly() {
    Dinosaur value = new Dinosaur.Builder().build();
    String json = "{" + "\"picture_urls\":[]" + "}";
    assertThat(gson.toJson(value)).isEqualTo(json);
    Dinosaur decoded = gson.fromJson(json, Dinosaur.class);
    assertThat(decoded).isEqualTo(value);
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode());
    assertThat(decoded.toString()).isEqualTo(value.toString());
  }

  @Ignore("Absent lists are initialized to null in Java.")
  @Test
  public void javaEmptyObject() {
    Dinosaur value = new Dinosaur.Builder().build();
    String json = "{}";
    assertThat(gson.toJson(value)).isEqualTo("{\"picture_urls\":[]}");
    Dinosaur decoded = gson.fromJson(json, Dinosaur.class);
    assertThat(decoded).isEqualTo(value);
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode());
    assertThat(decoded.toString()).isEqualTo(value.toString());
  }

  @Test
  public void interopFullObject() {
    com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur value =
        new com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur.Builder()
            .name("Stegosaurus")
            .period(com.squareup.wire.proto2.geology.javainteropkotlin.Period.JURASSIC)
            .length_meters(9.0)
            .mass_kilograms(5_000.0)
            .picture_urls(Arrays.asList("http://goo.gl/LD5KY5", "http://goo.gl/VYRM67"))
            .build();
    String json =
        "{"
            + "\"name\":\"Stegosaurus\","
            + "\"length_meters\":9.0,"
            + "\"mass_kilograms\":5000.0,"
            + "\"period\":\"JURASSIC\","
            + "\"picture_urls\":[\"http://goo.gl/LD5KY5\",\"http://goo.gl/VYRM67\"]"
            + "}";
    assertThat(gson.toJson(value)).isEqualTo(json);
    com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur decoded =
        gson.fromJson(json, com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur.class);
    assertThat(decoded).isEqualTo(value);
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode());
    assertThat(decoded.toString()).isEqualTo(value.toString());
  }

  @Test
  public void interopListsOnly() {
    com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur value =
        new com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur.Builder().build();
    String json = "{" + "\"picture_urls\":[]" + "}";
    assertThat(gson.toJson(value)).isEqualTo(json);
    com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur decoded =
        gson.fromJson(json, com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur.class);
    assertThat(decoded).isEqualTo(value);
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode());
    assertThat(decoded.toString()).isEqualTo(value.toString());
  }

  @Test
  public void interopEmptyObject() {
    com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur value =
        new com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur.Builder().build();
    String json = "{}";
    assertThat(gson.toJson(value)).isEqualTo("{\"picture_urls\":[]}");
    com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur decoded =
        gson.fromJson(json, com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur.class);
    assertThat(decoded).isEqualTo(value);
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode());
    assertThat(decoded.toString()).isEqualTo(value.toString());
  }

  @Test
  public void kotlinFullObject() {
    com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur value =
        new com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur(
            "Stegosaurus",
            Arrays.asList("http://goo.gl/LD5KY5", "http://goo.gl/VYRM67"),
            9.0,
            5_000.0,
            com.squareup.wire.proto2.geology.kotlin.Period.JURASSIC,
            ByteString.EMPTY);
    String json =
        "{"
            + "\"name\":\"Stegosaurus\","
            + "\"length_meters\":9.0,"
            + "\"mass_kilograms\":5000.0,"
            + "\"period\":\"JURASSIC\","
            + "\"picture_urls\":[\"http://goo.gl/LD5KY5\",\"http://goo.gl/VYRM67\"]"
            + "}";
    assertThat(gson.toJson(value)).isEqualTo(json);
    com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur decoded =
        gson.fromJson(json, com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur.class);
    assertThat(decoded).isEqualTo(value);
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode());
    assertThat(decoded.toString()).isEqualTo(value.toString());
  }

  @Test
  public void kotlinListsOnly() {
    com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur value =
        new com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur(
            null, Collections.emptyList(), null, null, null, ByteString.EMPTY);
    String json = "{" + "\"picture_urls\":[]" + "}";
    assertThat(gson.toJson(value)).isEqualTo(json);
    com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur decoded =
        gson.fromJson(json, com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur.class);
    assertThat(decoded).isEqualTo(value);
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode());
    assertThat(decoded.toString()).isEqualTo(value.toString());
  }

  @Test
  public void kotlinEmptyObject() {
    com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur value =
        new com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur(
            null, Collections.emptyList(), null, null, null, ByteString.EMPTY);
    String json = "{}";
    assertThat(gson.toJson(value)).isEqualTo("{\"picture_urls\":[]}");
    com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur decoded =
        gson.fromJson(json, com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur.class);
    assertThat(decoded).isEqualTo(value);
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode());
    assertThat(decoded.toString()).isEqualTo(value.toString());
  }
}
