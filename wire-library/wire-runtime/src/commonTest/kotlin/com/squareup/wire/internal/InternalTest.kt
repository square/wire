package com.squareup.wire.internal

import kotlin.test.Test
import kotlin.test.assertEquals

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

    assertEquals("""[\,, \{, \}, \[, \], \\]""",
        sanitize(listOf(""",""", """{""", """}""", """[""", """]""", """\""")))
  }

  @Test fun lowerCamelCase() {
    assertEquals("", camelCase("", false))
    assertEquals("", camelCase("_", false))
    assertEquals("", camelCase("__", false))
    assertEquals("aBC", camelCase("a_b_c", false))
    assertEquals("aBC", camelCase("a_b_c_", false))
    assertEquals("aBC", camelCase("ABC", false))
    assertEquals("aBC", camelCase("A_B_C", false))
    assertEquals("aBC", camelCase("A__B__C", false))
    assertEquals("aBC", camelCase("A__B__C__", false))
    assertEquals("ABC", camelCase("__A__B__C__", false))
    assertEquals("helloWorld", camelCase("HelloWorld", false))
    assertEquals("helloWorld", camelCase("helloWorld", false))
    assertEquals("helloWorld", camelCase("hello_world", false))
    assertEquals("HelloWorld", camelCase("_hello_world", false))
    assertEquals("HelloWorld", camelCase("_hello_world_", false))
    assertEquals("🦕", camelCase("🦕", false))
    assertEquals("hello🦕world", camelCase("hello_🦕world", false))
    assertEquals("hello🦕World", camelCase("hello_🦕_world", false))
  }

  @Test fun upperCamelCase() {
    assertEquals("", camelCase("", true))
    assertEquals("", camelCase("_", true))
    assertEquals("", camelCase("__", true))
    assertEquals("ABC", camelCase("a_b_c", true))
    assertEquals("ABC", camelCase("a_b_c_", true))
    assertEquals("ABC", camelCase("ABC", true))
    assertEquals("ABC", camelCase("A_B_C", true))
    assertEquals("ABC", camelCase("A__B__C", true))
    assertEquals("ABC", camelCase("A__B__C__", true))
    assertEquals("ABC", camelCase("__A__B__C__", true))
    assertEquals("HelloWorld", camelCase("HelloWorld", true))
    assertEquals("HelloWorld", camelCase("helloWorld", true))
    assertEquals("HelloWorld", camelCase("hello_world", true))
    assertEquals("HelloWorld", camelCase("_hello_world", true))
    assertEquals("HelloWorld", camelCase("_hello_world_", true))
    assertEquals("🦕", camelCase("🦕", true))
    assertEquals("Hello🦕world", camelCase("hello_🦕world", true))
    assertEquals("Hello🦕World", camelCase("hello_🦕_world", true))
  }
}
