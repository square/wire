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

import com.squareup.wire.internal.NANOS_PER_SECOND
import com.squareup.wire.internal.addExactLong
import com.squareup.wire.internal.floorDivLong
import com.squareup.wire.internal.floorModLong

actual class Duration internal constructor(
  private val seconds: Long,
  private val nanos: Int
) {
  actual fun getSeconds(): Long = seconds
  actual fun getNano(): Int = nanos
}

actual fun durationOfSeconds(
  seconds: Long,
  nanoAdjustment: Long
): Duration {
  val secs = addExactLong(seconds, floorDivLong(nanoAdjustment, NANOS_PER_SECOND))
  val nos = floorModLong(nanoAdjustment, NANOS_PER_SECOND).toInt()
  return Duration(secs, nos)
}
