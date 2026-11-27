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
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import com.squareup.wire.gradle.WireVersionAlignmentWarnings.Companion.compareVersions
import org.junit.Test

class WireVersionAlignmentWarningsTest {
  @Test
  fun versionsAreComparedLikeGradle() {
    assertThat(compareVersions("7.2.2", "8.1.0-SNAPSHOT")).isLessThan(0)
    assertThat(compareVersions("8.0.0-alpha01", "8.0.0")).isLessThan(0)
    assertThat(compareVersions("8.0.0-alpha01", "8.0.0-alpha02")).isLessThan(0)
    assertThat(compareVersions("8.0.0-alpha02", "8.0.0-beta01")).isLessThan(0)
    assertThat(compareVersions("8.1.0-SNAPSHOT", "8.1.0")).isLessThan(0)
    assertThat(compareVersions("8.0.9", "8.0.10")).isLessThan(0)
    assertThat(compareVersions("8.0", "8.0.0")).isEqualTo(0)
    assertThat(compareVersions("8.1.0-SNAPSHOT", "8.1.0-SNAPSHOT")).isEqualTo(0)
    assertThat(compareVersions("8.1.0", "8.0.0-alpha01")).isEqualTo(1)
    assertThat(compareVersions("99.0.0", "8.1.0-SNAPSHOT")).isEqualTo(1)
  }
}
