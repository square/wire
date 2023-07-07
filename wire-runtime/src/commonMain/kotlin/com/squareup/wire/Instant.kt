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
 * This represents a timestamp, though we use the name 'instant' in the runtime because that's what
 * the JVM libraries use, and 'Timestamp' means something else on that platform.
 */
expect class Instant {

  /**
   * Returns the number of seconds since the UNIX epoch (1970-01-01T00:00:00Z) if this value is
   * positive, or until the UNIX epoch if this value is negative.
   *
   * For example, this value will be -1 for the instant 1969-12-31T23:59:59Z, and 1 for the instant
   * 1970-01-01T00:00:01Z.
   */
  fun getEpochSecond(): Long

  /**
   * Returns a value in the range `[0..1,000,000,000)` indicating the fraction of a second that is
   * added to [getEpochSecond].
   */
  fun getNano(): Int
}

expect fun ofEpochSecond(epochSecond: Long, nano: Long): Instant
