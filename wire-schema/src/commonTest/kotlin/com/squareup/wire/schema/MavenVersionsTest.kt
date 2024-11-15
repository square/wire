/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.squareup.wire.schema

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.message
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.message
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.message
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import kotlin.test.Test

/**
 * This is Maven's `ComparableVersionTest` ported to our [SemVer] class and with several tests
 * omitted or commented out because of different behavior.
 *
 * https://github.com/apache/maven/blob/master/maven-artifact/src/test/java/org/apache/maven/artifact/versioning/ComparableVersionTest.java
 */
internal class MavenVersionsTest {
  /** Commented out tests are excluded because they disagree with the semver.org spec! */
  @Test
  fun testVersionComparing() {
    checkVersionsOrder("1", "2")
    checkVersionsOrder("1.5", "2")
    checkVersionsOrder("1", "2.5")
    checkVersionsOrder("1.0", "1.1")
    checkVersionsOrder("1.1", "1.2")
    checkVersionsOrder("1.0.0", "1.1")
    checkVersionsOrder("1.0.1", "1.1")
    checkVersionsOrder("1.1", "1.2.0")

    checkVersionsOrder("1.0-alpha-1", "1.0")
    checkVersionsOrder("1.0-alpha-1", "1.0-alpha-2")
    checkVersionsOrder("1.0-alpha-1", "1.0-beta-1")

    checkVersionsOrder("1.0-beta-1", "1.0-SNAPSHOT")
    checkVersionsOrder("1.0-SNAPSHOT", "1.0")
    // checkVersionsOrder("1.0-alpha-1-SNAPSHOT", "1.0-alpha-1")

    // checkVersionsOrder("1.0", "1.0-1")
    checkVersionsOrder("1.0-1", "1.0-2")
    // checkVersionsOrder("1.0.0", "1.0-1")

    checkVersionsOrder("2.0-1", "2.0.1")
    checkVersionsOrder("2.0.1-klm", "2.0.1-lmn")
    // checkVersionsOrder("2.0.1", "2.0.1-xyz")

    // checkVersionsOrder("2.0.1", "2.0.1-123")
    // checkVersionsOrder("2.0.1-xyz", "2.0.1-123")
  }

  /** Wire's version maintains a total ordering but the ordering is different from Maven's! */
  @Test
  fun testMng5568() {
    val a = "6.1.0"
    val b = "6.1.0rc3"
    val c = "6.1H.5-beta"
    checkVersionsOrder(a, b)
    checkVersionsOrder(b, c)
    checkVersionsOrder(a, c)
  }

  /** Wire's version maintains a total ordering but the ordering is different from Maven's! */
  @Test
  fun testMng6572() {
    val a = "20190126.230843"
    val b = "1234567890.12345"
    val c = "123456789012345.1H.5-beta"
    val d = "12345678901234567890.1H.5-beta"
    checkVersionsOrder(a, b)
    checkVersionsOrder(b, c)
    checkVersionsOrder(a, c)
    checkVersionsOrder(c, d)
    checkVersionsOrder(b, d)
    checkVersionsOrder(a, d)
  }

  @Test
  fun testVersionEqualWithLeadingZeroes() {
    assertThat(SemVer("0000000000000000001").compareTo(SemVer("1"))).isEqualTo(0)
  }

  @Test
  fun testVersionZeroEqualWithLeadingZeroes() {
    assertThat(SemVer("0000000000000000000").compareTo(SemVer("0"))).isEqualTo(0)
  }

  private fun checkVersionsOrder(aString: String, bString: String) {
    val a = SemVer(aString.lowercase())
    val b = SemVer(bString.lowercase())
    assertThat(a.compareTo(a), displayActual = { "$a == $a" }).isEqualTo(0)
    assertThat(b.compareTo(b), displayActual = { "$b == $b" }).isEqualTo(0)
    assertThat(a.compareTo(b), displayActual = { "$a > $b" }).isLessThan(0)
    assertThat(b.compareTo(a), displayActual = { "$b < $a" }).isGreaterThan(0)
  }
}
