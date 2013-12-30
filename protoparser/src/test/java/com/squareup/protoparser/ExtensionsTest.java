package com.squareup.protoparser;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class ExtensionsTest {
  @Test public void singleValueToString() {
    Extensions actual = new Extensions("", 500, 500);
    String expected = "extensions 500;\n";
    assertThat(actual.toString()).isEqualTo(expected);
  }

  @Test public void rangeToString() {
    Extensions actual = new Extensions("", 500, 505);
    String expected = "extensions 500 to 505;\n";
    assertThat(actual.toString()).isEqualTo(expected);
  }

  @Test public void maxRangeToString() {
    Extensions actual = new Extensions("", 500, ProtoFile.MAX_TAG_VALUE);
    String expected = "extensions 500 to max;\n";
    assertThat(actual.toString()).isEqualTo(expected);
  }

  @Test public void withDocumentationToString() {
    Extensions actual = new Extensions("Hello", 500, 500);
    String expected = ""
        + "// Hello\n"
        + "extensions 500;\n";
    assertThat(actual.toString()).isEqualTo(expected);
  }
}
