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

/**
 * A measurement of time. Durations may be positive, zero, or negative.
 *
 * Positive durations are measured in seconds, with both [#getSeconds] and [#getNano] parts with
 * non-negative signs.
 *
 * Negative durations may be surprising. The [#getSeconds] property is negative, but the [#getNano]
 * property is non-negative! To represent -1.3 seconds the seconds property is -2 and the nanos
 * property is 700,000,000.
 */
expect class Duration {
  fun getSeconds(): Long

  /** Returns a value in `[0..1,000,000,000)`. */
  fun getNano(): Int
}

expect fun durationOfSeconds(seconds: Long, nano: Long): Duration
