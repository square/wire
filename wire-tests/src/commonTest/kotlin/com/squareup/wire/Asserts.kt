/*
 * Copyright 2019 Square Inc.
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

import kotlin.math.abs
import kotlin.test.fail

fun assertFloatEquals(expected: Float, actual: Float, delta: Float = 0.01f) {
  require(delta >= 0) { "delta can't be negative, was $delta." }
  val diff = abs(expected - actual)
  if (diff > delta) {
    fail("Expected difference between <$expected> and <$actual> to not be greater than <$delta>," +
        " but was <$diff>.")
  }
}

fun assertArrayEquals(expected: ByteArray, actual: ByteArray) {
  if (expected === actual) return
  if (actual.size != expected.size) {
    fail("Expected array of length <${expected.size}> but was <${actual.size}>.")
  }
  for (i in expected.indices) {
    if (actual[i] != expected[i]) {
      fail("Expected element at position <$i> to be <${expected[i]}> but was <${actual[i]}>.")
    }
  }
}

fun assertArrayNotEquals(expected: ByteArray, actual: ByteArray) {
  if (expected === actual) {
    fail("Expected $actual to not be equal to $expected.")
  }
  if (actual.size != expected.size) return
  for (i in expected.indices) {
    if (actual[i] != expected[i]) return
  }
  fail("Expected ${actual.contentToString()} to not be equal to ${expected.contentToString()}.")
}
