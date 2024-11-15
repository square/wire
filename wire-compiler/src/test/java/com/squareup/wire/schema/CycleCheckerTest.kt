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
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire.schema

import assertk.assertThat
import assertk.assertions.hasMessage
import com.squareup.wire.testing.add
import kotlin.test.assertFailsWith
import okio.Path
import okio.fakefilesystem.FakeFileSystem
import org.junit.Test

class CycleCheckerTest {
  private val fs = FakeFileSystem().apply {
    if (Path.DIRECTORY_SEPARATOR == "\\") emulateWindows() else emulateUnix()
  }

  @Test
  fun singleFileImportCycle() {
    fs.add(
      "source-path/ouroboros.proto",
      """
        |syntax = "proto2";
        |import "ouroboros.proto";
        |message Snake {
        |}
      """.trimMargin(),
    )

    val exception = assertFailsWith<SchemaException> {
      loadAndLinkSchema()
    }
    assertThat(exception).hasMessage(
      """
        |imports form a cycle:
        |  ouroboros.proto:
        |    import "ouroboros.proto";
      """.trimMargin(),
    )
  }

  @Test
  fun threeFileImportCycle() {
    fs.add(
      "source-path/paper.proto",
      """
        |syntax = "proto2";
        |import "rock.proto";
        |message Paper {
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/rock.proto",
      """
        |syntax = "proto2";
        |import "scissors.proto";
        |message Rock {
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/scissors.proto",
      """
        |syntax = "proto2";
        |import "paper.proto";
        |message Scissors {
        |}
      """.trimMargin(),
    )

    val exception = assertFailsWith<SchemaException> {
      loadAndLinkSchema()
    }
    assertThat(exception).hasMessage(
      """
        |imports form a cycle:
        |  paper.proto:
        |    import "rock.proto";
        |  rock.proto:
        |    import "scissors.proto";
        |  scissors.proto:
        |    import "paper.proto";
      """.trimMargin(),
    )
  }

  @Test
  fun multipleCycleImportProblem() {
    fs.add(
      "source-path/a.proto",
      """
        |syntax = "proto2";
        |import "b.proto";
        |import "d.proto";
        |message A {
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/b.proto",
      """
        |syntax = "proto2";
        |import "c.proto";
        |message B {
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/c.proto",
      """
        |syntax = "proto2";
        |import "a.proto";
        |import "b.proto";
        |message C {
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/d.proto",
      """
        |syntax = "proto2";
        |message D {
        |}
      """.trimMargin(),
    )

    val exception = assertFailsWith<SchemaException> {
      loadAndLinkSchema()
    }
    assertThat(exception).hasMessage(
      """
        |imports form a cycle:
        |  a.proto:
        |    import "b.proto";
        |  b.proto:
        |    import "c.proto";
        |  c.proto:
        |    import "a.proto";
        |    import "b.proto";
      """.trimMargin(),
    )
  }

  /** The files form a dag, but the packages form a cycle. */
  @Test
  fun packageCycle() {
    fs.add(
      "source-path/people/employee.proto",
      """
        |syntax = "proto2";
        |import "locations/office.proto";
        |import "locations/residence.proto";
        |package people;
        |message Employee {
        |  optional locations.Office office = 1;
        |  optional locations.Residence residence = 2;
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/locations/office.proto",
      """
        |syntax = "proto2";
        |import "people/office_manager.proto";
        |package locations;
        |message Office {
        |  optional people.OfficeManager office_manager = 1;
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/locations/residence.proto",
      """
        |syntax = "proto2";
        |package locations;
        |message Residence {
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/people/office_manager.proto",
      """
        |syntax = "proto2";
        |package people;
        |message OfficeManager {
        |}
      """.trimMargin(),
    )

    val exception = assertFailsWith<SchemaException> {
      loadAndLinkSchema()
    }
    assertThat(exception).hasMessage(
      """
        |packages form a cycle:
        |  locations imports people
        |    locations/office.proto:
        |      import "people/office_manager.proto";
        |  people imports locations
        |    people/employee.proto:
        |      import "locations/office.proto";
        |      import "locations/residence.proto";
      """.trimMargin(),
    )
  }

  /**
   * In this example the files form a dag, but the packages don't because d.proto and e.proto are
   * in packages a and b. This test confirms that the Go package is used to detect and display
   * cycles.
   */
  @Test
  fun goPackagePreferredWhenResolvingPackageCycles() {
    fs.add(
      "source-path/a.proto",
      """
        |syntax = "proto2";
        |import "b.proto";
        |option go_package = "a";
      """.trimMargin(),
    )
    fs.add(
      "source-path/b.proto",
      """
        |syntax = "proto2";
        |package b;
        |import "c.proto";
        |option go_package = "b";
      """.trimMargin(),
    )
    fs.add(
      "source-path/c.proto",
      """
        |syntax = "proto2";
        |import "d.proto";
        |option go_package = "c";
      """.trimMargin(),
    )
    fs.add(
      "source-path/d.proto",
      """
        |syntax = "proto2";
        |package d;
        |import "e.proto";
        |option go_package = "a";
      """.trimMargin(),
    )
    fs.add(
      "source-path/e.proto",
      """
        |syntax = "proto2";
        |package b;
      """.trimMargin(),
    )

    val exception = assertFailsWith<SchemaException> {
      loadAndLinkSchema()
    }
    assertThat(exception).hasMessage(
      """
        |packages form a cycle:
        |  a imports b
        |    a.proto:
        |      import "b.proto";
        |    d.proto:
        |      import "e.proto";
        |  b imports c
        |    b.proto:
        |      import "c.proto";
        |  c imports a
        |    c.proto:
        |      import "d.proto";
      """.trimMargin(),
    )
  }

  /** Check messaging when the cycle involves a file without a package specified. */
  @Test
  fun emptyPackageCycle() {
    fs.add(
      "source-path/a.proto",
      """
        |syntax = "proto2";
        |import "b.proto";
      """.trimMargin(),
    )
    fs.add(
      "source-path/b.proto",
      """
        |syntax = "proto2";
        |package b;
        |import "c.proto";
      """.trimMargin(),
    )
    fs.add(
      "source-path/c.proto",
      """
        |syntax = "proto2";
      """.trimMargin(),
    )

    val exception = assertFailsWith<SchemaException> {
      loadAndLinkSchema()
    }
    assertThat(exception).hasMessage(
      """
        |packages form a cycle:
        |  <default> imports b
        |    a.proto:
        |      import "b.proto";
        |  b imports <default>
        |    b.proto:
        |      import "c.proto";
      """.trimMargin(),
    )
  }

  private fun loadAndLinkSchema(): Schema {
    val loader = SchemaLoader(fs)
    loader.initRoots(
      sourcePath = listOf(Location.get("source-path")),
      protoPath = listOf(),
    )
    return loader.loadSchema()
  }
}
