package com.squareup.protoparser;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ExtensionsElementTest {
  @Test public void invalidTagRangeThrows() {
    try {
      ExtensionsElement.create(Integer.MIN_VALUE, 500);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Invalid start value: -2147483648");
    }
    try {
      ExtensionsElement.create(500, Integer.MAX_VALUE);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Invalid end value: 2147483647");
    }
  }

  @Test public void singleValueToString() {
    ExtensionsElement actual = ExtensionsElement.create(500, 500);
    String expected = "extensions 500;\n";
    assertThat(actual.toString()).isEqualTo(expected);
  }

  @Test public void rangeToString() {
    ExtensionsElement actual = ExtensionsElement.create(500, 505);
    String expected = "extensions 500 to 505;\n";
    assertThat(actual.toString()).isEqualTo(expected);
  }

  @Test public void maxRangeToString() {
    ExtensionsElement actual = ExtensionsElement.create(500, ProtoFile.MAX_TAG_VALUE);
    String expected = "extensions 500 to max;\n";
    assertThat(actual.toString()).isEqualTo(expected);
  }

  @Test public void withDocumentationToString() {
    ExtensionsElement actual = ExtensionsElement.create(500, 500, "Hello");
    String expected = ""
        + "// Hello\n"
        + "extensions 500;\n";
    assertThat(actual.toString()).isEqualTo(expected);
  }
}
