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

import com.squareup.wire.protos.kotlin.serialization.cdn.CdnResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8

class KotlinSerializationTest {
  @Test
  fun encodeDecodeJson() {
    val expectedValue = CdnResource(
        url = "https://cdn.cashapp.com/res/a2a5d5f35673ec00b822d153b8b69c49",
        byte_count = 8589934593L,
        content_type = CdnResource.ContentType.IMAGE,
        data_ = "hello".encodeUtf8()
    )
    val expectedJson = """
        |{
        |  "url": "https://cdn.cashapp.com/res/a2a5d5f35673ec00b822d153b8b69c49",
        |  "byteCount": 8589934593,
        |  "contentType": "IMAGE",
        |  "data": "aGVsbG8="
        |}""".trimMargin().replace(Regex("[\n ]"), "")

    val actualJson = Json.encodeToString(expectedValue)
    val actualValue = Json.decodeFromString<CdnResource>(expectedJson)
    assertEquals(expectedJson, actualJson)
    assertEquals(expectedValue, actualValue)
  }
}
