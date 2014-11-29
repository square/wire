package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.ServiceElement.RpcElement;
import static com.squareup.protoparser.TestUtils.NO_METHODS;
import static com.squareup.protoparser.TestUtils.NO_OPTIONS;
import static com.squareup.protoparser.TestUtils.list;
import static org.assertj.core.api.Assertions.assertThat;

public class ServiceElementTest {
  @Test public void emptyToString() {
    ServiceElement service = ServiceElement.create("Service", "", "", NO_OPTIONS, NO_METHODS);
    String expected = "service Service {}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void singleToString() {
    ServiceElement.RpcElement
        rpc = ServiceElement.RpcElement.create("Name", "", "RequestType", "ResponseType",
        NO_OPTIONS);
    ServiceElement service = ServiceElement.create("Service", "", "", NO_OPTIONS, list(rpc));
    String expected = ""
        + "service Service {\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void singleWithOptionsToString() {
    ServiceElement.RpcElement
        rpc = ServiceElement.RpcElement.create("Name", "", "RequestType", "ResponseType",
        NO_OPTIONS);
    ServiceElement
        service = ServiceElement.create("Service", "", "", list(OptionElement.create("foo", "bar", false)),
        list(rpc));
    String expected = ""
        + "service Service {\n"
        + "  option foo = \"bar\";\n"
        + "\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void singleWithDocumentation() {
    ServiceElement.RpcElement
        rpc = RpcElement.create("Name", "", "RequestType", "ResponseType", NO_OPTIONS);
    ServiceElement service = ServiceElement.create("Service", "", "Hello", NO_OPTIONS, list(rpc));
    String expected = ""
        + "// Hello\n"
        + "service Service {\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void multipleToString() {
    RpcElement
        rpc = ServiceElement.RpcElement.create("Name", "", "RequestType", "ResponseType",
        NO_OPTIONS);
    ServiceElement service = ServiceElement.create("Service", "", "", NO_OPTIONS, list(rpc, rpc));
    String expected = ""
        + "service Service {\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void rpcToString() {
    ServiceElement.RpcElement
        rpc = ServiceElement.RpcElement.create("Name", "", "RequestType", "ResponseType",
        NO_OPTIONS);
    String expected = "rpc Name (RequestType) returns (ResponseType);\n";
    assertThat(rpc.toString()).isEqualTo(expected);
  }

  @Test public void rpcWithDocumentationToString() {
    ServiceElement.RpcElement
        rpc = ServiceElement.RpcElement.create("Name", "Hello", "RequestType", "ResponseType",
        NO_OPTIONS);
    String expected = ""
        + "// Hello\n"
        + "rpc Name (RequestType) returns (ResponseType);\n";
    assertThat(rpc.toString()).isEqualTo(expected);
  }

  @Test public void rpcWithOptions() {
    RpcElement
        rpc = RpcElement.create("Name", "", "RequestType", "ResponseType",
        list(OptionElement.create("foo", "bar", false)));
    String expected = ""
        + "rpc Name (RequestType) returns (ResponseType) {\n"
        + "  option foo = \"bar\";\n"
        + "};\n";
    assertThat(rpc.toString()).isEqualTo(expected);
  }
}
