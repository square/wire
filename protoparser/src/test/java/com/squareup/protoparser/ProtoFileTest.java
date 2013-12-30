package com.squareup.protoparser;

import org.junit.Test;

import static com.squareup.protoparser.ProtoFile.MAX_TAG_VALUE;
import static com.squareup.protoparser.ProtoFile.MIN_TAG_VALUE;
import static com.squareup.protoparser.ProtoFile.isValidTag;
import static org.fest.assertions.api.Assertions.assertThat;

public class ProtoFileTest {
  @Test public void tagValueValidation() {
    assertThat(isValidTag(MIN_TAG_VALUE - 1)).isFalse(); // Less than minimum.
    assertThat(isValidTag(MIN_TAG_VALUE)).isTrue();
    assertThat(isValidTag(1234)).isTrue();
    assertThat(isValidTag(19222)).isFalse(); // Reserved range.
    assertThat(isValidTag(2319573)).isTrue();
    assertThat(isValidTag(MAX_TAG_VALUE)).isTrue();
    assertThat(isValidTag(MAX_TAG_VALUE + 1)).isFalse(); // Greater than maximum.
  }
}
