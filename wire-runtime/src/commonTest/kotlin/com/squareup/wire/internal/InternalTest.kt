/*
 * Copyright (C) 2023 Square, Inc.
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
package com.squareup.wire.internal

import com.squareup.wire.VERSION
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InternalTest {
  @Test fun countNonNull() {
    assertEquals(0, countNonNull(null, null))
    assertEquals(1, countNonNull("xx", null))
    assertEquals(2, countNonNull("xx", "xx"))
    assertEquals(2, countNonNull("xx", "xx", null))
    assertEquals(3, countNonNull("xx", "xx", "xx"))
    assertEquals(3, countNonNull("xx", "xx", "xx", null))
    assertEquals(4, countNonNull("xx", "xx", "xx", "xx"))
    assertEquals(4, countNonNull("xx", "xx", "xx", "xx", null))
    assertEquals(5, countNonNull("xx", "xx", "xx", "xx", "xx"))
  }

  @Test fun sanitizeStrings() {
    assertEquals("""\,""", sanitize(""","""))
    assertEquals("""\{""", sanitize("""{"""))
    assertEquals("""\}""", sanitize("""}"""))
    assertEquals("""\[""", sanitize("""["""))
    assertEquals("""\]""", sanitize("""]"""))
    assertEquals("""\\""", sanitize("""\"""))
    assertEquals("""Hi\, I'm \{CURRENT_HOST\} dax!""", sanitize("""Hi, I'm {CURRENT_HOST} dax!"""))

    assertEquals(
      """[\,, \{, \}, \[, \], \\]""",
      sanitize(listOf(""",""", """{""", """}""", """[""", """]""", """\""")),
    )
  }

  @Test fun lowerCamelCase() {
    assertEquals("", camelCase(""))
    assertEquals("", camelCase("_"))
    assertEquals("", camelCase("__"))
    assertEquals("aBC", camelCase("a_b_c"))
    assertEquals("aBC", camelCase("a_b_c_"))
    assertEquals("ABC", camelCase("_a_b_c_"))
    assertEquals("ABC", camelCase("ABC"))
    assertEquals("ABC", camelCase("A_B_C"))
    assertEquals("ABC", camelCase("A__B__C"))
    assertEquals("ABC", camelCase("A__B__C__"))
    assertEquals("ABC", camelCase("__A__B__C__"))
    assertEquals("HelloWorld", camelCase("HelloWorld"))
    assertEquals("helloWorld", camelCase("helloWorld"))
    assertEquals("helloWorld", camelCase("hello_world"))
    assertEquals("HelloWorld", camelCase("_hello_world"))
    assertEquals("HelloWorld", camelCase("_hello_world_"))
    assertEquals("🦕", camelCase("🦕"))
    assertEquals("hello🦕world", camelCase("hello_🦕world"))
    assertEquals("hello🦕World", camelCase("hello_🦕_world"))
  }

  @Test fun upperCamelCase() {
    assertEquals("", camelCase("", upperCamel = true))
    assertEquals("", camelCase("_", upperCamel = true))
    assertEquals("", camelCase("__", upperCamel = true))
    assertEquals("ABC", camelCase("a_b_c", upperCamel = true))
    assertEquals("ABC", camelCase("a_b_c_", upperCamel = true))
    assertEquals("ABC", camelCase("ABC", upperCamel = true))
    assertEquals("ABC", camelCase("A_B_C", upperCamel = true))
    assertEquals("ABC", camelCase("A__B__C", upperCamel = true))
    assertEquals("ABC", camelCase("A__B__C__", upperCamel = true))
    assertEquals("ABC", camelCase("__A__B__C__", upperCamel = true))
    assertEquals("HelloWorld", camelCase("HelloWorld", upperCamel = true))
    assertEquals("HelloWorld", camelCase("helloWorld", upperCamel = true))
    assertEquals("HelloWorld", camelCase("hello_world", upperCamel = true))
    assertEquals("HelloWorld", camelCase("_hello_world", upperCamel = true))
    assertEquals("HelloWorld", camelCase("_hello_world_", upperCamel = true))
    assertEquals("🦕", camelCase("🦕", upperCamel = true))
    assertEquals("Hello🦕world", camelCase("hello_🦕world", upperCamel = true))
    assertEquals("Hello🦕World", camelCase("hello_🦕_world", upperCamel = true))
  }

  @Test fun versionIsExposed() {
    assertNotNull(VERSION)
  }
}
