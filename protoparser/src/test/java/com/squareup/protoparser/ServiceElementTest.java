package com.squareup.protoparser;

import com.squareup.protoparser.DataType.NamedType;
import java.util.Arrays;
import java.util.Collections;
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
      ServiceElement.builder().addRpcs(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("rpcs == null");
    }
    try {
      ServiceElement.builder().addRpcs(Collections.<RpcElement>singleton(null));
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
    try {
      ServiceElement.builder().addOptions(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("options == null");
    }
    try {
      ServiceElement.builder().addOptions(Collections.<OptionElement>singleton(null));
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

  @Test public void addMultipleRpcs() {
    RpcElement firstName = RpcElement.builder()
        .name("FirstName")
        .requestType(NamedType.create("RequestType"))
        .responseType(NamedType.create("ResponseType"))
        .build();
    RpcElement lastName = RpcElement.builder()
        .name("LastName")
        .requestType(NamedType.create("RequestType"))
        .responseType(NamedType.create("ResponseType"))
        .build();
    ServiceElement service = ServiceElement.builder()
        .name("Service")
        .addRpcs(Arrays.asList(firstName, lastName))
        .build();
    String expected = ""
        + "service Service {\n"
        + "  rpc FirstName (RequestType) returns (ResponseType);\n"
        + "  rpc LastName (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void singleWithOptionsToString() {
    ServiceElement service = ServiceElement.builder()
        .name("Service")
        .addOption(OptionElement.create("foo", "bar"))
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

  @Test public void addMultipleOptions() {
    OptionElement kitKat = OptionElement.create("kit", "kat");
    OptionElement fooBar = OptionElement.create("foo", "bar");
    ServiceElement service = ServiceElement.builder()
        .name("Service")
        .addOptions(Arrays.asList(kitKat, fooBar))
        .addRpc(RpcElement.builder()
            .name("Name")
            .requestType(NamedType.create("RequestType"))
            .responseType(NamedType.create("ResponseType"))
            .build())
        .build();
    String expected = ""
        + "service Service {\n"
        + "  option kit = \"kat\";\n"
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
        .addOption(OptionElement.create("foo", "bar"))
        .build();
    String expected = ""
        + "rpc Name (RequestType) returns (ResponseType) {\n"
        + "  option foo = \"bar\";\n"
        + "};\n";
    assertThat(rpc.toString()).isEqualTo(expected);
  }
}
