package com.squareup.protoparser;

import com.squareup.protoparser.DataType.NamedType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ServiceElementTest {
  @Test public void nameRequired() {
    try {
      ServiceElement.builder().qualifiedName("Test").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
  }

  @Test public void nameSetsQualifiedName() {
    ServiceElement test = ServiceElement.builder().name("Test").build();
    assertThat(test.name()).isEqualTo("Test");
    assertThat(test.qualifiedName()).isEqualTo("Test");
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      ServiceElement.builder().name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
    try {
      ServiceElement.builder().qualifiedName(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("qualifiedName == null");
    }
    try {
      ServiceElement.builder().documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation == null");
    }
    try {
      ServiceElement.builder().addRpc(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("rpc == null");
    }
    try {
      ServiceElement.builder().addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option == null");
    }
  }

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
            .requestType(NamedType.create("RequestType"))
            .responseType(NamedType.create("ResponseType"))
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
            .requestType(NamedType.create("RequestType"))
            .responseType(NamedType.create("ResponseType"))
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
            .requestType(NamedType.create("RequestType"))
            .responseType(NamedType.create("ResponseType"))
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
        .requestType(NamedType.create("RequestType"))
        .responseType(NamedType.create("ResponseType"))
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
        .requestType(NamedType.create("RequestType"))
        .responseType(NamedType.create("ResponseType"))
        .build();
    String expected = "rpc Name (RequestType) returns (ResponseType);\n";
    assertThat(rpc.toString()).isEqualTo(expected);
  }

  @Test public void rpcWithDocumentationToString() {
    RpcElement rpc = RpcElement.builder()
        .name("Name")
        .documentation("Hello")
        .requestType(NamedType.create("RequestType"))
        .responseType(NamedType.create("ResponseType"))
        .build();
    String expected = ""
        + "// Hello\n"
        + "rpc Name (RequestType) returns (ResponseType);\n";
    assertThat(rpc.toString()).isEqualTo(expected);
  }

  @Test public void rpcWithOptions() {
    RpcElement rpc = RpcElement.builder()
        .name("Name")
        .requestType(NamedType.create("RequestType"))
        .responseType(NamedType.create("ResponseType"))
        .addOption(OptionElement.create("foo", "bar", false))
        .build();
    String expected = ""
        + "rpc Name (RequestType) returns (ResponseType) {\n"
        + "  option foo = \"bar\";\n"
        + "};\n";
    assertThat(rpc.toString()).isEqualTo(expected);
  }
}
