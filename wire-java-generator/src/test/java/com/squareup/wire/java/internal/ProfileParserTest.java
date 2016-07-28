/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.java.internal;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.Location;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class ProfileParserTest {
  Location location = Location.get("android.wire");

  @Test public void parseType() {
    String proto = ""
        + "syntax = \"wire2\";\n"
        + "package squareup.dinosaurs;\n"
        + "\n"
        + "import \"squareup/geology/period.proto\";\n"
        + "\n"
        + "/* Roar! */\n"
        + "type squareup.dinosaurs.Dinosaur {\n"
        + "  target com.squareup.dino.Dinosaur using com.squareup.dino.Dinosaurs#DINO_ADAPTER;\n"
        + "}\n"
        + "\n"
        + "type squareup.geology.Period {\n"
        + "  target java.time.Period using com.squareup.time.Time#PERIOD_ADAPTER;\n"
        + "}\n";
    ProfileFileElement expected = ProfileFileElement.builder(location)
        .packageName("squareup.dinosaurs")
        .imports(ImmutableList.of("squareup/geology/period.proto"))
        .typeConfigs(ImmutableList.of(
            TypeConfigElement.builder(location.at(7, 1))
                .documentation("Roar!")
                .type("squareup.dinosaurs.Dinosaur")
                .target("com.squareup.dino.Dinosaur")
                .adapter("com.squareup.dino.Dinosaurs#DINO_ADAPTER")
                .build(),
            TypeConfigElement.builder(location.at(11, 1))
                .type("squareup.geology.Period")
                .target("java.time.Period")
                .adapter("com.squareup.time.Time#PERIOD_ADAPTER")
                .build()))
        .build();
    ProfileParser parser = new ProfileParser(location, proto);
    assertThat(parser.read()).isEqualTo(expected);
  }

  @Test public void invalidSyntax() {
    String proto = ""
        + "syntax = \"proto2\";\n"
        + "\n"
        + "type squareup.dinosaurs.Dinosaur {\n"
        + "  target com.squareup.dino.Dinosaur using com.squareup.dino.Dinosaurs#DINO_ADAPTER;\n"
        + "}\n";
    ProfileParser parser = new ProfileParser(location, proto);
    try {
      parser.read();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("Syntax error in android.wire at 1:18: expected 'wire2'");
    }
  }

  @Test public void tooManyTargets() {
    String proto = ""
        + "syntax = \"wire2\";\n"
        + "\n"
        + "type squareup.dinosaurs.Dinosaur {\n"
        + "  target com.squareup.dino.Herbivore using com.squareup.dino.Dinosaurs#PLANT_ADAPTER;\n"
        + "  target com.squareup.dino.Carnivore using com.squareup.dino.Dinosaurs#MEAT_ADAPTER;\n"
        + "}\n";
    ProfileParser parser = new ProfileParser(location, proto);
    try {
      parser.read();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage("Syntax error in android.wire at 5:3: too many targets");
    }
  }
}
