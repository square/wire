package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.MessageElement.Label.ONE_OF;
import static com.squareup.protoparser.MessageElement.Label.REQUIRED;
import static com.squareup.protoparser.TestUtils.NO_EXTENSIONS;
import static com.squareup.protoparser.TestUtils.NO_FIELDS;
import static com.squareup.protoparser.TestUtils.NO_ONEOFS;
import static com.squareup.protoparser.TestUtils.NO_OPTIONS;
import static com.squareup.protoparser.TestUtils.NO_TYPES;
import static com.squareup.protoparser.TestUtils.list;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class MessageElementTest {
  @Test public void emptyToString() {
    TypeElement element =
        MessageElement.create("Message", "", "", NO_FIELDS, NO_ONEOFS, NO_TYPES, NO_EXTENSIONS,
            NO_OPTIONS);
    String expected = "message Message {}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void simpleToString() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    TypeElement element =
        MessageElement.create("Message", "", "", list(field), NO_ONEOFS, NO_TYPES, NO_EXTENSIONS,
            NO_OPTIONS);
    String expected = ""
        + "message Message {\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithDocumentationToString() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    TypeElement element =
        MessageElement.create("Message", "", "Hello", list(field), NO_ONEOFS, NO_TYPES,
            NO_EXTENSIONS, NO_OPTIONS);
    String expected = ""
        + "// Hello\n"
        + "message Message {\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithOptionsToString() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    TypeElement element =
        MessageElement.create("Message", "", "", list(field), NO_ONEOFS, NO_TYPES, NO_EXTENSIONS,
            list(OptionElement.create("kit", "kat", false)));
    String expected = ""
        + "message Message {\n"
        + "  option kit = \"kat\";\n"
        + "\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithNestedElementsToString() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    TypeElement nested =
        MessageElement.create("Nested", "", "", list(field), NO_ONEOFS, NO_TYPES, NO_EXTENSIONS,
            NO_OPTIONS);
    TypeElement element =
        MessageElement.create("Message", "", "", list(field), NO_ONEOFS, list(nested),
            NO_EXTENSIONS, NO_OPTIONS);
    String expected = ""
        + "message Message {\n"
        + "  required Type name = 1;\n"
        + "\n"
        + "  message Nested {\n"
        + "    required Type name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithExtensionsToString() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    ExtensionsElement extensions = ExtensionsElement.create("", 500, 501);
    TypeElement element =
        MessageElement.create("Message", "", "", list(field), NO_ONEOFS, NO_TYPES, list(extensions),
            NO_OPTIONS);
    String expected = ""
        + "message Message {\n"
        + "  required Type name = 1;\n"
        + "\n"
        + "  extensions 500 to 501;\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void oneOfToString() {
    FieldElement field = FieldElement.create(ONE_OF, "Type", "name", 1, "", NO_OPTIONS);
    OneOfElement oneOf = OneOfElement.create("hi", "", list(field));
    TypeElement element =
        MessageElement.create("Message", "", "", NO_FIELDS, list(oneOf), NO_TYPES, NO_EXTENSIONS,
            NO_OPTIONS);
    String expected = ""
        + "message Message {\n"
        + "  oneof hi {\n"
        + "    Type name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void multipleEverythingToString() {
    FieldElement field1 = FieldElement.create(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    FieldElement field2 =
        FieldElement.create(REQUIRED, "OtherType", "other_name", 2, "", NO_OPTIONS);
    FieldElement oneOf1Field = FieldElement.create(ONE_OF, "Type", "namey", 3, "", NO_OPTIONS);
    OneOfElement oneOf1 = OneOfElement.create("thingy", "", list(oneOf1Field));
    FieldElement oneOf2Field = FieldElement.create(ONE_OF, "Type", "namer", 4, "", NO_OPTIONS);
    OneOfElement oneOf2 = OneOfElement.create("thinger", "", list(oneOf2Field));
    ExtensionsElement extensions1 = ExtensionsElement.create("", 500, 501);
    ExtensionsElement extensions2 = ExtensionsElement.create("", 503, 503);
    TypeElement nested =
        MessageElement.create("Nested", "", "", list(field1), NO_ONEOFS, NO_TYPES, NO_EXTENSIONS,
            NO_OPTIONS);
    OptionElement option = OptionElement.create("kit", "kat", false);
    TypeElement element =
        MessageElement.create("Message", "", "", list(field1, field2), list(oneOf1, oneOf2),
            list(nested), list(extensions1, extensions2), list(option));
    String expected = ""
        + "message Message {\n"
        + "  option kit = \"kat\";\n"
        + "\n"
        + "  required Type name = 1;\n"
        + "  required OtherType other_name = 2;\n"
        + "\n"
        + "  oneof thingy {\n"
        + "    Type namey = 3;\n"
        + "  }\n"
        + "  oneof thinger {\n"
        + "    Type namer = 4;\n"
        + "  }\n"
        + "\n"
        + "  extensions 500 to 501;\n"
        + "  extensions 503;\n"
        + "\n"
        + "  message Nested {\n"
        + "    required Type name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void fieldToString() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name", 1, "", NO_OPTIONS);
    String expected = "required Type name = 1;\n";
    assertThat(field.toString()).isEqualTo(expected);
  }

  @Test public void oneOfFieldToString() {
    FieldElement field = FieldElement.create(ONE_OF, "Type", "name", 1, "", NO_OPTIONS);
    String expected = "Type name = 1;\n";
    assertThat(field.toString()).isEqualTo(expected);
  }

  @Test public void fieldWithDocumentationToString() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name", 1, "Hello", NO_OPTIONS);
    String expected = ""
        + "// Hello\n"
        + "required Type name = 1;\n";
    assertThat(field.toString()).isEqualTo(expected);
  }

  @Test public void fieldWithOptions() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name", 1, "",
        list(OptionElement.create("kit", "kat", false)));
    String expected = "required Type name = 1 [\n"
        + "  kit = \"kat\"\n"
        + "];\n";
    assertThat(field.toString()).isEqualTo(expected);
  }

  @Test public void duplicateTagValueThrows() {
    FieldElement field1 = FieldElement.create(REQUIRED, "Type", "name1", 1, "", NO_OPTIONS);
    FieldElement field2 = FieldElement.create(REQUIRED, "Type", "name2", 1, "", NO_OPTIONS);
    try {
      MessageElement.create("Message", "example.Message", "", list(field1, field2), NO_ONEOFS,
          NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
      fail("Duplicate tag values are not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate tag 1 in example.Message");
    }
  }

  @Test public void duplicateTagValueOneOfThrows() {
    FieldElement field1 = FieldElement.create(REQUIRED, "Type", "name1", 1, "", NO_OPTIONS);
    FieldElement field2 = FieldElement.create(ONE_OF, "Type", "name2", 1, "", NO_OPTIONS);
    OneOfElement oneOf = OneOfElement.create("name3", "", list(field2));

    try {
      MessageElement.create("Message", "example.Message", "", list(field1), list(oneOf),
          NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
      fail("Duplicate tag values are not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate tag 1 in example.Message");
    }
  }

  @Test public void oneOfFieldDisallowed() {
    FieldElement field1 = FieldElement.create(ONE_OF, "Type", "name", 1, "", NO_OPTIONS);
    try {
      MessageElement.create("Message", "example.Message", "", list(field1), NO_ONEOFS, NO_TYPES,
          NO_EXTENSIONS, NO_OPTIONS);
      fail("Fields declaring as 'oneof' are not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Field 'name' in example.Message improperly declares itself a member of a 'oneof' group but is not.");
    }
  }

  @Test public void duplicateEnumValueTagInScopeThrows() {
    EnumConstantElement value = EnumConstantElement.create("VALUE", 1, "", NO_OPTIONS);
    TypeElement enum1 = EnumElement.create("Enum1", "example.Enum1", "", NO_OPTIONS, list(value));
    TypeElement enum2 = EnumElement.create("Enum2", "example.Enum2", "", NO_OPTIONS, list(value));
    try {
      MessageElement.create("Message", "example.Message", "", NO_FIELDS, NO_ONEOFS,
          list(enum1, enum2), NO_EXTENSIONS, NO_OPTIONS);
      fail("Duplicate name not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate enum constant VALUE in scope example.Message");
    }
  }

  @Test public void deprecatedTrue() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name1", 1, "",
        list(OptionElement.create("deprecated", "true", false)));
    assertThat(field.isDeprecated()).isTrue();
  }

  @Test public void deprecatedFalse() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name1", 1, "",
        list(OptionElement.create("deprecated", "false", false)));
    assertThat(field.isDeprecated()).isFalse();
  }

  @Test public void deprecatedMissing() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name1", 1, "", NO_OPTIONS);
    assertThat(field.isDeprecated()).isFalse();
  }

  @Test public void packedTrue() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name1", 1, "",
        list(OptionElement.create("packed", "true", false)));
    assertThat(field.isPacked()).isTrue();
  }

  @Test public void packedFalse() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name1", 1, "",
        list(OptionElement.create("packed", "false", false)));
    assertThat(field.isPacked()).isFalse();
  }

  @Test public void packedMissing() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name1", 1, "", NO_OPTIONS);
    assertThat(field.isPacked()).isFalse();
  }

  @Test public void defaultValue() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name1", 1, "",
        list(OptionElement.create("default", "foo", false)));
    assertThat(field.getDefault()).isEqualTo("foo");
  }

  @Test public void defaultMissing() {
    FieldElement field = FieldElement.create(REQUIRED, "Type", "name1", 1, "", NO_OPTIONS);
    assertThat(field.getDefault()).isNull();
  }
}
