package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.EnumType.Value;
import static com.squareup.protoparser.MessageType.Field;
import static com.squareup.protoparser.MessageType.Label.REQUIRED;
import static com.squareup.protoparser.TestUtils.NO_EXTENSIONS;
import static com.squareup.protoparser.TestUtils.NO_FIELDS;
import static com.squareup.protoparser.TestUtils.NO_OPTIONS;
import static com.squareup.protoparser.TestUtils.NO_TYPES;
import static com.squareup.protoparser.TestUtils.list;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class MessageTypeTest {
  @Test public void emptyToString() {
    Type type = new MessageType("Message", "", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    String expected = "message Message {}\n";
    assertThat(type.toString()).isEqualTo(expected);
  }

  @Test public void simpleToString() {
    Field field = new Field(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    Type type =
        new MessageType("Message", "", "", list(field), NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    String expected = ""
        + "message Message {\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(type.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithDocumentationToString() {
    Field field = new Field(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    Type type =
        new MessageType("Message", "", "Hello", list(field), NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    String expected = ""
        + "// Hello\n"
        + "message Message {\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(type.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithOptionsToString() {
    Field field = new Field(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    Type type = new MessageType("Message", "", "", list(field), NO_TYPES, NO_EXTENSIONS,
        list(new Option("kit", "kat")));
    String expected = ""
        + "message Message {\n"
        + "  option kit = \"kat\";\n"
        + "\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(type.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithNestedTypesToString() {
    Field field = new Field(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    Type nested =
        new MessageType("Nested", "", "", list(field), NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    Type type =
        new MessageType("Message", "", "", list(field), list(nested), NO_EXTENSIONS, NO_OPTIONS);
    String expected = ""
        + "message Message {\n"
        + "  required Type name = 1;\n"
        + "\n"
        + "  message Nested {\n"
        + "    required Type name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(type.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithExtensionsToString() {
    Field field = new Field(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    Extensions extensions = new Extensions("", 500, 501);
    Type type =
        new MessageType("Message", "", "", list(field), NO_TYPES, list(extensions), NO_OPTIONS);
    String expected = ""
        + "message Message {\n"
        + "  required Type name = 1;\n"
        + "\n"
        + "  extensions 500 to 501;\n"
        + "}\n";
    assertThat(type.toString()).isEqualTo(expected);
  }

  @Test public void multipleEverythingToString() {
    Field field1 = new Field(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    Field field2 = new Field(REQUIRED, "OtherType", "other_name", 2, "", NO_OPTIONS);
    Extensions extensions1 = new Extensions("", 500, 501);
    Extensions extensions2 = new Extensions("", 503, 503);
    Type nested =
        new MessageType("Nested", "", "", list(field1), NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    Option option = new Option("kit", "kat");
    Type type = new MessageType("Message", "", "", list(field1, field2), list(nested),
        list(extensions1, extensions2), list(option));
    String expected = ""
        + "message Message {\n"
        + "  option kit = \"kat\";\n"
        + "\n"
        + "  required Type name = 1;\n"
        + "  required OtherType other_name = 2;\n"
        + "\n"
        + "  extensions 500 to 501;\n"
        + "  extensions 503;\n"
        + "\n"
        + "  message Nested {\n"
        + "    required Type name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(type.toString()).isEqualTo(expected);
  }

  @Test public void fieldToString() {
    Field field = new Field(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    String expected = "required Type name = 1;\n";
    assertThat(field.toString()).isEqualTo(expected);
  }

  @Test public void fieldWithDocumentationToString() {
    Field field = new Field(REQUIRED, "Type", "name", 1, "Hello", NO_OPTIONS);
    String expected = ""
        + "// Hello\n"
        + "required Type name = 1;\n";
    assertThat(field.toString()).isEqualTo(expected);
  }

  @Test public void fieldWithOptions() {
    Field field = new Field(REQUIRED, "Type", "name", 1, "", list(new Option("kit", "kat")));
    String expected = "required Type name = 1 [\n"
        + "  kit = \"kat\"\n"
        + "];\n";
    assertThat(field.toString()).isEqualTo(expected);
  }

  @Test public void duplicateTagValueThrows() {
    Field field1 = new Field(REQUIRED, "Type", "name1", 1, "", NO_OPTIONS);
    Field field2 = new Field(REQUIRED, "Type", "name2", 1, "", NO_OPTIONS);
    try {
      new MessageType("Message", "example.Message", "", list(field1, field2), NO_TYPES,
          NO_EXTENSIONS, NO_OPTIONS);
      fail("Duplicate tag values are not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate tag 1 in example.Message");
    }
  }

  @Test public void duplicateEnumValueTagInScopeThrows() {
    Value value = new Value("VALUE", 1, "", NO_OPTIONS);
    Type enum1 = new EnumType("Enum1", "example.Enum1", "", NO_OPTIONS, list(value));
    Type enum2 = new EnumType("Enum2", "example.Enum2", "", NO_OPTIONS, list(value));
    try {
      new MessageType("Message", "example.Message", "", NO_FIELDS, list(enum1, enum2),
          NO_EXTENSIONS, NO_OPTIONS);
      fail("Duplicate name not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate enum name VALUE in scope example.Message");
    }
  }

  @Test public void deprecatedTrue() {
    Field field =
        new Field(REQUIRED, "Type", "name1", 1, "", list(new Option("deprecated", "true")));
    assertThat(field.isDeprecated()).isTrue();
  }

  @Test public void deprecatedFalse() {
    Field field =
        new Field(REQUIRED, "Type", "name1", 1, "", list(new Option("deprecated", "false")));
    assertThat(field.isDeprecated()).isFalse();
  }

  @Test public void deprecatedMissing() {
    Field field = new Field(REQUIRED, "Type", "name1", 1, "", NO_OPTIONS);
    assertThat(field.isDeprecated()).isFalse();
  }

  @Test public void packedTrue() {
    Field field = new Field(REQUIRED, "Type", "name1", 1, "", list(new Option("packed", "true")));
    assertThat(field.isPacked()).isTrue();
  }

  @Test public void packedFalse() {
    Field field = new Field(REQUIRED, "Type", "name1", 1, "", list(new Option("packed", "false")));
    assertThat(field.isPacked()).isFalse();
  }

  @Test public void packedMissing() {
    Field field = new Field(REQUIRED, "Type", "name1", 1, "", NO_OPTIONS);
    assertThat(field.isPacked()).isFalse();
  }

  @Test public void defaultValue() {
    Field field = new Field(REQUIRED, "Type", "name1", 1, "", list(new Option("default", "foo")));
    assertThat(field.getDefault()).isEqualTo("foo");
  }

  @Test public void defaultMissing() {
    Field field = new Field(REQUIRED, "Type", "name1", 1, "", NO_OPTIONS);
    assertThat(field.getDefault()).isNull();
  }
}
