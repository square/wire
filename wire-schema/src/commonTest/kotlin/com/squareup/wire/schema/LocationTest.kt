package com.squareup.wire.schema

import kotlin.test.Test
import assertk.assertThat
import assertk.assertions.isEqualTo

class LocationTest {

  @Test fun getWithBaseAndRelativePath() {
    val location = Location.get("/base/dir", "test/file.proto")
    assertThat(location.base).isEqualTo("/base/dir")
    assertThat(location.path).isEqualTo("test/file.proto")
  }

  @Test fun getWithWindowsStylePaths() {
    val location = Location.get(
      "C:\\Users\\protoDir",
      "languageDir\\language.proto"
    )
    assertThat(location.base).isEqualTo("C:/Users/protoDir")
    assertThat(location.path).isEqualTo("languageDir/language.proto")
  }
}
