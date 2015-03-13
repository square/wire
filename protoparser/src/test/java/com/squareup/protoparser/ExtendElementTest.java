package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.MessageElement.Label.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ExtendElementTest {
  @Test public void nameRequired() {
    try {
      ExtendElement.builder().qualifiedName("Test").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
  }

  @Test public void nameSetsQualifiedName() {
    ExtendElement test = ExtendElement.builder().name("Test").build();
    assertThat(test.name()).isEqualTo("Test");
    assertThat(test.qualifiedName()).isEqualTo("Test");
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      ExtendElement.builder().name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
    try {
      ExtendElement.builder().qualifiedName(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("qualifiedName == null");
    }
    try {
      ExtendElement.builder().documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation == null");
    }
    try {
      ExtendElement.builder().addField(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("field == null");
    }
  }

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
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate tag 1 in example.Extend");
    }
  }
}
