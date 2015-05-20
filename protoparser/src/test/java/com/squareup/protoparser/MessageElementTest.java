package com.squareup.protoparser;

import com.squareup.protoparser.OptionElement.Kind;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;

import static com.squareup.protoparser.DataType.ScalarType.BOOL;
import static com.squareup.protoparser.DataType.ScalarType.STRING;
import static com.squareup.protoparser.FieldElement.Label.ONE_OF;
import static com.squareup.protoparser.FieldElement.Label.REQUIRED;
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
      MessageElement.builder().addFields(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("fields == null");
    }
    try {
      MessageElement.builder().addFields(Collections.<FieldElement>singleton(null));
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
      MessageElement.builder().addTypes(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("types == null");
    }
    try {
      MessageElement.builder().addTypes(Collections.<TypeElement>singleton(null));
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
      MessageElement.builder().addOneOfs(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("oneOfs == null");
    }
    try {
      MessageElement.builder().addOneOfs(Collections.<OneOfElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("oneOf == null");
    }
    try {
      MessageElement.builder().addExtensions((ExtensionsElement) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("extensions == null");
    }
    try {
      MessageElement.builder().addExtensions((Collection<ExtensionsElement>) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("extensions == null");
    }
    try {
      MessageElement.builder().addExtensions(Collections.<ExtensionsElement>singleton(null));
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
    try {
      MessageElement.builder().addOptions(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("options == null");
    }
    try {
      MessageElement.builder().addOptions(Collections.<OptionElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option == null");
    }
  }

  @Test public void emptyToSchema() {
    TypeElement element = MessageElement.builder().name("Message").build();
    String expected = "message Message {}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void simpleToSchema() {
    TypeElement element = MessageElement.builder()
        .name("Message")
        .addField(FieldElement.builder().label(REQUIRED).type(STRING).name("name").tag(1).build())
        .build();
    String expected = ""
        + "message Message {\n"
        + "  required string name = 1;\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleFields() {
    FieldElement firstName =
        FieldElement.builder().label(REQUIRED).type(STRING).name("first_name").tag(1).build();
    FieldElement lastName =
        FieldElement.builder().label(REQUIRED).type(STRING).name("last_name").tag(2).build();
    MessageElement element = MessageElement.builder()
        .name("Message")
        .addFields(Arrays.asList(firstName, lastName))
        .build();
    assertThat(element.fields()).hasSize(2);
  }

  @Test public void simpleWithDocumentationToSchema() {
    TypeElement element = MessageElement.builder()
        .name("Message")
        .documentation("Hello")
        .addField(FieldElement.builder().label(REQUIRED).type(STRING).name("name").tag(1).build())
        .build();
    String expected = ""
        + "// Hello\n"
        + "message Message {\n"
        + "  required string name = 1;\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void simpleWithOptionsToSchema() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name")
        .tag(1)
        .build();
    TypeElement element = MessageElement.builder()
        .name("Message")
        .addField(field)
        .addOption(OptionElement.create("kit", Kind.STRING, "kat"))
        .build();
    String expected = ""
        + "message Message {\n"
        + "  option kit = \"kat\";\n"
        + "\n"
        + "  required string name = 1;\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleOptions() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name")
        .tag(1)
        .build();
    OptionElement kitKat = OptionElement.create("kit", Kind.STRING, "kat");
    OptionElement fooBar = OptionElement.create("foo", Kind.STRING, "bar");
    MessageElement element = MessageElement.builder()
        .name("Message")
        .addField(field)
        .addOptions(Arrays.asList(kitKat, fooBar))
        .build();
    assertThat(element.options()).hasSize(2);
  }

  @Test public void simpleWithNestedElementsToSchema() {
    TypeElement element = MessageElement.builder()
        .name("Message")
        .addField(FieldElement.builder().label(REQUIRED).type(STRING).name("name").tag(1).build())
        .addType(MessageElement.builder()
            .name("Nested")
            .addField(
                FieldElement.builder().label(REQUIRED).type(STRING).name("name").tag(1).build())
            .build())
        .build();
    String expected = ""
        + "message Message {\n"
        + "  required string name = 1;\n"
        + "\n"
        + "  message Nested {\n"
        + "    required string name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleTypes() {
    TypeElement nested1 = MessageElement.builder().name("Nested1").build();
    TypeElement nested2 = MessageElement.builder().name("Nested2").build();
    TypeElement element = MessageElement.builder()
        .name("Message")
        .addField(FieldElement.builder()
            .label(REQUIRED)
            .type(STRING)
            .name("name")
            .tag(1)
            .build())
        .addTypes(Arrays.asList(nested1, nested2))
        .build();
    assertThat(element.nestedElements()).hasSize(2);
  }

  @Test public void simpleWithExtensionsToSchema() {
    TypeElement element = MessageElement.builder()
        .name("Message")
        .addField(FieldElement.builder()
            .label(REQUIRED)
            .type(STRING)
            .name("name")
            .tag(1)
            .build())
        .addExtensions(ExtensionsElement.create(500, 501))
        .build();
    String expected = ""
        + "message Message {\n"
        + "  required string name = 1;\n"
        + "\n"
        + "  extensions 500 to 501;\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleExtensions() {
    ExtensionsElement fives = ExtensionsElement.create(500, 501);
    ExtensionsElement sixes = ExtensionsElement.create(600, 601);
    MessageElement element = MessageElement.builder()
        .name("Message")
        .addField(FieldElement.builder()
            .label(REQUIRED)
            .type(STRING)
            .name("name")
            .tag(1)
            .build())
        .addExtensions(Arrays.asList(fives, sixes))
        .build();
    assertThat(element.extensions()).hasSize(2);
  }

  @Test public void oneOfToSchema() {
    TypeElement element = MessageElement.builder()
        .name("Message")
        .addOneOf(OneOfElement.builder()
            .name("hi")
            .addField(FieldElement.builder().label(ONE_OF).type(STRING).name("name").tag(1).build())
            .build())
        .build();
    String expected = ""
        + "message Message {\n"
        + "  oneof hi {\n"
        + "    string name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleOneOfs() {
    OneOfElement hi = OneOfElement.builder()
        .name("hi")
        .addField(FieldElement.builder().label(ONE_OF).type(STRING).name("name").tag(1).build())
        .build();
    OneOfElement hey = OneOfElement.builder()
        .name("hey")
        .addField(FieldElement.builder().label(ONE_OF).type(STRING).name("city").tag(2).build())
        .build();
    MessageElement element = MessageElement.builder()
        .name("Message")
        .addOneOfs(Arrays.asList(hi, hey))
        .build();
    assertThat(element.oneOfs()).hasSize(2);
  }

  @Test public void multipleEverythingToSchema() {
    FieldElement field1 = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name")
        .tag(1)
        .build();
    FieldElement field2 = FieldElement.builder()
        .label(REQUIRED)
        .type(BOOL)
        .name("other_name")
        .tag(2)
        .build();
    FieldElement oneOf1Field = FieldElement.builder()
        .label(ONE_OF)
        .type(STRING)
        .name("namey")
        .tag(3)
        .build();
    OneOfElement oneOf1 = OneOfElement.builder()
        .name("thingy")
        .addField(oneOf1Field)
        .build();
    FieldElement oneOf2Field = FieldElement.builder()
        .label(ONE_OF)
        .type(STRING)
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
    OptionElement option = OptionElement.create("kit", Kind.STRING, "kat");
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
        + "  required string name = 1;\n"
        + "  required bool other_name = 2;\n"
        + "\n"
        + "  oneof thingy {\n"
        + "    string namey = 3;\n"
        + "  }\n"
        + "  oneof thinger {\n"
        + "    string namer = 4;\n"
        + "  }\n"
        + "\n"
        + "  extensions 500 to 501;\n"
        + "  extensions 503;\n"
        + "\n"
        + "  message Nested {\n"
        + "    required string name = 1;\n"
        + "  }\n"
        + "}\n";
    assertThat(element.toSchema()).isEqualTo(expected);
  }

  @Test public void fieldToSchema() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name")
        .tag(1)
        .build();
    String expected = "required string name = 1;\n";
    assertThat(field.toSchema()).isEqualTo(expected);
  }

  @Test public void oneOfFieldToSchema() {
    FieldElement field = FieldElement.builder()
        .label(ONE_OF)
        .type(STRING)
        .name("name")
        .tag(1)
        .build();
    String expected = "string name = 1;\n";
    assertThat(field.toSchema()).isEqualTo(expected);
  }

  @Test public void fieldWithDocumentationToSchema() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name")
        .tag(1)
        .documentation("Hello")
        .build();
    String expected = ""
        + "// Hello\n"
        + "required string name = 1;\n";
    assertThat(field.toSchema()).isEqualTo(expected);
  }

  @Test public void fieldWithOptionsToSchema() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name")
        .tag(1)
        .addOption(OptionElement.create("kit", Kind.STRING, "kat"))
        .build();
    String expected = "required string name = 1 [\n"
        + "  kit = \"kat\"\n"
        + "];\n";
    assertThat(field.toSchema()).isEqualTo(expected);
  }

  @Test public void duplicateTagValueThrows() {
    FieldElement field1 = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name1")
        .tag(1)
        .build();
    FieldElement field2 = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
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
        .type(STRING)
        .name("name1")
        .tag(1)
        .build();
    FieldElement field2 = FieldElement.builder()
        .label(ONE_OF)
        .type(STRING)
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
        .type(STRING)
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
        .type(STRING)
        .name("name1")
        .tag(1)
        .addOption(OptionElement.create("deprecated", Kind.BOOLEAN, "true"))
        .build();
    assertThat(field.isDeprecated()).isTrue();
  }

  @Test public void deprecatedFalse() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name1")
        .tag(1)
        .addOption(OptionElement.create("deprecated", Kind.BOOLEAN, "false"))
        .build();
    assertThat(field.isDeprecated()).isFalse();
  }

  @Test public void deprecatedMissing() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name1")
        .tag(1)
        .build();
    assertThat(field.isDeprecated()).isFalse();
  }

  @Test public void packedTrue() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name1")
        .tag(1)
        .addOption(OptionElement.create("packed", Kind.BOOLEAN, "true"))
        .build();
    assertThat(field.isPacked()).isTrue();
  }

  @Test public void packedFalse() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name1")
        .tag(1)
        .addOption(OptionElement.create("packed", Kind.BOOLEAN, "false"))
        .build();
    assertThat(field.isPacked()).isFalse();
  }

  @Test public void packedMissing() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name1")
        .tag(1)
        .build();
    assertThat(field.isPacked()).isFalse();
  }

  @Test public void defaultValue() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name1")
        .tag(1)
        .addOption(OptionElement.create("default", Kind.STRING, "foo"))
        .build();
    assertThat(field.getDefault().value()).isEqualTo("foo");
  }

  @Test public void defaultMissing() {
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name1")
        .tag(1)
        .build();
    assertThat(field.getDefault()).isNull();
  }
}
