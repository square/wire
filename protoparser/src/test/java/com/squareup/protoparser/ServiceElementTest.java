package com.squareup.protoparser;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceElementTest {
  @Test public void emptyToString() {
    ServiceElement service = ServiceElement.builder().name("Service").build();
    String expected = "service Service {}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void singleToString() {
    ServiceElement service = ServiceElement.builder()
        .name("Service")
        .addRpc(RpcElement.builder()
            .name("Name")
            .requestType("RequestType")
            .responseType("ResponseType")
            .build())
        .build();
    String expected = ""
        + "service Service {\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void singleWithOptionsToString() {
    ServiceElement service = ServiceElement.builder()
        .name("Service")
        .addOption(OptionElement.create("foo", "bar", false))
        .addRpc(RpcElement.builder()
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
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void singleWithDocumentation() {
    ServiceElement service = ServiceElement.builder()
        .name("Service")
        .documentation("Hello")
        .addRpc(RpcElement.builder()
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
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void multipleToString() {
    RpcElement rpc = RpcElement.builder()
        .name("Name")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build();
    ServiceElement service =
        ServiceElement.builder().name("Service").addRpc(rpc).addRpc(rpc).build();
    String expected = ""
        + "service Service {\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void rpcToString() {
    RpcElement rpc = RpcElement.builder()
        .name("Name")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build();
    String expected = "rpc Name (RequestType) returns (ResponseType);\n";
    assertThat(rpc.toString()).isEqualTo(expected);
  }

  @Test public void rpcWithDocumentationToString() {
    RpcElement rpc = RpcElement.builder()
        .name("Name")
        .documentation("Hello")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build();
    String expected = ""
        + "// Hello\n"
        + "rpc Name (RequestType) returns (ResponseType);\n";
    assertThat(rpc.toString()).isEqualTo(expected);
  }

  @Test public void rpcWithOptions() {
    RpcElement rpc = RpcElement.builder()
        .name("Name")
        .requestType("RequestType")
        .responseType("ResponseType")
        .addOption(OptionElement.create("foo", "bar", false))
        .build();
    String expected = ""
        + "rpc Name (RequestType) returns (ResponseType) {\n"
        + "  option foo = \"bar\";\n"
        + "};\n";
    assertThat(rpc.toString()).isEqualTo(expected);
  }
}
