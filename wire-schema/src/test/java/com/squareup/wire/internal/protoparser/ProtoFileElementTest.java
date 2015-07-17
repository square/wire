/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.internal.protoparser;

import com.squareup.wire.internal.protoparser.OptionElement.Kind;
import com.squareup.wire.schema.Location;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static com.squareup.wire.internal.protoparser.ProtoFileElement.MAX_TAG_VALUE;
import static com.squareup.wire.internal.protoparser.ProtoFileElement.MIN_TAG_VALUE;
import static com.squareup.wire.internal.protoparser.ProtoFileElement.Syntax.PROTO_2;
import static com.squareup.wire.internal.protoparser.ProtoFileElement.isValidTag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ProtoFileElementTest {
  Location location = Location.get("file.proto");

  @Test public void nullBuilderValuesThrow() {
    try {
      ProtoFileElement.builder((Location) null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("location");
    }
    try {
      ProtoFileElement.builder(location).packageName(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("packageName");
    }
    try {
      ProtoFileElement.builder(location).syntax(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("syntax");
    }
    try {
      ProtoFileElement.builder(location).addDependency(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("dependency");
    }
    try {
      ProtoFileElement.builder(location).addDependencies(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("dependencies");
    }
    try {
      ProtoFileElement.builder(location).addDependencies(Collections.<String>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("dependency");
    }
    try {
      ProtoFileElement.builder(location).addPublicDependency(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("dependency");
    }
    try {
      ProtoFileElement.builder(location).addPublicDependencies(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("dependencies");
    }
    try {
      ProtoFileElement.builder(location)
          .addPublicDependencies(Collections.<String>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("dependency");
    }
    try {
      ProtoFileElement.builder(location).addType(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("type");
    }
    try {
      ProtoFileElement.builder(location).addTypes(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("types");
    }
    try {
      ProtoFileElement.builder(location).addTypes(Collections.<TypeElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("type");
    }
    try {
      ProtoFileElement.builder(location).addService(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("service");
    }
    try {
      ProtoFileElement.builder(location).addServices(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("services");
    }
    try {
      ProtoFileElement.builder(location)
          .addServices(Collections.<ServiceElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("service");
    }
    try {
      ProtoFileElement.builder(location).addExtendDeclaration(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("extend");
    }
    try {
      ProtoFileElement.builder(location).addExtendDeclarations(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("extendDeclarations");
    }
    try {
      ProtoFileElement.builder(location).addExtendDeclarations(
          Collections.<ExtendElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("extend");
    }
    try {
      ProtoFileElement.builder(location).addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
    }
    try {
      ProtoFileElement.builder(location).addOptions(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("options");
    }
    try {
      ProtoFileElement.builder(location)
          .addOptions(Collections.<OptionElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
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

  @Test public void emptyToSchema() {
    ProtoFileElement file = ProtoFileElement.builder(location).build();
    String expected = "// file.proto\n";
    assertThat(file.toSchema()).isEqualTo(expected);
  }

  @Test public void emptyWithPackageToSchema() {
    ProtoFileElement file = ProtoFileElement.builder(location)
        .packageName("example.simple")
        .build();
    String expected = ""
        + "// file.proto\n"
        + "package example.simple;\n";
    assertThat(file.toSchema()).isEqualTo(expected);
  }

  @Test public void simpleToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    ProtoFileElement file = ProtoFileElement.builder(location).addType(element).build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toSchema()).isEqualTo(expected);
  }

  @Test public void simpleWithImportsToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    ProtoFileElement file = ProtoFileElement.builder(location)
        .addDependency("example.other")
        .addType(element)
        .build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "import \"example.other\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleDependencies() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    ProtoFileElement file = ProtoFileElement.builder(location)
        .addDependencies(Arrays.asList("example.other", "example.another"))
        .addType(element)
        .build();
    assertThat(file.dependencies()).hasSize(2);
  }

  @Test public void simpleWithPublicImportsToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    ProtoFileElement file = ProtoFileElement.builder(location)
        .addPublicDependency("example.other")
        .addType(element)
        .build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "import public \"example.other\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultiplePublicDependencies() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    ProtoFileElement file = ProtoFileElement.builder(location)
        .addPublicDependencies(Arrays.asList("example.other", "example.another"))
        .addType(element)
        .build();
    assertThat(file.publicDependencies()).hasSize(2);
  }

  @Test public void simpleWithBothImportsToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    ProtoFileElement file = ProtoFileElement.builder(location)
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
    assertThat(file.toSchema()).isEqualTo(expected);
  }

  @Test public void simpleWithServicesToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    ServiceElement service = ServiceElement.builder(location).name("Service").build();
    ProtoFileElement
        file = ProtoFileElement.builder(location).addType(element).addService(service).build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "message Message {}\n"
        + "\n"
        + "service Service {}\n";
    assertThat(file.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleServices() {
    ServiceElement service1 = ServiceElement.builder(location).name("Service1").build();
    ServiceElement service2 = ServiceElement.builder(location).name("Service2").build();
    ProtoFileElement file = ProtoFileElement.builder(location)
        .addServices(Arrays.asList(service1, service2))
        .build();
    assertThat(file.services()).hasSize(2);
  }

  @Test public void simpleWithOptionsToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    OptionElement option = OptionElement.create("kit", Kind.STRING, "kat");
    ProtoFileElement
        file = ProtoFileElement.builder(location).addOption(option).addType(element).build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "option kit = \"kat\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleOptions() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    OptionElement kitKat = OptionElement.create("kit", Kind.STRING, "kat");
    OptionElement fooBar = OptionElement.create("foo", Kind.STRING, "bar");
    ProtoFileElement file = ProtoFileElement.builder(location)
        .addOptions(Arrays.asList(kitKat, fooBar))
        .addType(element)
        .build();
    assertThat(file.options()).hasSize(2);
  }

  @Test public void simpleWithExtendsToSchema() {
    ProtoFileElement file = ProtoFileElement.builder(location)
        .addExtendDeclaration(ExtendElement.builder(location.at(5, 1)).name("Extend").build())
        .addType(MessageElement.builder(location).name("Message").build())
        .build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "message Message {}\n"
        + "\n"
        + "extend Extend {}\n";
    assertThat(file.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleExtends() {
    ExtendElement extend1 = ExtendElement.builder(location).name("Extend1").build();
    ExtendElement extend2 = ExtendElement.builder(location).name("Extend2").build();
    ProtoFileElement file = ProtoFileElement.builder(location)
        .addExtendDeclarations(Arrays.asList(extend1, extend2))
        .build();
    assertThat(file.extendDeclarations()).hasSize(2);
  }

  @Test public void multipleEverythingToSchema() {
    TypeElement element1 = MessageElement.builder(location.at(10, 1))
        .name("Message1")
        .qualifiedName("example.simple.Message1")
        .build();
    TypeElement element2 = MessageElement.builder(location.at(11, 1))
        .name("Message2")
        .qualifiedName("example.simple.Message2")
        .build();
    ExtendElement extend1 = ExtendElement.builder(location.at(13, 1))
        .name("Extend1")
        .qualifiedName("example.simple.Extend1")
        .build();
    ExtendElement extend2 = ExtendElement.builder(location.at(14, 1))
        .name("Extend2")
        .qualifiedName("example.simple.Extend2")
        .build();
    OptionElement option1 = OptionElement.create("kit", Kind.STRING, "kat");
    OptionElement option2 = OptionElement.create("foo", Kind.STRING, "bar");
    ServiceElement service1 = ServiceElement.builder(location.at(16, 1))
        .name("Service1")
        .qualifiedName("example.simple.Service1")
        .build();
    ServiceElement service2 = ServiceElement.builder(location.at(17, 1))
        .name("Service2")
        .qualifiedName("example.simple.Service2")
        .build();
    ProtoFileElement file = ProtoFileElement.builder(location)
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
    assertThat(file.toSchema()).isEqualTo(expected);

    // Re-parse the expected string into a ProtoFile and ensure they're equal.
    ProtoFileElement parsed = ProtoParser.parse(location, expected);
    assertThat(parsed).isEqualTo(file);
  }

  @Test public void syntaxToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    ProtoFileElement
        file = ProtoFileElement.builder(location).syntax(PROTO_2).addType(element).build();
    String expected = ""
        + "// file.proto\n"
        + "syntax \"proto2\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toSchema()).isEqualTo(expected);
  }
}
