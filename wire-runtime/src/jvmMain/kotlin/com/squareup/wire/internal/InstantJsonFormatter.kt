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
package com.squareup.wire.internal

import com.squareup.wire.Instant
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

/**
 * Encode an instant as a JSON string like "1950-01-01T00:00:00Z". From the spec:
 *
 * > Uses RFC 3339, where generated output will always be Z-normalized and uses 0, 3, 6 or 9
 * > fractional digits. Offsets other than "Z" are also accepted.
 */
object InstantJsonFormatter : JsonFormatter<Instant> {
  override fun toStringOrNumber(value: Instant): Any {
    return ISO_INSTANT.format(value)
  }

  override fun fromString(value: String): Instant {
    val parsed = ISO_OFFSET_DATE_TIME.parse(value)
    return Instant.from(parsed)
  }
}
