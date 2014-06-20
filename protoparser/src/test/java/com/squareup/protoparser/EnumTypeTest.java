package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.EnumType.Value;
import static com.squareup.protoparser.TestUtils.NO_OPTIONS;
import static com.squareup.protoparser.TestUtils.NO_VALUES;
import static com.squareup.protoparser.TestUtils.list;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class EnumTypeTest {
  @Test public void emptyToString() {
    EnumType type = new EnumType("Enum", "", "", NO_OPTIONS, NO_VALUES);
    String expected = "enum Enum {}\n";
    assertThat(type.toString()).isEqualTo(expected);
  }

  @Test public void simpleToString() {
    Value one = new Value("ONE", 1, "", NO_OPTIONS);
    Value two = new Value("TWO", 2, "", NO_OPTIONS);
    Value six = new Value("SIX", 6, "", NO_OPTIONS);
    EnumType type = new EnumType("Enum", "", "", NO_OPTIONS, list(one, two, six));
    String expected = ""
        + "enum Enum {\n"
        + "  ONE = 1;\n"
        + "  TWO = 2;\n"
        + "  SIX = 6;\n"
        + "}\n";
    assertThat(type.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithOptionsToString() {
    Value one = new Value("ONE", 1, "", NO_OPTIONS);
    Value two = new Value("TWO", 2, "", NO_OPTIONS);
    Value six = new Value("SIX", 6, "", NO_OPTIONS);
    Option kitKat = new Option("kit", "kat");
    EnumType type = new EnumType("Enum", "", "", list(kitKat), list(one, two, six));
    String expected = ""
        + "enum Enum {\n"
        + "  option kit = \"kat\";\n"
        + "\n"
        + "  ONE = 1;\n"
        + "  TWO = 2;\n"
        + "  SIX = 6;\n"
        + "}\n";
    assertThat(type.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithDocumentationToString() {
    Value one = new Value("ONE", 1, "", NO_OPTIONS);
    Value two = new Value("TWO", 2, "", NO_OPTIONS);
    Value six = new Value("SIX", 6, "", NO_OPTIONS);
    EnumType type = new EnumType("Enum", "", "Hello", NO_OPTIONS, list(one, two, six));
    String expected = ""
        + "// Hello\n"
        + "enum Enum {\n"
        + "  ONE = 1;\n"
        + "  TWO = 2;\n"
        + "  SIX = 6;\n"
        + "}\n";
    assertThat(type.toString()).isEqualTo(expected);
  }

  @Test public void fieldToString() {
    Value value = new Value("NAME", 1, "", NO_OPTIONS);
    String expected = "NAME = 1;\n";
    assertThat(value.toString()).isEqualTo(expected);
  }

  @Test public void fieldWithDocumentationToString() {
    Value value = new Value("NAME", 1, "Hello", NO_OPTIONS);
    String expected = ""
        + "// Hello\n"
        + "NAME = 1;\n";
    assertThat(value.toString()).isEqualTo(expected);
  }

  @Test public void fieldWithOptions() {
    Value value = new Value("NAME", 1, "", list(new Option("kit", "kat")));
    String expected = "NAME = 1 [\n"
        + "  kit = \"kat\"\n"
        + "];\n";
    assertThat(value.toString()).isEqualTo(expected);
  }

  @Test public void duplicateValueTagThrows() {
    Value value1 = new Value("VALUE1", 1, "", NO_OPTIONS);
    Value value2 = new Value("VALUE2", 1, "", NO_OPTIONS);
    try {
      new EnumType("Enum1", "example.Enum", "", NO_OPTIONS, list(value1, value2));
      fail("Duplicate tags not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate tag 1 in example.Enum");
    }
  }

  @Test public void duplicateValueTagWithAllowAlias() {
    Option option1 = new Option("allow_alias", true);
    Value value1 = new Value("VALUE1", 1, "", NO_OPTIONS);
    Value value2 = new Value("VALUE2", 1, "", NO_OPTIONS);
    EnumType type = new EnumType("Enum1", "example.Enum", "", list(option1), list(value1, value2));

    String expected = ""
        + "enum Enum1 {\n"
        + "  option allow_alias = true;\n"
        + "\n"
        + "  VALUE1 = 1;\n"
        + "  VALUE2 = 1;\n"
        + "}\n";

    assertThat(type.toString()).isEqualTo(expected);
  }
}
