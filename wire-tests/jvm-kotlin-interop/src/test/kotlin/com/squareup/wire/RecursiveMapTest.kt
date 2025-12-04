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
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire

import ModelEvaluation
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class RecursiveMapTest {
  @Test
  fun createUseBuilderWithoutCrashing() {
    val model = ModelEvaluation.Builder()
      .name("name")
      .score(33.0)
      .models(mapOf())
      .build()
    assertThat(ModelEvaluation.ADAPTER.encodeByteString(model).hex())
      .isEqualTo("0a046e616d65110000000000804040")
  }
}
