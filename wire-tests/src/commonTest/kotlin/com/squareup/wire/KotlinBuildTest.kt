/*
 * Copyright (C) 2018 Square, Inc.
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

import com.squareup.wire.protos.kotlin.simple.buildersonly.SimpleMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinBuildTest {
  @Test fun kotlinBuildEquivalentToBuilderResult() {
    val bb = 100

    val kotlinBuildResult = SimpleMessage.build {
      required_int32 = 4
      optional_int32 = 5
      optional_nested_msg = SimpleMessage.NestedMessage.build {
        this.bb = bb
      }
    }
    val builderResult = SimpleMessage.Builder()
      .required_int32(4)
      .optional_int32(5)
      .optional_nested_msg(
        SimpleMessage.NestedMessage.Builder()
          .bb(bb)
          .build(),
      ).build()

    assertEquals(kotlinBuildResult, builderResult)
  }
}
