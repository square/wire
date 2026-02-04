/*
 * Copyright (C) 2026 Square, Inc.
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
import com.squareup.wire.protos.kotlin.large_field.LargeFieldMessage
import kotlin.test.Test

class LargeFieldNumberTest {

  private val adapter = LargeFieldMessage.ADAPTER

  @Test fun encodeDecode_largeFieldNumbers() {
    val original = LargeFieldMessage(
      a = "123",
      b = "456",
      large_string_field = "wire",
      large_int_field = 123456,
    )

    val encoded = adapter.encode(original)
    val decoded = adapter.decode(encoded)

    assertThat(decoded).isEqualTo(original)
  }
}
