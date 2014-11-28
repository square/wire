package com.squareup.protoparser;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class ExtensionsElementTest {
  @Test public void singleValueToString() {
    ExtensionsElement actual = ExtensionsElement.create("", 500, 500);
    String expected = "extensions 500;\n";
    assertThat(actual.toString()).isEqualTo(expected);
  }

  @Test public void rangeToString() {
    ExtensionsElement actual = ExtensionsElement.create("", 500, 505);
    String expected = "extensions 500 to 505;\n";
    assertThat(actual.toString()).isEqualTo(expected);
  }

  @Test public void maxRangeToString() {
    ExtensionsElement actual = ExtensionsElement.create("", 500, ProtoFile.MAX_TAG_VALUE);
    String expected = "extensions 500 to max;\n";
    assertThat(actual.toString()).isEqualTo(expected);
  }

  @Test public void withDocumentationToString() {
    ExtensionsElement actual = ExtensionsElement.create("Hello", 500, 500);
    String expected = ""
        + "// Hello\n"
        + "extensions 500;\n";
    assertThat(actual.toString()).isEqualTo(expected);
  }
}
