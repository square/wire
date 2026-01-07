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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.Moshi
import com.squareup.wire.protos.redacted.NotRedacted
import com.squareup.wire.protos.redacted.RedactedChild
import com.squareup.wire.protos.redacted.RedactedFields
import org.junit.Test

class MoshiRedactedTest {
  @Test fun nonRedacted() {
    val redacted = RedactedFields.Builder().a("a").b("b").c("c").build()
    val jsonAdapter = moshi.adapter(RedactedFields::class.java)
    assertThat(jsonAdapter.toJson(redacted)).isEqualTo("""{"a":"a","b":"b","c":"c"}""")
  }

  @Test fun redacted() {
    val redacted = RedactedFields.Builder().a("a").b("b").c("c").build()
    val jsonAdapter = moshi.adapter(RedactedFields::class.java).redacting()
    assertThat(jsonAdapter.toJson(redacted))
      .isEqualTo("""{"b":"b","c":"c","__redacted_fields":["a"]}""")
  }

  @Test fun redactedButSkipped() {
    val redacted = RedactedFields.Builder().b("b").c("c").build()
    val jsonAdapter = moshi.adapter(RedactedFields::class.java).redacting()
    assertThat(jsonAdapter.toJson(redacted)).isEqualTo("""{"b":"b","c":"c"}""")
  }

  @Test fun redactedChild() {
    val redacted = RedactedChild.Builder()
      .a("a")
      .b(
        RedactedFields.Builder()
          .a("a")
          .b("b")
          .c("c")
          .build(),
      ).c(
        NotRedacted.Builder()
          .a("a")
          .b("b")
          .build(),
      ).build()

    val jsonAdapter = moshi.adapter(RedactedChild::class.java).redacting()
    assertThat(jsonAdapter.toJson(redacted)).isEqualTo(
      """{"a":"a","b":{"b":"b","c":"c","__redacted_fields":["a"]},"c":{"a":"a","b":"b"}}""",
    )
  }

  companion object {
    private val moshi = Moshi.Builder()
      .add(WireJsonAdapterFactory())
      .build()
  }
}
