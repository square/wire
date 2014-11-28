package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.ProtoFile.MAX_TAG_VALUE;
import static com.squareup.protoparser.ProtoFile.MIN_TAG_VALUE;
import static com.squareup.protoparser.ProtoFile.isValidTag;
import static com.squareup.protoparser.TestUtils.NO_EXTEND_DECLARATIONS;
import static com.squareup.protoparser.TestUtils.NO_EXTENSIONS;
import static com.squareup.protoparser.TestUtils.NO_FIELDS;
import static com.squareup.protoparser.TestUtils.NO_METHODS;
import static com.squareup.protoparser.TestUtils.NO_OPTIONS;
import static com.squareup.protoparser.TestUtils.NO_SERVICES;
import static com.squareup.protoparser.TestUtils.NO_STRINGS;
import static com.squareup.protoparser.TestUtils.NO_TYPES;
import static com.squareup.protoparser.TestUtils.list;
import static org.fest.assertions.api.Assertions.assertThat;

public class ProtoFileTest {
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
    ProtoFile file =
        new ProtoFile("file.proto", null, NO_STRINGS, NO_STRINGS, NO_TYPES, NO_SERVICES, NO_OPTIONS,
            NO_EXTEND_DECLARATIONS);
    String expected = "// file.proto\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void emptyWithPackageToString() {
    ProtoFile file =
        new ProtoFile("file.proto", "example.simple", NO_STRINGS, NO_STRINGS, NO_TYPES, NO_SERVICES, NO_OPTIONS,
            NO_EXTEND_DECLARATIONS);
    String expected = ""
        + "// file.proto\n"
        + "package example.simple;\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleToString() {
    TypeElement element =
        new MessageElement("Message", "", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile file =
        new ProtoFile("file.proto", null, NO_STRINGS, NO_STRINGS, list(element),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithImportsToString() {
    TypeElement element =
        new MessageElement("Message", "", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile file =
        new ProtoFile("file.proto", null, list("example.other"), NO_STRINGS, list(element),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "import \"example.other\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithPublicImportsToString() {
    TypeElement element =
        new MessageElement("Message", "", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile file =
        new ProtoFile("file.proto", null, NO_STRINGS, list("example.other"), list(element),
            NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "import public \"example.other\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithBothImportsToString() {
    TypeElement element =
        new MessageElement("Message", "", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile file =
        new ProtoFile("file.proto", null, list("example.thing"), list("example.other"),
            list(element), NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
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
    TypeElement element =
        new MessageElement("Message", "", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ServiceElement service = new ServiceElement("Service", "", "", NO_OPTIONS, NO_METHODS);
    ProtoFile file =
        new ProtoFile("file.proto", null, NO_STRINGS, NO_STRINGS, list(element),
            list(service), NO_OPTIONS, NO_EXTEND_DECLARATIONS);
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "message Message {}\n"
        + "\n"
        + "service Service {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithOptionsToString() {
    TypeElement element =
        new MessageElement("Message", "", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    OptionElement option = new OptionElement("kit", "kat", false);
    ProtoFile file =
        new ProtoFile("file.proto", null, NO_STRINGS, NO_STRINGS, list(element),
            NO_SERVICES, list(option), NO_EXTEND_DECLARATIONS);
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "option kit = \"kat\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void simpleWithExtendsToString() {
    TypeElement element =
        new MessageElement("Message", "", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ExtendElement extend = new ExtendElement("Extend", "Extend", "", NO_FIELDS);
    ProtoFile file =
        new ProtoFile("file.proto", null, NO_STRINGS, NO_STRINGS, list(element),
            NO_SERVICES, NO_OPTIONS, list(extend));
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "message Message {}\n"
        + "\n"
        + "extend Extend {}\n";
    assertThat(file.toString()).isEqualTo(expected);
  }

  @Test public void multipleEverythingToString() {
    TypeElement element1 =
        new MessageElement("Message1", "example.simple.Message1", "", NO_FIELDS, NO_TYPES,
            NO_EXTENSIONS, NO_OPTIONS);
    TypeElement element2 =
        new MessageElement("Message2", "example.simple.Message2", "", NO_FIELDS, NO_TYPES,
            NO_EXTENSIONS, NO_OPTIONS);
    ExtendElement extend1 =
        new ExtendElement("Extend1", "example.simple.Extend1", "", NO_FIELDS);
    ExtendElement extend2 =
        new ExtendElement("Extend2", "example.simple.Extend2", "", NO_FIELDS);
    OptionElement option1 = new OptionElement("kit", "kat", false);
    OptionElement option2 = new OptionElement("foo", "bar", false);
    ServiceElement service1 =
        new ServiceElement("Service1", "example.simple.Service1", "", NO_OPTIONS, NO_METHODS);
    ServiceElement service2 =
        new ServiceElement("Service2", "example.simple.Service2", "", NO_OPTIONS, NO_METHODS);
    ProtoFile file =
        new ProtoFile("file.proto", "example.simple", list("example.thing"), list("example.other"),
            list(element1, element2), list(service1, service2), list(option1, option2),
            list(extend1, extend2));
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
    ProtoFile parsed = ProtoSchemaParser.parse("file.proto", expected);
    assertThat(parsed).isEqualTo(file);
  }
}
