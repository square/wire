package com.squareup.protoparser;

import com.squareup.protoparser.DataType.NamedType;
import com.squareup.protoparser.OptionElement.Kind;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static com.squareup.protoparser.DataType.ScalarType.STRING;
import static com.squareup.protoparser.FieldElement.Label.OPTIONAL;
import static com.squareup.protoparser.FieldElement.Label.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class FieldElementTest {
  @Test public void field() {
    FieldElement field = FieldElement.builder()
        .label(OPTIONAL)
        .type(NamedType.create("CType"))
        .name("ctype")
        .tag(1)
        .addOption(OptionElement.create("default", Kind.ENUM, "TEST"))
        .addOption(OptionElement.create("deprecated", Kind.BOOLEAN, "true"))
        .build();
    assertThat(field.isDeprecated()).isTrue();
    assertThat(field.getDefault().value()).isEqualTo("TEST");
    assertThat(field.options()).containsOnly( //
        OptionElement.create("default", Kind.ENUM, "TEST"), //
        OptionElement.create("deprecated", Kind.BOOLEAN, "true"));
  }

  @Test public void deprecatedSupportStringAndBoolean() {
    FieldElement field1 = FieldElement.builder()
        .label(OPTIONAL)
        .type(NamedType.create("CType"))
        .name("ctype")
        .tag(1)
        .addOption(OptionElement.create("deprecated", Kind.STRING, "true"))
        .build();
    assertThat(field1.isDeprecated()).isTrue();
    FieldElement field2 = FieldElement.builder()
        .label(OPTIONAL)
        .type(NamedType.create("CType"))
        .name("ctype")
        .tag(1)
        .addOption(OptionElement.create("deprecated", Kind.BOOLEAN, "true"))
        .build();
    assertThat(field2.isDeprecated()).isTrue();
  }

  @Test public void packedSupportStringAndBoolean() {
    FieldElement field1 = FieldElement.builder()
        .label(OPTIONAL)
        .type(NamedType.create("CType"))
        .name("ctype")
        .tag(1)
        .addOption(OptionElement.create("packed", Kind.STRING, "true"))
        .build();
    assertThat(field1.isPacked()).isTrue();
    FieldElement field2 = FieldElement.builder()
        .label(OPTIONAL)
        .type(NamedType.create("CType"))
        .name("ctype")
        .tag(1)
        .addOption(OptionElement.create("packed", Kind.BOOLEAN, "true"))
        .build();
    assertThat(field2.isPacked()).isTrue();
  }

  @Test public void addMultipleOptions() {
    OptionElement kitKat = OptionElement.create("kit", Kind.STRING, "kat");
    OptionElement fooBar = OptionElement.create("foo", Kind.STRING, "bar");
    FieldElement field = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name")
        .tag(1)
        .addOptions(Arrays.asList(kitKat, fooBar))
        .build();
    assertThat(field.options()).hasSize(2);
  }

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
    try {
      FieldElement.builder().addOptions(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("options == null");
    }
    try {
      FieldElement.builder().addOptions(Collections.<OptionElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option == null");
    }
  }
}
