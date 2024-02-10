/*
 * Copyright (C) 2024 Square, Inc.
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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.squareup.wire.json.assertJsonEquals
import org.junit.Test
import squareup.proto3.AllStructs

class GsonAllStructsTest {

  @Test
  fun kotlinFullObject() {
    val json =
      """
      |{
      |  "list": ["a1", "a2"],
      |  "mapInt32List": {"2":["c1", "c2"]},
      |  "mapInt32NullValue": {"1":null},
      |  "mapInt32Struct": {"1":{"key3":"value3"}},
      |  "mapInt32ValueA": {"1":{"key4":"value4"}},
      |  "oneofStruct": {"key2":"value2"},
      |  "struct": {"key1":"value1"}
      |}
      """.trimMargin()
    val allStruct = gson.fromJson(json, AllStructs::class.java)
    assertJsonEquals(json, gson.toJson(allStruct))
  }

  companion object {
    private val gson: Gson = GsonBuilder()
      .registerTypeAdapterFactory(WireTypeAdapterFactory())
      .create()
  }
}
