package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.DataType.ScalarType.STRING;
import static com.squareup.protoparser.MessageElement.Label.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class FieldElementTest {
  @Test public void labelRequired() {
    try {
      FieldElement.builder().type(STRING).name("name").tag(1).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("label == null");
    }
  }

  @Test public void typeRequired() {
    try {
      FieldElement.builder().label(REQUIRED).name("name").tag(1).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("type == null");
    }
  }

  @Test public void nameRequired() {
    try {
      FieldElement.builder().label(REQUIRED).type(STRING).tag(1).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
  }

  @Test public void tagRequired() {
    try {
      FieldElement.builder().label(REQUIRED).type(STRING).name("name").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("tag == null");
    }
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      FieldElement.builder().type(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("type == null");
    }
    try {
      FieldElement.builder().name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
    try {
      FieldElement.builder().documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation == null");
    }
    try {
      FieldElement.builder().addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option == null");
    }
  }
}
