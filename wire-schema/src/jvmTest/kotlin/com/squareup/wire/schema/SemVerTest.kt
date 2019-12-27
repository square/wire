/*
 * Copyright (C) 2019 Square, Inc.
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
package com.squareup.wire.schema

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test

internal class SemVerTest {
  @Test
  fun majorMinorPatch() {
    assertSorted(
        "1.0.0",
        "1.1.0",
        "1.2.0",
        "1.3.0",
        "1.3.1",
        "1.3.2",
        "1.3.3",
        "2.0.0",
        "3.0.0"
    )
  }

  @Test
  fun numbersAreNumeric() {
    assertSorted(
        "1.0.0",
        "1.5.0",
        "1.10.0",
        "1.15.0",
        "1.105.0",
        "1.500.0",
        "1.505.0",
        "1.1000.0",
        "1.1005.0"
    )
  }

  @Test
  fun stringsAreLexicographic() {
    assertSorted(
        "1.a0.0",
        "1.a10.0",
        "1.a1000.0",
        "1.a1005.0",
        "1.a105.0",
        "1.a15.0",
        "1.a5.0",
        "1.a500.0",
        "1.a505.0"
    )
  }

  @Test
  fun longerIsLater() {
    assertSorted(
        "1.0-a",
        "1.0-a.b",
        "1.0",
        "1.0.a",
        "1.0.a.b",
        "2.0-a",
        "2.0-a.b",
        "2.0",
        "2.0.a",
        "2.0.a.b"
    )
  }

  @Test
  fun alphaBetaRc() {
    assertSorted(
        "1.0.0-alpha",
        "1.0.0-beta",
        "1.0.0-rc-1",
        "1.0.0-rc-2",
        "1.0.0-rc-10",
        "1.0.0-rc-20",
        "1.0.0"
    )
  }

  /**
   * semver.org permits both uppercase and lowercase versions, and sorts them case-sensitively, so
   * 1.0-alpha comes after 1.0-Beta. To avoid this surprising behavior we forbid uppercase
   * characters in version strings.
   */
  @Test
  fun uppercaseVersionsForbidden() {
    try {
      SemVer("1.0.0-A")
      fail()
    } catch (_: IllegalArgumentException) {
    }
  }

  @Test
  fun `semver dot org examples`() {
    assertSorted(
        "1.9.0",
        "1.10.0",
        "1.11.0"
    )
    assertSorted(
        "1.0.0-0.3.7",
        "1.0.0-alpha",
        "1.0.0-alpha.1",
        "1.0.0-x.7.z.92"
    )
    assertSorted(
        "1.0.0-alpha+001",
        "1.0.0-beta+exp.sha.5114f85",
        "1.0.0+20130313144700"
    )
    assertSorted(
        "1.0.0",
        "2.0.0",
        "2.1.0",
        "2.1.1"
    )
    assertSorted(
        "1.0.0-alpha",
        "1.0.0"
    )
    assertSorted(
        "1.0.0-alpha",
        "1.0.0-alpha.1",
        "1.0.0-alpha.beta",
        "1.0.0-beta",
        "1.0.0-beta.2",
        "1.0.0-beta.11",
        "1.0.0-rc.1",
        "1.0.0"
    )
  }

  @Test
  fun wideNumbers() {
    assertSorted(
        "10000000000000000000.0",
        "10000000000000000000.1"
    )
  }

  private fun assertSorted(vararg elements: String) {
    val semVers = elements.map { SemVer(it) }
    for (i in 1 until semVers.size) {
      val a = semVers[i - 1]
      val b = semVers[i]
      assertThat(a.compareTo(a)).overridingErrorMessage("$a == $a").isEqualTo(0)
      assertThat(b.compareTo(b)).overridingErrorMessage("$b == $b").isEqualTo(0)
      assertThat(a.compareTo(b)).overridingErrorMessage("$a > $b").isLessThan(0)
      assertThat(b.compareTo(a)).overridingErrorMessage("$b < $a").isGreaterThan(0)
    }
  }
}
