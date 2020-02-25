/*
 * Copyright 2019 Square Inc.
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
import com.squareup.dinosaurs.Dinosaur;
import com.squareup.geology.Period;
import java.util.Arrays;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Using Gson without the adapter is quite fragile: it leaks implementation details of how Gson
 * works, and it breaks on absent collection types. Nevertheless this is sufficiently commonplace
 * that we support it like it were a public API.
 */
public class GsonNoAdapterTest {
  private final Gson gson = new GsonBuilder()
      .disableHtmlEscaping()
      .create();

  /** This test passes with Wire 2.2. */
  @Test public void fullObject() {
    Dinosaur value = new Dinosaur.Builder()
        .name("Stegosaurus")
        .period(Period.JURASSIC)
        .length_meters(9.0)
        .mass_kilograms(5_000.0)
        .picture_urls(Arrays.asList("http://goo.gl/LD5KY5", "http://goo.gl/VYRM67"))
        .build();
    String json = "{"
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

  /** This test passes with Wire 2.2. */
  @Test public void listsOnly() {
    Dinosaur value = new Dinosaur.Builder()
        .build();
    String json = "{"
        + "\"picture_urls\":[]"
        + "}";
    assertThat(gson.toJson(value)).isEqualTo(json);
    Dinosaur decoded = gson.fromJson(json, Dinosaur.class);
    assertThat(decoded).isEqualTo(value);
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode());
    assertThat(decoded.toString()).isEqualTo(value.toString());
  }

  /** This test fails with Wire 2.2 because absent lists are initialized to null. */
  @Ignore
  @Test public void emptyObject() {
    Dinosaur value = new Dinosaur.Builder()
        .build();
    String json = "{}";
    assertThat(gson.toJson(value)).isEqualTo("{\"picture_urls\":[]}");
    Dinosaur decoded = gson.fromJson(json, Dinosaur.class);
    assertThat(decoded).isEqualTo(value);
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode());
    assertThat(decoded.toString()).isEqualTo(value.toString());
  }
}
