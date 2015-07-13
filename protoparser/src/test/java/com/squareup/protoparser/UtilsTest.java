package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static org.assertj.core.api.Assertions.assertThat;

public class UtilsTest {
  @Test public void indentationTest() {
    String input = "Foo\nBar\nBaz";
    String expected = "  Foo\n  Bar\n  Baz\n";
    StringBuilder builder = new StringBuilder();
    appendIndented(builder, input);
    assertThat(builder.toString()).isEqualTo(expected);
  }

  @Test public void documentationTest() {
    String input = "Foo\nBar\nBaz";
    String expected = ""
        + "// Foo\n"
        + "// Bar\n"
        + "// Baz\n";
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, input);
    assertThat(builder.toString()).isEqualTo(expected);
  }
}
