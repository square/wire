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

import com.squareup.wire.FieldEncoding.LENGTH_DELIMITED
import java.time.Duration

internal object DurationProtoAdapter : ProtoAdapter<Duration>(
    fieldEncoding = LENGTH_DELIMITED,
    type = Duration::class,
    typeUrl = "type.googleapis.com/google.protobuf.Duration"
) {
  override fun encodedSize(value: Duration): Int {
    var result = 0
    val seconds = value.sameSignSeconds
    if (seconds != 0L) result += INT64.encodedSizeWithTag(1, seconds)
    val nanos = value.sameSignNanos
    if (nanos != 0) result += INT32.encodedSizeWithTag(2, nanos)
    return result
  }

  override fun encode(writer: ProtoWriter, value: Duration) {
    val seconds = value.sameSignSeconds
    if (seconds != 0L) INT64.encodeWithTag(writer, 1, seconds)
    val nanos = value.sameSignNanos
    if (nanos != 0) INT32.encodeWithTag(writer, 2, nanos)
  }

  override fun decode(reader: ProtoReader): Duration {
    var seconds = 0L
    var nanos = 0
    reader.forEachTag { tag ->
      when (tag) {
        1 -> seconds = INT64.decode(reader)
        2 -> nanos = INT32.decode(reader)
        else -> reader.readUnknownField(tag)
      }
    }
    return Duration.ofSeconds(seconds, nanos.toLong())
  }

  override fun redact(value: Duration): Duration = value

  /**
   * Returns a value like 1 for 1.200s and -1 for -1.200s. This is different from the Duration
   * seconds field which is always the integer floor when seconds is negative.
   */
  private val Duration.sameSignSeconds: Long
    get() {
      return when {
        seconds < 0L && nano != 0 -> seconds + 1L
        else -> seconds
      }
    }

  /**
   * Returns a value like 200_000_000 for 1.200s and -200_000_000 for -1.200s. This is different
   * from the Duration nanos field which can be positive when seconds is negative.
   */
  private val Duration.sameSignNanos: Int
    get() {
      return when {
        seconds < 0L && nano != 0 -> nano - 1_000_000_000
        else -> nano
      }
    }
}
