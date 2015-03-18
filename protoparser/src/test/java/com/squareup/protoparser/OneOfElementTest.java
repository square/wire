package com.squareup.protoparser;

import java.util.Collections;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class OneOfElementTest {
  @Test public void nameRequired() {
    try {
      OneOfElement.builder().build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      OneOfElement.builder().name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
    try {
      OneOfElement.builder().documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation == null");
    }
    try {
      OneOfElement.builder().addField(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("field == null");
    }
    try {
      OneOfElement.builder().addFields(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("fields == null");
    }
    try {
      OneOfElement.builder().addFields(Collections.<FieldElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("field == null");
    }
  }
}
