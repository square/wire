/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.internal.protoparser;

import com.squareup.wire.internal.protoparser.OptionElement.Kind;
import com.squareup.wire.schema.Location;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static com.squareup.wire.internal.protoparser.FieldElement.Label.OPTIONAL;
import static com.squareup.wire.internal.protoparser.FieldElement.Label.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class FieldElementTest {
  Location location = Location.get("file.proto");

  @Test public void field() {
    FieldElement field = FieldElement.builder(location)
        .label(OPTIONAL)
        .type("CType")
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
    FieldElement field1 = FieldElement.builder(location)
        .label(OPTIONAL)
        .type("CType")
        .name("ctype")
        .tag(1)
        .addOption(OptionElement.create("deprecated", Kind.STRING, "true"))
        .build();
    assertThat(field1.isDeprecated()).isTrue();
    FieldElement field2 = FieldElement.builder(location)
        .label(OPTIONAL)
        .type("CType")
        .name("ctype")
        .tag(1)
        .addOption(OptionElement.create("deprecated", Kind.BOOLEAN, "true"))
        .build();
    assertThat(field2.isDeprecated()).isTrue();
  }

  @Test public void packedSupportStringAndBoolean() {
    FieldElement field1 = FieldElement.builder(location)
        .label(OPTIONAL)
        .type("CType")
        .name("ctype")
        .tag(1)
        .addOption(OptionElement.create("packed", Kind.STRING, "true"))
        .build();
    assertThat(field1.isPacked()).isTrue();
    FieldElement field2 = FieldElement.builder(location)
        .label(OPTIONAL)
        .type("CType")
        .name("ctype")
        .tag(1)
        .addOption(OptionElement.create("packed", Kind.BOOLEAN, "true"))
        .build();
    assertThat(field2.isPacked()).isTrue();
  }

  @Test public void addMultipleOptions() {
    OptionElement kitKat = OptionElement.create("kit", Kind.STRING, "kat");
    OptionElement fooBar = OptionElement.create("foo", Kind.STRING, "bar");
    FieldElement field = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("name")
        .tag(1)
        .addOptions(Arrays.asList(kitKat, fooBar))
        .build();
    assertThat(field.options()).hasSize(2);
  }

  @Test public void labelRequired() {
    try {
      FieldElement.builder(location).type("string").name("name").tag(1).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("label");
    }
  }

  @Test public void typeRequired() {
    try {
      FieldElement.builder(location).label(REQUIRED).name("name").tag(1).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("type");
    }
  }

  @Test public void nameRequired() {
    try {
      FieldElement.builder(location).label(REQUIRED).type("string").tag(1).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
  }

  @Test public void tagRequired() {
    try {
      FieldElement.builder(location).label(REQUIRED).type("string").name("name").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("tag");
    }
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      FieldElement.builder(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("location");
    }
    try {
      FieldElement.builder(location).type(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("type");
    }
    try {
      FieldElement.builder(location).name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
    try {
      FieldElement.builder(location).documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation");
    }
    try {
      FieldElement.builder(location).addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
    }
    try {
      FieldElement.builder(location).addOptions(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("options");
    }
    try {
      FieldElement.builder(location).addOptions(Collections.<OptionElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
    }
  }
}
