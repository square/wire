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
package com.squareup.wire.java.internal

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.parser.OptionElement
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test

class ProfileParserTest {
  private var location = Location.get("android.wire")
  @Test
  fun parseType() {
    val proto =
      """
      |syntax = "wire2";
      |package squareup.dinosaurs;
      |
      |import "squareup/geology/period.proto";
      |
      |/* Roar! */
      |type squareup.dinosaurs.Dinosaur {
      |  target com.squareup.dino.Dinosaur using com.squareup.dino.Dinosaurs#DINO_ADAPTER;
      |}
      |
      |type squareup.geology.Period {
      |  with custom_type = "duration";
      |  target java.time.Period using com.squareup.time.Time#PERIOD_ADAPTER;
      |}
      """.trimMargin()
    val expected = ProfileFileElement(
        location = location,
        packageName = "squareup.dinosaurs",
        imports = listOf("squareup/geology/period.proto"),
        typeConfigs = listOf(
            TypeConfigElement(
                location = location.at(7, 1),
                documentation = "Roar!",
                type = "squareup.dinosaurs.Dinosaur",
                target = "com.squareup.dino.Dinosaur",
                adapter = "com.squareup.dino.Dinosaurs#DINO_ADAPTER"
            ),

            TypeConfigElement(
                location = location.at(11, 1),
                type = "squareup.geology.Period",
                with = listOf(
                    OptionElement.create(
                        "custom_type", OptionElement.Kind.STRING, "duration"
                    )
                ),
                target = "java.time.Period",
                adapter = "com.squareup.time.Time#PERIOD_ADAPTER"
            )
        )
    )

    val parser = ProfileParser(location, proto)
    assertThat(parser.read()).isEqualTo(expected)
  }

  @Test
  fun invalidSyntax() {
    val proto =
      """
      |syntax = "proto2";
      |
      |type squareup.dinosaurs.Dinosaur {
      |  target com.squareup.dino.Dinosaur using com.squareup.dino.Dinosaurs#DINO_ADAPTER;
      |}
      """.trimMargin()
    val parser = ProfileParser(location, proto)
    try {
      parser.read()
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("Syntax error in android.wire at 1:18: expected 'wire2'")
    }
  }

  @Test
  fun tooManyTargets() {
    val proto =
      """
      |syntax = "wire2";
      |
      |type squareup.dinosaurs.Dinosaur {
      |  target com.squareup.dino.Herbivore using com.squareup.dino.Dinosaurs#PLANT_ADAPTER;
      |  target com.squareup.dino.Carnivore using com.squareup.dino.Dinosaurs#MEAT_ADAPTER;
      |}
      """.trimMargin()
    val parser = ProfileParser(location, proto)
    try {
      parser.read()
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("Syntax error in android.wire at 5:3: too many targets")
    }
  }

  @Test
  fun readAndWriteRoundTrip() {
    val proto =
      """
      |// android.wire
      |syntax = "wire2";
      |package squareup.dinosaurs;
      |
      |import "squareup/geology/period.proto";
      |
      |// Roar!
      |type squareup.dinosaurs.Dinosaur {
      |  target com.squareup.dino.Dinosaur using com.squareup.dino.Dinosaurs#DINO_ADAPTER;
      |}
      |type squareup.geology.Period {
      |  with custom_type = "duration";
      |  target java.time.Period using com.squareup.time.Time#PERIOD_ADAPTER;
      |}
      |""".trimMargin()
    val parser = ProfileParser(location, proto)
    assertThat(parser.read().toSchema()).isEqualTo(proto)
  }
}
