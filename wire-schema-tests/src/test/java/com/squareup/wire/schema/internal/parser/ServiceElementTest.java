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
package com.squareup.wire.schema.internal.parser;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.internal.parser.OptionElement.Kind;
import com.squareup.wire.schema.Location;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class ServiceElementTest {
  Location location = Location.get("file.proto");

  @Test public void emptyToSchema() {
    ServiceElement service = ServiceElement.builder(location).name("Service").build();
    String expected = "service Service {}\n";
    assertThat(service.toSchema()).isEqualTo(expected);
  }

  @Test public void singleToSchema() {
    ServiceElement service = ServiceElement.builder(location)
        .name("Service")
        .rpcs(ImmutableList.of(
            RpcElement.builder(location)
                .name("Name")
                .requestType("RequestType")
                .responseType("ResponseType")
                .build()))
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
        .rpcs(ImmutableList.of(firstName, lastName))
        .build();
    assertThat(service.rpcs()).hasSize(2);
  }

  @Test public void singleWithOptionsToSchema() {
    ServiceElement service = ServiceElement.builder(location)
        .name("Service")
        .options(ImmutableList.of(
            OptionElement.create("foo", Kind.STRING, "bar")))
        .rpcs(ImmutableList.of(
            RpcElement.builder(location)
                .name("Name")
                .requestType("RequestType")
                .responseType("ResponseType")
                .build()))
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
        .options(ImmutableList.of(kitKat, fooBar))
        .rpcs(ImmutableList.of(
            RpcElement.builder(location)
                .name("Name")
                .requestType("RequestType")
                .responseType("ResponseType")
                .build()))
        .build();
    assertThat(service.options()).hasSize(2);
  }

  @Test public void singleWithDocumentationToSchema() {
    ServiceElement service = ServiceElement.builder(location)
        .name("Service")
        .documentation("Hello")
        .rpcs(ImmutableList.of(
            RpcElement.builder(location)
                .name("Name")
                .requestType("RequestType")
                .responseType("ResponseType")
                .build()))
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
    ServiceElement service = ServiceElement.builder(location)
        .name("Service")
        .rpcs(ImmutableList.of(rpc, rpc))
        .build();
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
        .options(ImmutableList.of(
            OptionElement.create("foo", Kind.STRING, "bar")))
        .build();
    String expected = ""
        + "rpc Name (RequestType) returns (ResponseType) {\n"
        + "  option foo = \"bar\";\n"
        + "};\n";
    assertThat(rpc.toSchema()).isEqualTo(expected);
  }

  @Test public void rpcWithRequestStreamingToSchema() {
    RpcElement rpc = RpcElement.builder(location)
            .name("Name")
            .requestType("RequestType")
            .responseType("ResponseType")
            .requestStreaming(true)
            .build();
    String expected = "rpc Name (stream RequestType) returns (ResponseType);\n";
    assertThat(rpc.toSchema()).isEqualTo(expected);
  }

  @Test public void rpcWithResponseStreamingToSchema() {
    RpcElement rpc = RpcElement.builder(location)
            .name("Name")
            .requestType("RequestType")
            .responseType("ResponseType")
            .responseStreaming(true)
            .build();
    String expected = "rpc Name (RequestType) returns (stream ResponseType);\n";
    assertThat(rpc.toSchema()).isEqualTo(expected);
  }
}
