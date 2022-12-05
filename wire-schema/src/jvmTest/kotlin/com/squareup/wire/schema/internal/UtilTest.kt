/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.schema.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UtilTest {
  @Test
  fun indentationTest() {
    val input = """
        |Foo
        |Bar
        |Baz""".trimMargin()
    val expected = """
        |  Foo
        |  Bar
        |  Baz
        |""".trimMargin()
    val actual = buildString {
      appendIndented(input)
    }
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun documentationTest() {
    val input = "Foo\nBar\nBaz"
    val expected = """
        |// Foo
        |// Bar
        |// Baz
        |""".trimMargin()
    val actual = buildString {
      appendDocumentation(input)
    }
    assertThat(actual).isEqualTo(expected)
  }
}
