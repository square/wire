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

import com.squareup.wire.protos.kotlin.redacted_map.Secret
import com.squareup.wire.protos.kotlin.redacted_map.SecretCollections
import kotlin.test.Test
import kotlin.test.assertEquals

class ReflectiveRedactTest {
  private val adapter = ProtoAdapter.newMessageAdapter(SecretCollections::class.java)

  @Test
  fun redactsNestedMessagesListsAndMapValues() {
    val secret = Secret(a = "secret", b = "keep")
    val message = SecretCollections(
      one = secret,
      many = listOf(secret),
      by_key = mapOf("k" to secret),
      name = "name",
    )

    val kept = Secret(b = "keep")
    val expected = SecretCollections(
      one = kept,
      many = listOf(kept),
      by_key = mapOf("k" to kept),
      name = "name",
    )
    assertEquals(expected, adapter.redact(message))
    assertEquals(expected, SecretCollections.ADAPTER.redact(message))
  }
}
