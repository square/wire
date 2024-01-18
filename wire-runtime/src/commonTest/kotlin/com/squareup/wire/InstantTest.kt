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

import kotlin.test.Test
import kotlin.test.assertEquals

class InstantTest {
  @Test fun positiveValues() {
    val wireMessage = ofEpochSecond(1L, 200_000_000L)
    assertEquals(1L, wireMessage.getEpochSecond())
    assertEquals(200_000_000, wireMessage.getNano())
  }

  @Test fun zero() {
    val wireMessage = ofEpochSecond(0L, 0L)
    assertEquals(0L, wireMessage.getEpochSecond())
    assertEquals(0, wireMessage.getNano())
  }

  @Test fun negativeNearZero() {
    val wireMessage = ofEpochSecond(0L, -200_000_000L)
    assertEquals(-1L, wireMessage.getEpochSecond())
    assertEquals(800_000_000, wireMessage.getNano())
  }

  @Test fun negativeValues() {
    val wireMessage = ofEpochSecond(-1L, -200_000_000L)
    assertEquals(-2L, wireMessage.getEpochSecond())
    assertEquals(800_000_000, wireMessage.getNano())
  }

  @Test fun equality() {
    assertEquals(ofEpochSecond(0L, 0L), ofEpochSecond(0L, 0L))
  }
}
