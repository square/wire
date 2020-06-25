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
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle.STRICT

/**
 * Encode an instant as a JSON string like "1950-01-01T00:00:00Z". From the spec:
 *
 * > Uses RFC 3339, where generated output will always be Z-normalized and uses 0, 3, 6 or 9
 * > fractional digits. Offsets other than "Z" are also accepted.
 */
internal object InstantJsonAdapter : JsonAdapter<Instant>() {
  override fun toJson(out: JsonWriter, instant: Instant?) {
    val string = instantToString(instant)
    out.value(string)
  }

  internal fun instantToString(instant: Instant?) = ISO_INSTANT.format(instant!!)

  override fun fromJson(input: JsonReader): Instant? {
    val string = input.nextString()
    try {
      return stringToInstant(string)
    } catch (_: DateTimeParseException) {
      throw JsonDataException("not an instant: $string at path ${input.path}")
    }
  }

  internal fun stringToInstant(string: String?): java.time.Instant? {
    val parsed = ISO_OFFSET_DATE_TIME.parse(string)
    return Instant.from(parsed)
  }
}
