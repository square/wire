package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.ServiceElement.Method;
import static com.squareup.protoparser.TestUtils.NO_METHODS;
import static com.squareup.protoparser.TestUtils.NO_OPTIONS;
import static com.squareup.protoparser.TestUtils.list;
import static org.fest.assertions.api.Assertions.assertThat;

public class ServiceElementTest {
  @Test public void emptyToString() {
    ServiceElement service = ServiceElement.create("Service", "", "", NO_OPTIONS, NO_METHODS);
    String expected = "service Service {}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void singleToString() {
    Method method = Method.create("Name", "", "RequestType", "ResponseType", NO_OPTIONS);
    ServiceElement service = ServiceElement.create("Service", "", "", NO_OPTIONS, list(method));
    String expected = ""
        + "service Service {\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void singleWithOptionsToString() {
    Method method = Method.create("Name", "", "RequestType", "ResponseType", NO_OPTIONS);
    ServiceElement
        service = ServiceElement.create("Service", "", "", list(OptionElement.create("foo", "bar", false)),
        list(method));
    String expected = ""
        + "service Service {\n"
        + "  option foo = \"bar\";\n"
        + "\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void singleWithDocumentation() {
    Method method = Method.create("Name", "", "RequestType", "ResponseType", NO_OPTIONS);
    ServiceElement service = ServiceElement.create("Service", "", "Hello", NO_OPTIONS, list(method));
    String expected = ""
        + "// Hello\n"
        + "service Service {\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void multipleToString() {
    Method method = Method.create("Name", "", "RequestType", "ResponseType", NO_OPTIONS);
    ServiceElement service = ServiceElement.create("Service", "", "", NO_OPTIONS, list(method, method));
    String expected = ""
        + "service Service {\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "  rpc Name (RequestType) returns (ResponseType);\n"
        + "}\n";
    assertThat(service.toString()).isEqualTo(expected);
  }

  @Test public void methodToString() {
    Method method = Method.create("Name", "", "RequestType", "ResponseType", NO_OPTIONS);
    String expected = "rpc Name (RequestType) returns (ResponseType);\n";
    assertThat(method.toString()).isEqualTo(expected);
  }

  @Test public void methodWithDocumentationToString() {
    Method method = Method.create("Name", "Hello", "RequestType", "ResponseType", NO_OPTIONS);
    String expected = ""
        + "// Hello\n"
        + "rpc Name (RequestType) returns (ResponseType);\n";
    assertThat(method.toString()).isEqualTo(expected);
  }

  @Test public void methodWithOptions() {
    Method method = Method.create("Name", "", "RequestType", "ResponseType",
        list(OptionElement.create("foo", "bar", false)));
    String expected = ""
        + "rpc Name (RequestType) returns (ResponseType) {\n"
        + "  option foo = \"bar\";\n"
        + "};\n";
    assertThat(method.toString()).isEqualTo(expected);
  }
}
