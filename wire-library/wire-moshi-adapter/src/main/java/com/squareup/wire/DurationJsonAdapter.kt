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

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Encode a duration as a JSON string like "1.200s". From the spec:
 *
 * > Generated output always contains 0, 3, 6, or 9 fractional digits, depending on required
 * > precision, followed by the suffix "s". Accepted are any fractional digits (also none) as long
 * > as they fit into nano-seconds precision and the suffix "s" is required.
 *
 * Note that [Duration] always returns a positive nanosPart, so "-1.200s" is represented as -2
 * seconds and 800_000_000 nanoseconds.
 */
internal object DurationJsonAdapter : JsonAdapter<Duration>() {
  override fun toJson(out: JsonWriter, duration: Duration?) {
    out.value(durationToString(duration!!))
  }

  override fun fromJson(input: JsonReader): Duration? {
    val string = input.nextString()
    try {
      return stringToDuration(string)
    } catch (_: NumberFormatException) {
      throw JsonDataException("not a duration: $string at path ${input.path}")
    }
  }

  internal fun durationToString(duration: Duration): String {
    var seconds = duration.seconds
    var nanos = duration.nano
    var prefix = ""
    if (seconds < 0L) {
      if (seconds == Long.MIN_VALUE) {
        prefix = "-922337203685477580" // Avoid overflow inverting MIN_VALUE.
        seconds = 8
      } else {
        prefix = "-"
        seconds = -seconds
      }
      if (nanos != 0) {
        seconds -= 1L
        nanos = 1_000_000_000 - nanos
      }
    }
    return when {
      nanos == 0 -> "%s%ds".format(prefix, seconds)
      nanos % 1_000_000 == 0 -> "%s%d.%03ds".format(prefix, seconds, nanos / 1_000_000L)
      nanos % 1_000 == 0 -> "%s%d.%06ds".format(prefix, seconds, nanos / 1_000L)
      else -> "%s%d.%09ds".format(prefix, seconds, nanos / 1L)
    }
  }

  /** Throws a NumberFormatException if the string isn't a number like "1s" or "1.23456789s". */
  internal fun stringToDuration(string: String): Duration {
    val sIndex = string.indexOf('s')
    if (sIndex != string.length - 1) throw NumberFormatException()

    val dotIndex = string.indexOf('.')
    if (dotIndex == -1) {
      val seconds = string.substring(0, sIndex).toLong()
      return Duration.ofSeconds(seconds)
    }

    val seconds = string.substring(0, dotIndex).toLong()
    var nanos = string.substring(dotIndex + 1, sIndex).toLong()
    if (string.startsWith("-")) nanos = -nanos
    val nanosDigits = sIndex - (dotIndex + 1)
    for (i in nanosDigits until 9) nanos *= 10
    for (i in 9 until nanosDigits) nanos /= 10
    return Duration.ofSeconds(seconds, nanos)
  }
}
