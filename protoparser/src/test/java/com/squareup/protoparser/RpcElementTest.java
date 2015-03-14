package com.squareup.protoparser;

import com.squareup.protoparser.DataType.NamedType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class RpcElementTest {
  @Test public void nameRequired() {
    try {
      RpcElement.builder()
          .requestType(NamedType.create("Foo"))
          .responseType(NamedType.create("Bar"))
          .build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
  }

  @Test public void requestTypeRequired() {
    try {
      RpcElement.builder().name("Test").responseType(NamedType.create("Bar")).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("requestType == null");
    }
  }

  @Test public void responseTypeRequired() {
    try {
      RpcElement.builder().name("Test").requestType(NamedType.create("Foo")).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("responseType == null");
    }
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      RpcElement.builder().name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
    try {
      RpcElement.builder().requestType(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("requestType == null");
    }
    try {
      RpcElement.builder().responseType(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("responseType == null");
    }
    try {
      RpcElement.builder().documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation == null");
    }
    try {
      RpcElement.builder().addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option == null");
    }
  }
}
