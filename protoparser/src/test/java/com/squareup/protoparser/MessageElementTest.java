package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.MessageElement.Label.ONE_OF;
import static com.squareup.protoparser.MessageElement.Label.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class MessageElementTest {
  @Test public void nameRequired() {
    try {
      MessageElement.builder().qualifiedName("Test").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
  }

  @Test public void nameSetsQualifiedName() {
    MessageElement test = MessageElement.builder().name("Test").build();
    assertThat(test.name()).isEqualTo("Test");
    assertThat(test.qualifiedName()).isEqualTo("Test");
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      MessageElement.builder().name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
    try {
      MessageElement.builder().qualifiedName(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("qualifiedName == null");
    }
    try {
      MessageElement.builder().documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation == null");
    }
    try {
      MessageElement.builder().addField(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("field == null");
    }
    try {
      MessageElement.builder().addType(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("type == null");
    }
    try {
      MessageElement.builder().addOneOf(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("oneOf == null");
    }
    try {
      MessageElement.builder().addExtensions(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("extensions == null");
    }
    try {
      MessageElement.builder().addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option == null");
    }
  }

  @Test public void emptyToString() {
    TypeElement element = MessageElement.builder().name("Message").build();
    String expected = "message Message {}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void simpleToString() {
    TypeElement element = MessageElement.builder()
        .name("Message")
        .addField(FieldElement.builder()
            .label(REQUIRED)
            .type("Type")
            .name("name")
            .tag(1)
            .build())
        .build();
    String expected = ""
        + "message Message {\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithDocumentationToString() {
    TypeElement element = MessageElement.builder()
        .name("Message")
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
        + "message Message {\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithOptionsToString() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name")
        .tag(1)
        .build();
    TypeElement element = MessageElement.builder()
        .name("Message")
        .addField(field)
        .addOption(OptionElement.create("kit", "kat", false))
        .build();
    String expected = ""
        + "message Message {\n"
        + "  option kit = \"kat\";\n"
        + "\n"
        + "  required Type name = 1;\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithNestedElementsToString() {
    TypeElement element = MessageElement.builder()
        .name("Message")
        .addField(FieldElement.builder()
            .label(REQUIRED)
            .type("Type")
            .name("name")
            .tag(1)
            .build())
        .addType(MessageElement.builder()
            .name("Nested")
            .addField(FieldElement.builder()
                .label(REQUIRED)
                .type("Type")
                .name("name")
                .tag(1)
                .build())
            .build())
        .build();
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
    TypeElement element = MessageElement.builder()
        .name("Message")
        .addField(FieldElement.builder()
            .label(REQUIRED)
            .type("Type")
            .name("name")
            .tag(1)
            .build())
        .addExtensions(ExtensionsElement.create(500, 501))
        .build();
    String expected = ""
        + "message Message {\n"
        + "  required Type name = 1;\n"
        + "\n"
        + "  extensions 500 to 501;\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void oneOfToString() {
    TypeElement element = MessageElement.builder()
        .name("Message")
        .addOneOf(OneOfElement.builder()
            .name("hi")
            .addField(FieldElement.builder()
                .label(ONE_OF)
                .type("Type")
                .name("name")
                .tag(1)
                .build())
            .build())
        .build();
    String expected = ""
        + "message Message {\n"
        + "  oneof hi {\n"
        + "    Type name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toString()).isEqualTo(expected);
  }

  @Test public void multipleEverythingToString() {
    FieldElement field1 = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name")
        .tag(1)
        .build();
    FieldElement field2 = FieldElement.builder()
        .label(REQUIRED)
        .type("OtherType")
        .name("other_name")
        .tag(2)
        .build();
    FieldElement oneOf1Field = FieldElement.builder()
        .label(ONE_OF)
        .type("Type")
        .name("namey")
        .tag(3)
        .build();
    OneOfElement oneOf1 = OneOfElement.builder()
        .name("thingy")
        .addField(oneOf1Field)
        .build();
    FieldElement oneOf2Field = FieldElement.builder()
        .label(ONE_OF)
        .type("Type")
        .name("namer")
        .tag(4)
        .build();
    OneOfElement oneOf2 = OneOfElement.builder()
        .name("thinger")
        .addField(oneOf2Field)
        .build();
    ExtensionsElement extensions1 = ExtensionsElement.create(500, 501);
    ExtensionsElement extensions2 = ExtensionsElement.create(503, 503);
    TypeElement nested = MessageElement.builder().name("Nested").addField(field1).build();
    OptionElement option = OptionElement.create("kit", "kat", false);
    TypeElement element = MessageElement.builder()
        .name("Message")
        .addField(field1)
        .addField(field2)
        .addOneOf(oneOf1)
        .addOneOf(oneOf2)
        .addType(nested)
        .addExtensions(extensions1)
        .addExtensions(extensions2)
        .addOption(option)
        .build();
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
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name")
        .tag(1)
        .build();
    String expected = "required Type name = 1;\n";
    assertThat(field.toString()).isEqualTo(expected);
  }

  @Test public void oneOfFieldToString() {
    FieldElement field = FieldElement.builder()
        .label(ONE_OF)
        .type("Type")
        .name("name")
        .tag(1)
        .build();
    String expected = "Type name = 1;\n";
    assertThat(field.toString()).isEqualTo(expected);
  }

  @Test public void fieldWithDocumentationToString() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name")
        .tag(1)
        .documentation("Hello")
        .build();
    String expected = ""
        + "// Hello\n"
        + "required Type name = 1;\n";
    assertThat(field.toString()).isEqualTo(expected);
  }

  @Test public void fieldWithOptions() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name")
        .tag(1)
        .addOption(OptionElement.create("kit", "kat", false))
        .build();
    String expected = "required Type name = 1 [\n"
        + "  kit = \"kat\"\n"
        + "];\n";
    assertThat(field.toString()).isEqualTo(expected);
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
      MessageElement.builder()
          .name("Message")
          .qualifiedName("example.Message")
          .addField(field1)
          .addField(field2)
          .build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate tag 1 in example.Message");
    }
  }

  @Test public void duplicateTagValueOneOfThrows() {
    FieldElement field1 = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name1")
        .tag(1)
        .build();
    FieldElement field2 = FieldElement.builder()
        .label(ONE_OF)
        .type("Type")
        .name("name2")
        .tag(1)
        .build();
    OneOfElement oneOf = OneOfElement.builder().name("name3").addField(field2).build();

    try {
      MessageElement.builder()
          .name("Message")
          .qualifiedName("example.Message")
          .addField(field1)
          .addOneOf(oneOf)
          .build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate tag 1 in example.Message");
    }
  }

  @Test public void oneOfFieldDisallowed() {
    FieldElement field1 = FieldElement.builder()
        .label(ONE_OF)
        .type("Type")
        .name("name")
        .tag(1)
        .build();
    try {
      MessageElement.builder()
          .name("Message")
          .qualifiedName("example.Message")
          .addField(field1)
          .build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Field 'name' in example.Message improperly declares itself a member of a 'oneof' group but is not.");
    }
  }

  @Test public void duplicateEnumValueTagInScopeThrows() {
    EnumConstantElement value = EnumConstantElement.builder().name("VALUE").tag(1).build();
    TypeElement enum1 = EnumElement.builder()
        .name("Enum1")
        .qualifiedName("example.Enum1")
        .addConstant(value)
        .build();
    TypeElement enum2 = EnumElement.builder()
        .name("Enum2")
        .qualifiedName("example.Enum2")
        .addConstant(value)
        .build();
    try {
      MessageElement.builder()
          .name("Message")
          .qualifiedName("example.Message")
          .addType(enum1)
          .addType(enum2)
          .build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate enum constant VALUE in scope example.Message");
    }
  }

  @Test public void deprecatedTrue() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name1")
        .tag(1)
        .addOption(OptionElement.create("deprecated", "true", false))
        .build();
    assertThat(field.isDeprecated()).isTrue();
  }

  @Test public void deprecatedFalse() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name1")
        .tag(1)
        .addOption(OptionElement.create("deprecated", "false", false))
        .build();
    assertThat(field.isDeprecated()).isFalse();
  }

  @Test public void deprecatedMissing() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name1")
        .tag(1)
        .build();
    assertThat(field.isDeprecated()).isFalse();
  }

  @Test public void packedTrue() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name1")
        .tag(1)
        .addOption(OptionElement.create("packed", "true", false))
        .build();
    assertThat(field.isPacked()).isTrue();
  }

  @Test public void packedFalse() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name1")
        .tag(1)
        .addOption(OptionElement.create("packed", "false", false))
        .build();
    assertThat(field.isPacked()).isFalse();
  }

  @Test public void packedMissing() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name1")
        .tag(1)
        .build();
    assertThat(field.isPacked()).isFalse();
  }

  @Test public void defaultValue() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name1")
        .tag(1)
        .addOption(OptionElement.create("default", "foo", false))
        .build();
    assertThat(field.getDefault()).isEqualTo("foo");
  }

  @Test public void defaultMissing() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type("Type")
        .name("name1")
        .tag(1)
        .build();
    assertThat(field.getDefault()).isNull();
  }
}
