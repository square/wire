/*
 * Copyright (C) 2014 Square, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ServiceElementTest {
  Location location = Location.get("file.proto");

  @Test public void locationRequired() {
    try {
      ServiceElement.builder(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("location");
    }
  }

  @Test public void nameRequired() {
    try {
      ServiceElement.builder(location).qualifiedName("Test").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
  }

  @Test public void nameSetsQualifiedName() {
    ServiceElement test = ServiceElement.builder(location).name("Test").build();
    assertThat(test.name()).isEqualTo("Test");
    assertThat(test.qualifiedName()).isEqualTo("Test");
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      ServiceElement.builder(location).name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
    try {
      ServiceElement.builder(location).qualifiedName(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("qualifiedName");
    }
    try {
      ServiceElement.builder(location).documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation");
    }
    try {
      ServiceElement.builder(location).addRpc(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("rpc");
    }
    try {
      ServiceElement.builder(location).addRpcs(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("rpcs");
    }
    try {
      ServiceElement.builder(location).addRpcs(Collections.<RpcElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("rpc");
    }
    try {
      ServiceElement.builder(location).addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
    }
    try {
      ServiceElement.builder(location).addOptions(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("options");
    }
    try {
      ServiceElement.builder(location).addOptions(Collections.<OptionElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
    }
  }

  @Test public void emptyToSchema() {
    ServiceElement service = ServiceElement.builder(location).name("Service").build();
    String expected = "service Service {}\n";
    assertThat(service.toSchema()).isEqualTo(expected);
  }

  @Test public void singleToSchema() {
    ServiceElement service = ServiceElement.builder(location)
        .name("Service")
        .addRpc(RpcElement.builder(location)
            .name("Name")
            .requestType("RequestType")
            .responseType("ResponseType")
            .build())
        .build();
    String expected = ""
        + "service Service {\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleRpcs() {
    RpcElement firstName = RpcElement.builder(location)
        .name("FirstName")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build();
    RpcElement lastName = RpcElement.builder(location)
        .name("LastName")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build();
    ServiceElement service = ServiceElement.builder(location)
        .name("Service")
        .addRpcs(Arrays.asList(firstName, lastName))
        .build();
    assertThat(service.rpcs()).hasSize(2);
  }

  @Test public void singleWithOptionsToSchema() {
    ServiceElement service = ServiceElement.builder(location)
        .name("Service")
        .addOption(OptionElement.create("foo", Kind.STRING, "bar"))
        .addRpc(RpcElement.builder(location)
            .name("Name")
            .requestType("RequestType")
            .responseType("ResponseType")
            .build())
        .build();
    String expected = ""
        + "service Service {\n"
        + "  option foo = \"bar\";\n"
        + "\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleOptions() {
    OptionElement kitKat = OptionElement.create("kit", Kind.STRING, "kat");
    OptionElement fooBar = OptionElement.create("foo", Kind.STRING, "bar");
    ServiceElement service = ServiceElement.builder(location)
        .name("Service")
        .addOptions(Arrays.asList(kitKat, fooBar))
        .addRpc(RpcElement.builder(location)
            .name("Name")
            .requestType("RequestType")
            .responseType("ResponseType")
            .build())
        .build();
    assertThat(service.options()).hasSize(2);
  }

  @Test public void singleWithDocumentationToSchema() {
    ServiceElement service = ServiceElement.builder(location)
        .name("Service")
        .documentation("Hello")
        .addRpc(RpcElement.builder(location)
            .name("Name")
            .requestType("RequestType")
            .responseType("ResponseType")
            .build())
        .build();
    String expected = ""
        + "// Hello\n"
        + "service Service {\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toSchema()).isEqualTo(expected);
  }

  @Test public void multipleToSchema() {
    RpcElement rpc = RpcElement.builder(location)
        .name("Name")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build();
    ServiceElement service =
        ServiceElement.builder(location).name("Service").addRpc(rpc).addRpc(rpc).build();
    String expected = ""
        + "service Service {\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toSchema()).isEqualTo(expected);
  }

  @Test public void rpcToSchema() {
    RpcElement rpc = RpcElement.builder(location)
        .name("Name")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build();
    String expected = "rpc Name (RequestType) returns (ResponseType);\n";
    assertThat(rpc.toSchema()).isEqualTo(expected);
  }

  @Test public void rpcWithDocumentationToSchema() {
    RpcElement rpc = RpcElement.builder(location)
        .name("Name")
        .documentation("Hello")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build();
    String expected = ""
        + "// Hello\n"
        + "rpc Name (RequestType) returns (ResponseType);\n";
    assertThat(rpc.toSchema()).isEqualTo(expected);
  }

  @Test public void rpcWithOptionsToSchema() {
    RpcElement rpc = RpcElement.builder(location)
        .name("Name")
        .requestType("RequestType")
        .responseType("ResponseType")
        .addOption(OptionElement.create("foo", Kind.STRING, "bar"))
        .build();
    String expected = ""
        + "rpc Name (RequestType) returns (ResponseType) {\n"
        + "  option foo = \"bar\";\n"
        + "};\n";
    assertThat(rpc.toSchema()).isEqualTo(expected);
  }
}
