/*
 * Copyright (C) 2020 Square, Inc.
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
package com.squareup.wire.schema.internal

import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.RepoBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TypeMoverTest {
  /**
   * Move a type from one schema to another.
   *
   * This move triggers 3 import changes:
   *
   *  * Adding an import from the source file to the target file. (espresso.proto)
   *  * Removing an import from the source file that was only used by the target type. (roast.proto)
   *  * Adding an import to the target file required by the target type (roast.proto).
   */
  @Test fun `move type to new file`() {
    val oldSchema = RepoBuilder()
        .add("cafe/cafe.proto", """
            |syntax = "proto2";
            |
            |package cafe;
            |
            |import "cafe/roast.proto";
            |
            |message CafeDrink {
            |  optional int32 size_ounces = 1;
            |  repeated EspressoShot shots = 2;
            |}
            |
            |message EspressoShot {
            |  optional Roast roast = 1;
            |  optional bool decaf = 2;
            |}
            """.trimMargin()
        )
        .add("cafe/roast.proto", """
            |syntax = "proto2";
            |
            |package cafe;
            |
            |enum Roast {
            |  MEDIUM = 1;
            |  DARK = 2;
            |}
            """.trimMargin()
        )
        .schema()

    val newSchema = TypeMover(oldSchema,
        listOf(Move(ProtoType.get("cafe", "EspressoShot"), "cafe/espresso.proto"))
    ).move()

    assertThat(newSchema.protoFile("cafe/cafe.proto")!!.toSchema()).isEqualTo("""
        |// cafe/cafe.proto
        |syntax = "proto2";
        |package cafe;
        |
        |import "cafe/espresso.proto";
        |
        |message CafeDrink {
        |  optional int32 size_ounces = 1;
        |
        |  repeated EspressoShot shots = 2;
        |}
        |""".trimMargin()
    )
    assertThat(newSchema.protoFile("cafe/espresso.proto")!!.toSchema()).isEqualTo("""
        |// cafe/espresso.proto
        |syntax = "proto2";
        |package cafe;
        |
        |import "cafe/roast.proto";
        |
        |message EspressoShot {
        |  optional Roast roast = 1;
        |
        |  optional bool decaf = 2;
        |}
        |""".trimMargin()
    )
  }

  @Test fun `move type to existing file`() {
    val oldSchema = RepoBuilder()
        .add("cafe/cafe.proto", """
            |syntax = "proto2";
            |
            |package cafe;
            |
            |import "cafe/roast.proto";
            |
            |message CafeDrink {
            |  optional int32 size_ounces = 1;
            |  repeated EspressoShot shots = 2;
            |}
            |
            |message EspressoShot {
            |  optional Roast roast = 1;
            |  optional bool decaf = 2;
            |}
            """.trimMargin()
        )
        .add("cafe/roast.proto", """
            |syntax = "proto2";
            |
            |package cafe;
            |
            |enum Roast {
            |  MEDIUM = 1;
            |  DARK = 2;
            |}
            """.trimMargin()
        )
        .schema()

    val newSchema = TypeMover(oldSchema,
        listOf(Move(ProtoType.get("cafe", "EspressoShot"), "cafe/roast.proto"))
    ).move()

    assertThat(newSchema.protoFile("cafe/cafe.proto")!!.toSchema()).isEqualTo("""
        |// cafe/cafe.proto
        |syntax = "proto2";
        |package cafe;
        |
        |import "cafe/roast.proto";
        |
        |message CafeDrink {
        |  optional int32 size_ounces = 1;
        |
        |  repeated EspressoShot shots = 2;
        |}
        |""".trimMargin()
    )
    assertThat(newSchema.protoFile("cafe/roast.proto")!!.toSchema()).isEqualTo("""
        |// cafe/roast.proto
        |syntax = "proto2";
        |package cafe;
        |
        |enum Roast {
        |  MEDIUM = 1;
        |  DARK = 2;
        |}
        |
        |message EspressoShot {
        |  optional Roast roast = 1;
        |
        |  optional bool decaf = 2;
        |}
        |""".trimMargin()
    )
  }
}
