/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.schema;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.internal.parser.ExtendElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.OptionElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.RpcElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;
import com.squareup.wire.schema.internal.parser.TypeElement;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtoFileTest {
  Location location = Location.get("file.proto");

  @Test public void roundTripToElement() {
    TypeElement element1 = MessageElement.builder(location.at(11, 1))
        .name("Message1")
        .documentation("Some comments about Message1")
        .build();
    TypeElement element2 = MessageElement.builder(location.at(12, 1))
        .name("Message2")
        .fields(ImmutableList.<FieldElement>of(FieldElement.builder(location.at(13, 3))
            .type("string")
            .name("field")
            .tag(1)
            .build()))
        .build();
    ExtendElement extend1 = ExtendElement.builder(location.at(16, 1))
        .name("Extend1")
        .build();
    ExtendElement extend2 = ExtendElement.builder(location.at(17, 1))
        .name("Extend2")
        .build();
    OptionElement option1 = OptionElement.create("kit", OptionElement.Kind.STRING, "kat");
    OptionElement option2 = OptionElement.create("foo", OptionElement.Kind.STRING, "bar");
    ServiceElement service1 = ServiceElement.builder(location.at(19, 1))
        .name("Service1")
        .rpcs(ImmutableList.<RpcElement>of(RpcElement.builder(location.at(20, 3))
            .name("MethodA")
            .requestType("Message2")
            .responseType("Message1")
            .options(ImmutableList.<OptionElement>of(
                OptionElement.create("methodoption", OptionElement.Kind.NUMBER, 1)))
            .build()))
        .build();
    ServiceElement service2 = ServiceElement.builder(location.at(24, 1))
        .name("Service2")
        .build();
    ProtoFileElement fileElement = ProtoFileElement.builder(location)
        .packageName("example.simple")
        .imports(ImmutableList.of("example.thing"))
        .publicImports(ImmutableList.of("example.other"))
        .types(ImmutableList.of(element1, element2))
        .services(ImmutableList.of(service1, service2))
        .extendDeclarations(ImmutableList.of(extend1, extend2))
        .options(ImmutableList.of(option1, option2))
        .build();
    ProtoFile file = ProtoFile.get(fileElement);

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
        + "// Some comments about Message1\n"
        + "message Message1 {}\n"
        + "message Message2 {\n"
        + "  string field = 1;\n"
        + "}\n"
        + "\n"
        + "extend Extend1 {}\n"
        + "extend Extend2 {}\n"
        + "\n"
        + "service Service1 {\n"
        + "  rpc MethodA (Message2) returns (Message1) {\n"
        + "    option methodoption = 1;\n"
        + "  };\n"
        + "}\n"
        + "service Service2 {}\n";
    assertThat(file.toElement().toSchema()).isEqualTo(expected);
    assertThat(file.toElement()).isEqualToComparingFieldByField(fileElement);
  }
}