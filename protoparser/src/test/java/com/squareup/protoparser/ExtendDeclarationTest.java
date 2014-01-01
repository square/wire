package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.MessageType.Field;
import static com.squareup.protoparser.MessageType.Label.REQUIRED;
import static com.squareup.protoparser.TestUtils.NO_FIELDS;
import static com.squareup.protoparser.TestUtils.NO_OPTIONS;
import static com.squareup.protoparser.TestUtils.list;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class ExtendDeclarationTest {
  @Test public void emptyToString() {
    ExtendDeclaration extend = new ExtendDeclaration("Name", "", "", NO_FIELDS);
    String expected = "extend Name {}\n";
    assertThat(extend.toString()).isEqualTo(expected);
  }

  @Test public void simpleToString() {
    Field field = new Field(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    ExtendDeclaration extend = new ExtendDeclaration("Name", "", "", list(field));
    String expected = ""
        + "extend Name {\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(extend.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithDocumentationToString() {
    Field field = new Field(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    ExtendDeclaration extend = new ExtendDeclaration("Name", "", "Hello", list(field));
    String expected = ""
        + "// Hello\n"
        + "extend Name {\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(extend.toString()).isEqualTo(expected);
  }

  @Test public void duplicateTagValueThrows() {
    Field field1 = new Field(REQUIRED, "Type", "name1", 1, "", NO_OPTIONS);
    Field field2 = new Field(REQUIRED, "Type", "name2", 1, "", NO_OPTIONS);
    try {
      new ExtendDeclaration("Extend", "example.Extend", "", list(field1, field2));
      fail("Duplicate tag values are not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate tag 1 in example.Extend");
    }
  }
}
