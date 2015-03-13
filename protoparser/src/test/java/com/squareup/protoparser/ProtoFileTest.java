package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.ProtoFile.MAX_TAG_VALUE;
import static com.squareup.protoparser.ProtoFile.MIN_TAG_VALUE;
import static com.squareup.protoparser.ProtoFile.Syntax.PROTO_2;
import static com.squareup.protoparser.ProtoFile.isValidTag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ProtoFileTest {
  @Test public void nullBuilderValuesThrow() {
    try {
      ProtoFile.builder(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("filePath == null");
    }
    try {
      ProtoFile.builder("test.proto").packageName(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("packageName == null");
    }
    try {
      ProtoFile.builder("test.proto").syntax(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("syntax == null");
    }
    try {
      ProtoFile.builder("test.proto").addDependency(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("dependency == null");
    }
    try {
      ProtoFile.builder("test.proto").addPublicDependency(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("dependency == null");
    }
    try {
      ProtoFile.builder("test.proto").addType(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("type == null");
    }
    try {
      ProtoFile.builder("test.proto").addService(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("service == null");
    }
    try {
      ProtoFile.builder("test.proto").addExtendDeclaration(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("extend == null");
    }
    try {
      ProtoFile.builder("test.proto").addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option == null");
    }
  }

  @Test public void tagValueValidation() {
    assertThat(isValidTag(MIN_TAG_VALUE - 1)).isFalse(); // Less than minimum.
    assertThat(isValidTag(MIN_TAG_VALUE)).isTrue();
    assertThat(isValidTag(1234)).isTrue();
    assertThat(isValidTag(19222)).isFalse(); // Reserved range.
    assertThat(isValidTag(2319573)).isTrue();
    assertThat(isValidTag(MAX_TAG_VALUE)).isTrue();
    assertThat(isValidTag(MAX_TAG_VALUE + 1)).isFalse(); // Greater than maximum.
  }

  @Test public void emptyToString() {
    ProtoFile file = ProtoFile.builder("file.proto").build();
    String expected = "// file.proto\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void emptyWithPackageToString() {
    ProtoFile file = ProtoFile.builder("file.proto").packageName("example.simple").build();
    String expected = ""
        + "// file.proto\n"
        + "package example.simple;\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleToString() {
    TypeElement element = MessageElement.builder().name("Message").build();
    ProtoFile file = ProtoFile.builder("file.proto").addType(element).build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithImportsToString() {
    TypeElement element = MessageElement.builder().name("Message").build();
    ProtoFile file =
        ProtoFile.builder("file.proto").addDependency("example.other").addType(element).build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "import \"example.other\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithPublicImportsToString() {
    TypeElement element = MessageElement.builder().name("Message").build();
    ProtoFile file = ProtoFile.builder("file.proto")
        .addPublicDependency("example.other")
        .addType(element)
        .build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "import public \"example.other\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithBothImportsToString() {
    TypeElement element = MessageElement.builder().name("Message").build();
    ProtoFile file = ProtoFile.builder("file.proto")
        .addDependency("example.thing")
        .addPublicDependency("example.other")
        .addType(element)
        .build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "import \"example.thing\";\n"
        + "import public \"example.other\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithServicesToString() {
    TypeElement element = MessageElement.builder().name("Message").build();
    ServiceElement service = ServiceElement.builder().name("Service").build();
    ProtoFile file = ProtoFile.builder("file.proto").addType(element).addService(service).build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "message Message {}\n"
        + "\n"
        + "service Service {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithOptionsToString() {
    TypeElement element = MessageElement.builder().name("Message").build();
    OptionElement option = OptionElement.create("kit", "kat", false);
    ProtoFile file = ProtoFile.builder("file.proto").addOption(option).addType(element).build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "option kit = \"kat\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithExtendsToString() {
    ProtoFile file = ProtoFile.builder("file.proto")
        .addExtendDeclaration(ExtendElement.builder().name("Extend").build())
        .addType(MessageElement.builder().name("Message").build())
        .build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "message Message {}\n"
        + "\n"
        + "extend Extend {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void multipleEverythingToString() {
    TypeElement element1 = MessageElement.builder()
        .name("Message1")
        .qualifiedName("example.simple.Message1")
        .build();
    TypeElement element2 = MessageElement.builder()
        .name("Message2")
        .qualifiedName("example.simple.Message2")
        .build();
    ExtendElement extend1 = ExtendElement.builder()
        .name("Extend1")
        .qualifiedName("example.simple.Extend1")
        .build();
    ExtendElement extend2 = ExtendElement.builder()
        .name("Extend2")
        .qualifiedName("example.simple.Extend2")
        .build();
    OptionElement option1 = OptionElement.create("kit", "kat", false);
    OptionElement option2 = OptionElement.create("foo", "bar", false);
    ServiceElement service1 = ServiceElement.builder()
        .name("Service1")
        .qualifiedName("example.simple.Service1")
        .build();
    ServiceElement service2 = ServiceElement.builder()
        .name("Service2")
        .qualifiedName("example.simple.Service2")
        .build();
    ProtoFile file = ProtoFile.builder("file.proto")
        .packageName("example.simple")
        .addDependency("example.thing")
        .addPublicDependency("example.other")
        .addType(element1)
        .addType(element2)
        .addService(service1)
        .addService(service2)
        .addExtendDeclaration(extend1)
        .addExtendDeclaration(extend2)
        .addOption(option1)
        .addOption(option2)
        .build();
    String expected = ""
        + "// file.proto\n"
        + "package example.simple;\n"
        + "\n"
        + "import \"example.thing\";\n"
        + "import public \"example.other\";\n"
        + "\n"
        + "option kit = \"kat\";\n"
        + "option foo = \"bar\";\n"
        + "\n"
        + "message Message1 {}\n"
        + "message Message2 {}\n"
        + "\n"
        + "extend Extend1 {}\n"
        + "extend Extend2 {}\n"
        + "\n"
        + "service Service1 {}\n"
        + "service Service2 {}\n";
    assertThat(file.toString()).isEqualTo(expected);

    // Re-parse the expected string into a ProtoFile and ensure they're equal.
    ProtoFile parsed = ProtoParser.parse("file.proto", expected);
    assertThat(parsed).isEqualTo(file);
  }

  @Test public void syntaxToString() {
    TypeElement element = MessageElement.builder().name("Message").build();
    ProtoFile file = ProtoFile.builder("file.proto").syntax(PROTO_2).addType(element).build();
    String expected = ""
        + "// file.proto\n"
        + "syntax \"proto2\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }
}
