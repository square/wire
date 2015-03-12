package com.squareup.protoparser;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class EnumElementTest {
  @Test public void emptyToString() {
    EnumElement element = EnumElement.builder().name("Enum").build();
    String expected = "enum Enum {}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void simpleToString() {
    EnumElement element = EnumElement.builder()
        .name("Enum")
        .addConstant(EnumConstantElement.builder().name("ONE").tag(1).build())
        .addConstant(EnumConstantElement.builder().name("TWO").tag(2).build())
        .addConstant(EnumConstantElement.builder().name("SIX").tag(6).build())
        .build();
    String expected = ""
        + "enum Enum {\n"
        + "  ONE = 1;\n"
        + "  TWO = 2;\n"
        + "  SIX = 6;\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithOptionsToString() {
    EnumElement element = EnumElement.builder()
        .name("Enum")
        .addOption(OptionElement.create("kit", "kat", false))
        .addConstant(EnumConstantElement.builder().name("ONE").tag(1).build())
        .addConstant(EnumConstantElement.builder().name("TWO").tag(2).build())
        .addConstant(EnumConstantElement.builder().name("SIX").tag(6).build())
        .build();
    String expected = ""
        + "enum Enum {\n"
        + "  option kit = \"kat\";\n"
        + "\n"
        + "  ONE = 1;\n"
        + "  TWO = 2;\n"
        + "  SIX = 6;\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithDocumentationToString() {
    EnumElement element = EnumElement.builder()
        .name("Enum")
        .documentation("Hello")
        .addConstant(EnumConstantElement.builder().name("ONE").tag(1).build())
        .addConstant(EnumConstantElement.builder().name("TWO").tag(2).build())
        .addConstant(EnumConstantElement.builder().name("SIX").tag(6).build())
        .build();
    String expected = ""
        + "// Hello\n"
        + "enum Enum {\n"
        + "  ONE = 1;\n"
        + "  TWO = 2;\n"
        + "  SIX = 6;\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void fieldToString() {
    EnumConstantElement value = EnumConstantElement.builder().name("NAME").tag(1).build();
    String expected = "NAME = 1;\n";
    assertThat(value.toString()).isEqualTo(expected);
  }

  @Test public void fieldWithDocumentationToString() {
    EnumConstantElement value = EnumConstantElement.builder()
        .name("NAME")
        .tag(1)
        .documentation("Hello")
        .build();
    String expected = ""
        + "// Hello\n"
        + "NAME = 1;\n";
    assertThat(value.toString()).isEqualTo(expected);
  }

  @Test public void fieldWithOptions() {
    EnumConstantElement value = EnumConstantElement.builder()
        .name("NAME")
        .tag(1)
        .addOption(OptionElement.create("kit", "kat", true))
        .addOption(OptionElement.create("tit", "tat", false))
        .build();
    String expected = "NAME = 1 [\n"
        + "  (kit) = \"kat\",\n"
        + "  tit = \"tat\"\n"
        + "];\n";
    assertThat(value.toString()).isEqualTo(expected);
  }

  @Test public void duplicateValueTagThrows() {
    try {
      EnumElement.builder()
          .name("Enum1")
          .qualifiedName("example.Enum")
          .addConstant(EnumConstantElement.builder().name("VALUE1").tag(1).build())
          .addConstant(EnumConstantElement.builder().name("VALUE2").tag(1).build())
          .build();
      fail("Duplicate tags not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate tag 1 in example.Enum");
    }
  }

  @Test public void duplicateValueTagWithAllowAlias() {
    EnumElement element = EnumElement.builder()
        .name("Enum1")
        .qualifiedName("example.Enum")
        .addOption(OptionElement.create("allow_alias", true, false))
        .addConstant(EnumConstantElement.builder().name("VALUE1").tag(1).build())
        .addConstant(EnumConstantElement.builder().name("VALUE2").tag(1).build())
        .build();

    String expected = ""
        + "enum Enum1 {\n"
        + "  option allow_alias = true;\n"
        + "\n"
        + "  VALUE1 = 1;\n"
        + "  VALUE2 = 1;\n"
        + "}\n";

    assertThat(element.toString()).isEqualTo(expected);
  }
}
