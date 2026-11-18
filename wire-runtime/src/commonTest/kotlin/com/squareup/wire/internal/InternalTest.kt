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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.squareup.wire.wireVersion
import kotlin.test.Test

class InternalTest {
  @Test fun countNonNull() {
    assertThat(countNonNull(null, null)).isEqualTo(0)
    assertThat(countNonNull("xx", null)).isEqualTo(1)
    assertThat(countNonNull("xx", "xx")).isEqualTo(2)
    assertThat(countNonNull("xx", "xx", null)).isEqualTo(2)
    assertThat(countNonNull("xx", "xx", "xx")).isEqualTo(3)
    assertThat(countNonNull("xx", "xx", "xx", null)).isEqualTo(3)
    assertThat(countNonNull("xx", "xx", "xx", "xx")).isEqualTo(4)
    assertThat(countNonNull("xx", "xx", "xx", "xx", null)).isEqualTo(4)
    assertThat(countNonNull("xx", "xx", "xx", "xx", "xx")).isEqualTo(5)
  }

  @Test fun sanitizeStrings() {
    assertThat(sanitize(""",""")).isEqualTo("""\,""")
    assertThat(sanitize("""{""")).isEqualTo("""\{""")
    assertThat(sanitize("""}""")).isEqualTo("""\}""")
    assertThat(sanitize("""[""")).isEqualTo("""\[""")
    assertThat(sanitize("""]""")).isEqualTo("""\]""")
    assertThat(sanitize("""\""")).isEqualTo("""\\""")
    assertThat(sanitize("""Hi, I'm {CURRENT_HOST} dax!""")).isEqualTo("""Hi\, I'm \{CURRENT_HOST\} dax!""")

    assertThat(
      sanitize(
        listOf(
          """,""",
          """{""",
          """}""",
          """[""",
          """]""",
          """\""",
        ),
      ),
    ).isEqualTo("""[\,, \{, \}, \[, \], \\]""")
  }

  @Test fun sanitizeEscapesControlCharacters() {
    assertThat(sanitize("a\nb")).isEqualTo("""a\nb""")
    assertThat(sanitize("a\r\nb")).isEqualTo("""a\r\nb""")
    assertThat(sanitize("a\tb")).isEqualTo("""a\tb""")
    assertThat(sanitize("\u001b[31mred")).isEqualTo("""\u001b\[31mred""")
    assertThat(sanitize("\u0000\u0001\u001f\u007f\u0085\u009b")).isEqualTo("""\u0000\u0001\u001f\u007f\u0085\u009b""")
    // A literal backslash followed by `n` stays distinct from a newline.
    assertThat(sanitize("""a\nb""")).isEqualTo("""a\\nb""")
    assertThat(sanitize(" ~é🦕")).isEqualTo(" ~é🦕")
    assertThat(sanitize(listOf("a\nb", "c"))).isEqualTo("""[a\nb, c]""")
  }

  @Test fun sanitizeMaps() {
    assertThat(sanitize(mapOf("k1, forged=1" to "v}\nforged"))).isEqualTo("""{k1\, forged=1=v\}\nforged}""")
    assertThat(sanitize(mapOf(1 to "a\nb", 2 to 3))).isEqualTo("""{1=a\nb, 2=3}""")
    assertThat(sanitize(emptyMap<String, String>())).isEqualTo("{}")
  }

  @Test fun lowerCamelCase() {
    assertThat(camelCase("")).isEqualTo("")
    assertThat(camelCase("_")).isEqualTo("")
    assertThat(camelCase("__")).isEqualTo("")
    assertThat(camelCase("a_b_c")).isEqualTo("aBC")
    assertThat(camelCase("a_b_c_")).isEqualTo("aBC")
    assertThat(camelCase("_a_b_c_")).isEqualTo("ABC")
    assertThat(camelCase("ABC")).isEqualTo("ABC")
    assertThat(camelCase("A_B_C")).isEqualTo("ABC")
    assertThat(camelCase("A__B__C")).isEqualTo("ABC")
    assertThat(camelCase("A__B__C__")).isEqualTo("ABC")
    assertThat(camelCase("__A__B__C__")).isEqualTo("ABC")
    assertThat(camelCase("HelloWorld")).isEqualTo("HelloWorld")
    assertThat(camelCase("helloWorld")).isEqualTo("helloWorld")
    assertThat(camelCase("hello_world")).isEqualTo("helloWorld")
    assertThat(camelCase("_hello_world")).isEqualTo("HelloWorld")
    assertThat(camelCase("_hello_world_")).isEqualTo("HelloWorld")
    assertThat(camelCase("🦕")).isEqualTo("🦕")
    assertThat(camelCase("hello_🦕world")).isEqualTo("hello🦕world")
    assertThat(camelCase("hello_🦕_world")).isEqualTo("hello🦕World")
  }

  @Test fun upperCamelCase() {
    assertThat(camelCase("", upperCamel = true)).isEqualTo("")
    assertThat(camelCase("_", upperCamel = true)).isEqualTo("")
    assertThat(camelCase("__", upperCamel = true)).isEqualTo("")
    assertThat(camelCase("a_b_c", upperCamel = true)).isEqualTo("ABC")
    assertThat(camelCase("a_b_c_", upperCamel = true)).isEqualTo("ABC")
    assertThat(camelCase("ABC", upperCamel = true)).isEqualTo("ABC")
    assertThat(camelCase("A_B_C", upperCamel = true)).isEqualTo("ABC")
    assertThat(camelCase("A__B__C", upperCamel = true)).isEqualTo("ABC")
    assertThat(camelCase("A__B__C__", upperCamel = true)).isEqualTo("ABC")
    assertThat(camelCase("__A__B__C__", upperCamel = true)).isEqualTo("ABC")
    assertThat(camelCase("HelloWorld", upperCamel = true)).isEqualTo("HelloWorld")
    assertThat(camelCase("helloWorld", upperCamel = true)).isEqualTo("HelloWorld")
    assertThat(camelCase("hello_world", upperCamel = true)).isEqualTo("HelloWorld")
    assertThat(camelCase("_hello_world", upperCamel = true)).isEqualTo("HelloWorld")
    assertThat(camelCase("_hello_world_", upperCamel = true)).isEqualTo("HelloWorld")
    assertThat(camelCase("🦕", upperCamel = true)).isEqualTo("🦕")
    assertThat(camelCase("hello_🦕world", upperCamel = true)).isEqualTo("Hello🦕world")
    assertThat(camelCase("hello_🦕_world", upperCamel = true)).isEqualTo("Hello🦕World")
  }

  @Test fun versionIsExposed() {
    assertThat(wireVersion).isNotNull()
  }
}
