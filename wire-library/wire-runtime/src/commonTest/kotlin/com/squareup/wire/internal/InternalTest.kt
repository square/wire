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

  @Test fun camelCase() {
    assertEquals("", camelCase(""))
    assertEquals("", camelCase("_"))
    assertEquals("", camelCase("__"))
    assertEquals("aBC", camelCase("a_b_c"))
    assertEquals("aBC", camelCase("a_b_c_"))
    assertEquals("aBC", camelCase("ABC"))
    assertEquals("aBC", camelCase("A_B_C"))
    assertEquals("aBC", camelCase("A__B__C"))
    assertEquals("aBC", camelCase("A__B__C__"))
    assertEquals("ABC", camelCase("__A__B__C__"))
    assertEquals("helloWorld", camelCase("HelloWorld"))
    assertEquals("helloWorld", camelCase("helloWorld"))
    assertEquals("helloWorld", camelCase("hello_world"))
    assertEquals("HelloWorld", camelCase("_hello_world"))
    assertEquals("HelloWorld", camelCase("_hello_world_"))
    assertEquals("🦕", camelCase("🦕"))
    assertEquals("hello🦕world", camelCase("hello_🦕world"))
    assertEquals("hello🦕World", camelCase("hello_🦕_world"))
  }
}
