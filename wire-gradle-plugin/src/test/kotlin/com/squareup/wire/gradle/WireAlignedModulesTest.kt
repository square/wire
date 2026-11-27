/*
 * Copyright (C) 2026 Square, Inc.
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
package com.squareup.wire.gradle

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isNotEmpty
import java.io.File
import org.junit.Test

class WireAlignedModulesTest {
  /**
   * The plugin aligns the modules of the BOM. A module added to the BOM only would not be aligned,
   * so this test fails until it is added to [WirePlugin.wireAlignedModules] too.
   */
  @Test
  fun alignedModulesMatchTheBom() {
    val bomModules = bomModules(File("../wire-bom/build.gradle.kts").readText())
    assertThat(bomModules).isNotEmpty()

    // The plugin is on the build classpath, not on the project classpath.
    val expected = bomModules - "wire-gradle-plugin"

    assertThat(WirePlugin.wireAlignedModules).containsExactlyInAnyOrder(*expected.toTypedArray())
  }

  /**
   * Reads the constraints of the BOM: `api(projects.wireGrpcClient)` gives `wire-grpc-client`, and
   * `api(projects.wireGrpcClient.group + ":wire-grpc-client-jvm:" + ...)` gives
   * `wire-grpc-client-jvm`.
   */
  private fun bomModules(bomScript: String): Set<String> {
    val constraints = bomScript.substringAfter("constraints {").substringBefore("}")
    val accessors = Regex("""api\(projects\.(\w+)\)""").findAll(constraints)
      .map { it.groupValues[1].camelToKebabCase() }
    val coordinates = Regex("""":(wire-[a-z0-9-]+):"""").findAll(constraints)
      .map { it.groupValues[1] }
    return (accessors + coordinates).toSet()
  }

  private fun String.camelToKebabCase(): String = replace(Regex("([a-z0-9])([A-Z])"), "$1-$2").lowercase()
}
