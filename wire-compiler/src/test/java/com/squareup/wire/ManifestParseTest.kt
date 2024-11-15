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
package com.squareup.wire

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.containsOnly
import assertk.assertions.messageContains
import org.junit.Assert.fail
import org.junit.Test

class ManifestParseTest {
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

    val modules = parseManifestModules(yaml)
    assertThat(modules.keys).containsExactlyInAnyOrder("one", "two", "three")

    val one = modules.getValue("one")
    assertThat(one.dependencies).containsExactlyInAnyOrder("two", "three")
    assertThat(one.pruningRules!!.roots).containsExactlyInAnyOrder("example.A", "example.B")
    assertThat(one.pruningRules!!.prunes).containsExactlyInAnyOrder("example.C", "example.D")
  }

  @Test fun parseFormatFailsOnUnknownKey() {
    val yaml = """
      |one:
      |  includes:
      |    - example.A
    """.trimMargin()

    try {
      parseManifestModules(yaml)
      fail()
    } catch (e: Exception) {
      assertThat(e).messageContains("Unknown property 'includes'")
    }
  }

  @Test fun outOfOrderDependency() {
    val yaml = """
      |one:
      |  dependencies:
      |    - two
      |two: {}
    """.trimMargin()

    val modules = parseManifestModules(yaml)
    assertThat(modules.keys).containsOnly("one", "two")
    assertThat(modules.getValue("one").dependencies).containsOnly("two")
  }
}
