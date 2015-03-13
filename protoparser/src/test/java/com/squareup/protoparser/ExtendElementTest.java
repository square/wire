package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.MessageElement.Label.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ExtendElementTest {
  @Test public void emptyToString() {
    ExtendElement extend = ExtendElement.builder().name("Name").build();
    String expected = "extend Name {}\n";
    assertThat(extend.toString()).isEqualTo(expected);
  }

  @Test public void simpleToString() {
    ExtendElement extend = ExtendElement.builder()
        .name("Name")
        .addField(FieldElement.builder()
            .label(REQUIRED)
            .type("Type")
            .name("name")
            .tag(1)
            .build())
        .build();
    String expected = ""
        + "extend Name {\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(extend.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithDocumentationToString() {
    ExtendElement extend = ExtendElement.builder()
        .name("Name")
        .documentation("Hello")
        .addField(FieldElement.builder()
            .label(REQUIRED)
            .type("Type")
            .name("name")
            .tag(1)
            .build())
        .build();
    String expected = ""
        + "// Hello\n"
        + "extend Name {\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(extend.toString()).isEqualTo(expected);
  }

  @Test public void duplicateTagValueThrows() {
    FieldElement field1 = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name1")
        .tag(1)
        .build();
    FieldElement field2 = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name2")
        .tag(1)
        .build();
    try {
      ExtendElement.builder()
          .name("Extend")
          .qualifiedName("example.Extend")
          .addField(field1)
          .addField(field2)
          .build();
      fail("Duplicate tag values are not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate tag 1 in example.Extend");
    }
  }
}
