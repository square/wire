/*
 * Copyright (C) 2014 Square, Inc.
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
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.MAX_TAG_VALUE
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ExtensionsElementTest {
  internal var location = Location.get("file.proto")

  @Test
  fun singleValueToSchema() {
    val actual = ExtensionsElement(
        location = location,
        values = listOf(500)
    )
    val expected = "extensions 500;\n"
    assertThat(actual.toSchema()).isEqualTo(expected)
  }

  @Test
  fun rangeToSchema() {
    val actual = ExtensionsElement(
        location = location,
        values = listOf(500..505)
    )
    val expected = "extensions 500 to 505;\n"
    assertThat(actual.toSchema()).isEqualTo(expected)
  }

  @Test
  fun maxRangeToSchema() {
    val actual = ExtensionsElement(
        location = location,
        values = listOf(500..MAX_TAG_VALUE)
    )
    val expected = "extensions 500 to max;\n"
    assertThat(actual.toSchema()).isEqualTo(expected)
  }

  @Test
  fun withDocumentationToSchema() {
    val actual = ExtensionsElement(
        location = location,
        documentation =  "Hello",
        values = listOf(500)
    )
    val expected = """
        |// Hello
        |extensions 500;
        |""".trimMargin()
    assertThat(actual.toSchema()).isEqualTo(expected)
  }
}
