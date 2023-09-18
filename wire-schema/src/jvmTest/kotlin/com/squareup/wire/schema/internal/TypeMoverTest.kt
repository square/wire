/*
 * Copyright (C) 2020 Square, Inc.
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
package com.squareup.wire.schema.internal

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.ProtoType
import kotlin.test.Test
import kotlin.test.fail
import okio.Path.Companion.toPath

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
  @Test fun moveTypeToNewFile() {
    val oldSchema = buildSchema {
      add(
        "cafe/cafe.proto".toPath(),
        """
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
        """.trimMargin(),
      )
      add(
        "cafe/roast.proto".toPath(),
        """
            |syntax = "proto2";
            |
            |package cafe;
            |
            |enum Roast {
            |  MEDIUM = 1;
            |  DARK = 2;
            |}
        """.trimMargin(),
      )
    }

    val newSchema = TypeMover(
      oldSchema,
      listOf(TypeMover.Move(ProtoType.get("cafe", "EspressoShot"), "cafe/espresso.proto")),
    ).move()

    assertThat(newSchema.protoFile("cafe/cafe.proto")!!.toSchema()).isEqualTo(
      """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: cafe/cafe.proto
        |
        |syntax = "proto2";
        |
        |package cafe;
        |
        |import "cafe/espresso.proto";
        |
        |message CafeDrink {
        |  optional int32 size_ounces = 1;
        |
        |  repeated EspressoShot shots = 2;
        |}
        |
      """.trimMargin(),
    )
    assertThat(newSchema.protoFile("cafe/espresso.proto")!!.toSchema()).isEqualTo(
      """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: cafe/espresso.proto
        |
        |syntax = "proto2";
        |
        |package cafe;
        |
        |import "cafe/roast.proto";
        |
        |message EspressoShot {
        |  optional Roast roast = 1;
        |
        |  optional bool decaf = 2;
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun moveTypeToExistingFile() {
    val oldSchema = buildSchema {
      add(
        "cafe/cafe.proto".toPath(),
        """
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
        """.trimMargin(),
      )
      add(
        "cafe/roast.proto".toPath(),
        """
            |syntax = "proto2";
            |
            |package cafe;
            |
            |enum Roast {
            |  MEDIUM = 1;
            |  DARK = 2;
            |}
        """.trimMargin(),
      )
    }

    val newSchema = TypeMover(
      oldSchema,
      listOf(TypeMover.Move(ProtoType.get("cafe", "EspressoShot"), "cafe/roast.proto")),
    ).move()

    assertThat(newSchema.protoFile("cafe/cafe.proto")!!.toSchema()).isEqualTo(
      """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: cafe/cafe.proto
        |
        |syntax = "proto2";
        |
        |package cafe;
        |
        |import "cafe/roast.proto";
        |
        |message CafeDrink {
        |  optional int32 size_ounces = 1;
        |
        |  repeated EspressoShot shots = 2;
        |}
        |
      """.trimMargin(),
    )
    assertThat(newSchema.protoFile("cafe/roast.proto")!!.toSchema()).isEqualTo(
      """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: cafe/roast.proto
        |
        |syntax = "proto2";
        |
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
        |
      """.trimMargin(),
    )
  }

  @Test fun multipleMovesFromSingleSource() {
    val oldSchema = buildSchema {
      add(
        "abc.proto".toPath(),
        """
            |syntax = "proto2";
            |
            |message A {
            |  optional B b = 1;
            |  optional C c = 2;
            |}
            |
            |message B {
            |  optional C c = 1;
            |}
            |
            |message C {
            |}
        """.trimMargin(),
      )
    }

    val newSchema = TypeMover(
      oldSchema = oldSchema,
      moves = listOf(
        TypeMover.Move(ProtoType.get("A"), "a.proto"),
        TypeMover.Move(ProtoType.get("B"), "b.proto"),
        TypeMover.Move(ProtoType.get("C"), "c.proto"),
      ),
    ).move()

    assertThat(newSchema.protoFile("abc.proto")!!.toSchema()).isEqualTo(
      """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: abc.proto
        |
        |syntax = "proto2";
        |
      """.trimMargin(),
    )

    assertThat(newSchema.protoFile("a.proto")!!.toSchema()).isEqualTo(
      """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: a.proto
        |
        |syntax = "proto2";
        |
        |import "b.proto";
        |import "c.proto";
        |
        |message A {
        |  optional B b = 1;
        |
        |  optional C c = 2;
        |}
        |
      """.trimMargin(),
    )

    assertThat(newSchema.protoFile("b.proto")!!.toSchema()).isEqualTo(
      """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: b.proto
        |
        |syntax = "proto2";
        |
        |import "c.proto";
        |
        |message B {
        |  optional C c = 1;
        |}
        |
      """.trimMargin(),
    )

    assertThat(newSchema.protoFile("c.proto")!!.toSchema()).isEqualTo(
      """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: c.proto
        |
        |syntax = "proto2";
        |
        |message C {}
        |
      """.trimMargin(),
    )
  }

  @Test fun moveWithServiceDependency() {
    val oldSchema = buildSchema {
      add(
        "abc.proto".toPath(),
        """
            |syntax = "proto2";
            |
            |message A {
            |}
            |
            |message B {
            |}
            |
            |service C {
            |  rpc Go (A) returns (B);
            |}
        """.trimMargin(),
      )
    }

    val newSchema = TypeMover(
      oldSchema = oldSchema,
      moves = listOf(
        TypeMover.Move(ProtoType.get("A"), "a.proto"),
        TypeMover.Move(ProtoType.get("B"), "b.proto"),
      ),
    ).move()

    assertThat(newSchema.protoFile("abc.proto")!!.toSchema()).isEqualTo(
      """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: abc.proto
        |
        |syntax = "proto2";
        |
        |import "a.proto";
        |import "b.proto";
        |
        |service C {
        |  rpc Go (A) returns (B);
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun swapTypes() {
    val oldSchema = buildSchema {
      add(
        "a.proto".toPath(),
        """
            |syntax = "proto2";
            |
            |import "b.proto";
            |
            |message A {
            |  optional B b = 1;
            |}
        """.trimMargin(),
      )
      add(
        "b.proto".toPath(),
        """
            |syntax = "proto2";
            |
            |message B {
            |}
        """.trimMargin(),
      )
    }

    val newSchema = TypeMover(
      oldSchema = oldSchema,
      moves = listOf(
        TypeMover.Move(ProtoType.get("A"), "b.proto"),
        TypeMover.Move(ProtoType.get("B"), "a.proto"),
      ),
    ).move()

    assertThat(newSchema.protoFile("b.proto")!!.toSchema()).isEqualTo(
      """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: b.proto
        |
        |syntax = "proto2";
        |
        |import "a.proto";
        |
        |message A {
        |  optional B b = 1;
        |}
        |
      """.trimMargin(),
    )
    assertThat(newSchema.protoFile("a.proto")!!.toSchema()).isEqualTo(
      """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: a.proto
        |
        |syntax = "proto2";
        |
        |message B {}
        |
      """.trimMargin(),
    )
  }

  @Test fun unrelatedUnusedImportsNotPruned() {
    val oldSchema = buildSchema {
      add(
        "a.proto".toPath(),
        """
            |syntax = "proto2";
            |
            |import "b.proto";
            |
            |message A {
            |}
        """.trimMargin(),
      )
      add(
        "b.proto".toPath(),
        """
            |syntax = "proto2";
            |
        """.trimMargin(),
      )
    }

    val newSchema = TypeMover(
      oldSchema = oldSchema,
      moves = listOf(
        TypeMover.Move(ProtoType.get("A"), "c.proto"),
      ),
    ).move()

    assertThat(newSchema.protoFile("a.proto")!!.toSchema()).isEqualTo(
      """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: a.proto
        |
        |syntax = "proto2";
        |
        |import "b.proto";
        |
      """.trimMargin(),
    )
  }

  @Test fun moveInexistentType() {
    val oldSchema = buildSchema {
      add(
        "a.proto".toPath(),
        """
            |syntax = "proto2";
            |
            |message A {
            |}
        """.trimMargin(),
      )
    }

    try {
      TypeMover(
        oldSchema = oldSchema,
        moves = listOf(
          TypeMover.Move(ProtoType.get("B"), "b.proto"),
        ),
      ).move()
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("cannot move B, it isn't in this schema")
    }
  }
}
