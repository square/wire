package com.squareup.protoparser;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class EnumConstantElementTest {
  @Test public void nameRequired() {
    try {
      EnumConstantElement.builder().tag(1).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
  }

  @Test public void tagRequired() {
    try {
      EnumConstantElement.builder().name("Test").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("tag == null");
    }
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      EnumConstantElement.builder().name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
    try {
      EnumConstantElement.builder().documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation == null");
    }
    try {
      EnumConstantElement.builder().addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option == null");
    }
  }
}
