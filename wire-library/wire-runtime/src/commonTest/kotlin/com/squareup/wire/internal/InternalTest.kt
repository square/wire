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
}
