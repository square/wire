package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.MessageElement.Label.REQUIRED;
import static com.squareup.protoparser.TestUtils.NO_FIELDS;
import static com.squareup.protoparser.TestUtils.NO_OPTIONS;
import static com.squareup.protoparser.TestUtils.list;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ExtendElementTest {
  @Test public void emptyToString() {
    ExtendElement extend = ExtendElement.create("Name", "Name", "", NO_FIELDS);
    String expected = "extend Name {}\n";
    assertThat(extend.toString()).isEqualTo(expected);
  }

  @Test public void simpleToString() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    ExtendElement extend = ExtendElement.create("Name", "Name", "", list(field));
    String expected = ""
        + "extend Name {\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(extend.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithDocumentationToString() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    ExtendElement extend = ExtendElement.create("Name", "Name", "Hello", list(field));
    String expected = ""
        + "// Hello\n"
        + "extend Name {\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(extend.toString()).isEqualTo(expected);
  }

  @Test public void duplicateTagValueThrows() {
    FieldElement field1 = FieldElement.create(REQUIRED, "Type", "name1", 1, "", NO_OPTIONS);
    FieldElement field2 = FieldElement.create(REQUIRED, "Type", "name2", 1, "", NO_OPTIONS);
    try {
      ExtendElement.create("Extend", "example.Extend", "", list(field1, field2));
      fail("Duplicate tag values are not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate tag 1 in example.Extend");
    }
  }
}
