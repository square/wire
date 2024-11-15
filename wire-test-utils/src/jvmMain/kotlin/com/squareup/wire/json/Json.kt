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
@file:JvmName("JsonUtils")

package com.squareup.wire.json

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.JsonReader
import okio.Buffer

fun assertJsonEquals(expected: String, value: String) {
  assertThat(JsonReader.of(Buffer().writeUtf8(value)).readJsonValue())
    .isEqualTo(JsonReader.of(Buffer().writeUtf8(expected)).readJsonValue())
}
