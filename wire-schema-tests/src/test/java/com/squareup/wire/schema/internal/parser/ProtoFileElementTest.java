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
package com.squareup.wire.schema.internal.parser;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.internal.parser.OptionElement.Kind;
import com.squareup.wire.schema.Location;
import org.junit.Test;

import static com.squareup.wire.schema.ProtoFile.Syntax.PROTO_2;
import static org.assertj.core.api.Assertions.assertThat;

public final class ProtoFileElementTest {
  Location location = Location.get("file.proto");

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
    ProtoFileElement file = ProtoFileElement.builder(location)
        .types(ImmutableList.of(element))
        .build();
    String expected = ""
        + "// file.proto\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toSchema()).isEqualTo(expected);
  }

  @Test public void simpleWithImportsToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    ProtoFileElement file = ProtoFileElement.builder(location)
        .imports(ImmutableList.of("example.other"))
        .types(ImmutableList.of(element))
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
        .imports(ImmutableList.of("example.other", "example.another"))
        .types(ImmutableList.of(element))
        .build();
    assertThat(file.imports()).hasSize(2);
  }

  @Test public void simpleWithPublicImportsToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    ProtoFileElement file = ProtoFileElement.builder(location)
        .publicImports(ImmutableList.of("example.other"))
        .types(ImmutableList.of(element))
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
        .publicImports(ImmutableList.of("example.other", "example.another"))
        .types(ImmutableList.of(element))
        .build();
    assertThat(file.publicImports()).hasSize(2);
  }

  @Test public void simpleWithBothImportsToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    ProtoFileElement file = ProtoFileElement.builder(location)
        .imports(ImmutableList.of("example.thing"))
        .publicImports(ImmutableList.of("example.other"))
        .types(ImmutableList.of(element))
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
    ProtoFileElement file = ProtoFileElement.builder(location)
        .types(ImmutableList.of(element))
        .services(ImmutableList.of(service))
        .build();
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
        .services(ImmutableList.of(service1, service2))
        .build();
    assertThat(file.services()).hasSize(2);
  }

  @Test public void simpleWithOptionsToSchema() {
    TypeElement element = MessageElement.builder(location).name("Message").build();
    OptionElement option = OptionElement.create("kit", Kind.STRING, "kat");
    ProtoFileElement file = ProtoFileElement.builder(location)
        .options(ImmutableList.of(option))
        .types(ImmutableList.of(element))
        .build();
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
        .options(ImmutableList.of(kitKat, fooBar))
        .types(ImmutableList.of(element))
        .build();
    assertThat(file.options()).hasSize(2);
  }

  @Test public void simpleWithExtendsToSchema() {
    ProtoFileElement file = ProtoFileElement.builder(location)
        .extendDeclarations(ImmutableList.of(
            ExtendElement.builder(location.at(5, 1)).name("Extend").build()))
        .types(ImmutableList.<TypeElement>of(
            MessageElement.builder(location)
                .name("Message")
                .build()))
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
        .extendDeclarations(ImmutableList.of(extend1, extend2))
        .build();
    assertThat(file.extendDeclarations()).hasSize(2);
  }

  @Test public void multipleEverythingToSchema() {
    TypeElement element1 = MessageElement.builder(location.at(10, 1))
        .name("Message1")
        .build();
    TypeElement element2 = MessageElement.builder(location.at(11, 1))
        .name("Message2")
        .build();
    ExtendElement extend1 = ExtendElement.builder(location.at(13, 1))
        .name("Extend1")
        .build();
    ExtendElement extend2 = ExtendElement.builder(location.at(14, 1))
        .name("Extend2")
        .build();
    OptionElement option1 = OptionElement.create("kit", Kind.STRING, "kat");
    OptionElement option2 = OptionElement.create("foo", Kind.STRING, "bar");
    ServiceElement service1 = ServiceElement.builder(location.at(16, 1))
        .name("Service1")
        .build();
    ServiceElement service2 = ServiceElement.builder(location.at(17, 1))
        .name("Service2")
        .build();
    ProtoFileElement file = ProtoFileElement.builder(location)
        .packageName("example.simple")
        .imports(ImmutableList.of("example.thing"))
        .publicImports(ImmutableList.of("example.other"))
        .types(ImmutableList.of(element1, element2))
        .services(ImmutableList.of(service1, service2))
        .extendDeclarations(ImmutableList.of(extend1, extend2))
        .options(ImmutableList.of(option1, option2))
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
    ProtoFileElement file = ProtoFileElement.builder(location)
        .syntax(PROTO_2)
        .types(ImmutableList.of(element))
        .build();
    String expected = ""
        + "// file.proto\n"
        + "syntax \"proto2\";\n"
        + "\n"
        + "message Message {}\n";
    assertThat(file.toSchema()).isEqualTo(expected);
  }
}
