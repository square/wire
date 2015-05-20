package com.squareup.protoparser;

import com.squareup.protoparser.DataType.NamedType;
import com.squareup.protoparser.OptionElement.Kind;
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

  @Test public void emptyToSchema() {
    ServiceElement service = ServiceElement.builder().name("Service").build();
    String expected = "service Service {}\n";
    assertThat(service.toSchema()).isEqualTo(expected);
  }

  @Test public void singleToSchema() {
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
    assertThat(service.toSchema()).isEqualTo(expected);
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
    assertThat(service.rpcs()).hasSize(2);
  }

  @Test public void singleWithOptionsToSchema() {
    ServiceElement service = ServiceElement.builder()
        .name("Service")
        .addOption(OptionElement.create("foo", Kind.STRING, "bar"))
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
    assertThat(service.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleOptions() {
    OptionElement kitKat = OptionElement.create("kit", Kind.STRING, "kat");
    OptionElement fooBar = OptionElement.create("foo", Kind.STRING, "bar");
    ServiceElement service = ServiceElement.builder()
        .name("Service")
        .addOptions(Arrays.asList(kitKat, fooBar))
        .addRpc(RpcElement.builder()
            .name("Name")
            .requestType(NamedType.create("RequestType"))
            .responseType(NamedType.create("ResponseType"))
            .build())
        .build();
    assertThat(service.options()).hasSize(2);
  }

  @Test public void singleWithDocumentationToSchema() {
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
    assertThat(service.toSchema()).isEqualTo(expected);
  }

  @Test public void multipleToSchema() {
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
    assertThat(service.toSchema()).isEqualTo(expected);
  }

  @Test public void rpcToSchema() {
    RpcElement rpc = RpcElement.builder()
        .name("Name")
        .requestType(NamedType.create("RequestType"))
        .responseType(NamedType.create("ResponseType"))
        .build();
    String expected = "rpc Name (RequestType) returns (ResponseType);\n";
    assertThat(rpc.toSchema()).isEqualTo(expected);
  }

  @Test public void rpcWithDocumentationToSchema() {
    RpcElement rpc = RpcElement.builder()
        .name("Name")
        .documentation("Hello")
        .requestType(NamedType.create("RequestType"))
        .responseType(NamedType.create("ResponseType"))
        .build();
    String expected = ""
        + "// Hello\n"
        + "rpc Name (RequestType) returns (ResponseType);\n";
    assertThat(rpc.toSchema()).isEqualTo(expected);
  }

  @Test public void rpcWithOptionsToSchema() {
    RpcElement rpc = RpcElement.builder()
        .name("Name")
        .requestType(NamedType.create("RequestType"))
        .responseType(NamedType.create("ResponseType"))
        .addOption(OptionElement.create("foo", Kind.STRING, "bar"))
        .build();
    String expected = ""
        + "rpc Name (RequestType) returns (ResponseType) {\n"
        + "  option foo = \"bar\";\n"
        + "};\n";
    assertThat(rpc.toSchema()).isEqualTo(expected);
  }
}
