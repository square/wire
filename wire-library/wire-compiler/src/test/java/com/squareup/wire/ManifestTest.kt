/*
 * Copyright 2020 Square Inc.
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
package com.squareup.wire

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test

class ManifestTest {
  @Test fun parseFormat() {
    val yaml = """
      |one:
      |  dependencies:
      |    - two
      |    - three
      |  roots:
      |    - example.A
      |    - example.B
      |  prunes:
      |    - example.C
      |    - example.D
      |two: {}
      |three: {}
    """.trimMargin()

    val manifest = Manifest.fromYaml(yaml)
    assertThat(manifest.compilationUnits.keys).containsExactlyInAnyOrder("one", "two", "three")

    val one = manifest.compilationUnits.getValue("one")
    assertThat(one.dependencies).containsExactlyInAnyOrder("two", "three")
    assertThat(one.roots).containsExactlyInAnyOrder("example.A", "example.B")
    assertThat(one.prunes).containsExactlyInAnyOrder("example.C", "example.D")
  }

  @Test fun parseFormatFailsOnUnknownKey() {
    val yaml = """
      |one:
      |  includes:
      |    - example.A
    """.trimMargin()

    try {
      Manifest.fromYaml(yaml)
      fail()
    } catch (e: Exception) {
      assertThat(e).hasMessageContaining("Unknown property 'includes'")
    }
  }

  @Test fun outOfOrderDependency() {
    val yaml = """
      |one:
      |  dependencies:
      |    - two
      |two: {}
    """.trimMargin()

    val manifest = Manifest.fromYaml(yaml)
    assertThat(manifest.compilationUnits.keys).containsExactly("one", "two")
    assertThat(manifest.compilationUnits.getValue("one").dependencies).containsExactly("two")
  }

  @Test fun dependencyCycleThrows() {
    val yaml = """
      |one:
      |  dependencies:
      |    - three
      |two:
      |  dependencies:
      |   - one
      |three:
      |  dependencies:
      |    - two
    """.trimMargin()

    try {
      Manifest.fromYaml(yaml)
      fail()
    } catch (e: IllegalArgumentException) {
      assertThat(e).hasMessage("three's dependency on two forms a cycle")
    }
  }
}
