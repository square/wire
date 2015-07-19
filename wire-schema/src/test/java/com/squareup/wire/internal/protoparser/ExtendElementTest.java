/*
 * Copyright (C) 2014 Square, Inc.
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

import com.squareup.wire.schema.Location;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static com.squareup.wire.schema.Field.Label.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ExtendElementTest {
  Location location = Location.get("file.proto");

  @Test public void locationRequired() {
    try {
      ExtendElement.builder(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("location");
    }
  }

  @Test public void nameRequired() {
    try {
      ExtendElement.builder(location).qualifiedName("Test").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
  }

  @Test public void nameSetsQualifiedName() {
    ExtendElement test = ExtendElement.builder(location).name("Test").build();
    assertThat(test.name()).isEqualTo("Test");
    assertThat(test.qualifiedName()).isEqualTo("Test");
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      ExtendElement.builder(location).name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
    try {
      ExtendElement.builder(location).qualifiedName(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("qualifiedName");
    }
    try {
      ExtendElement.builder(location).documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation");
    }
    try {
      ExtendElement.builder(location).addField(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("field");
    }
    try {
      ExtendElement.builder(location).addFields(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("fields");
    }
    try {
      ExtendElement.builder(location).addFields(Collections.<FieldElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("field");
    }
  }

  @Test public void emptyToSchema() {
    ExtendElement extend = ExtendElement.builder(location).name("Name").build();
    String expected = "extend Name {}\n";
    assertThat(extend.toSchema()).isEqualTo(expected);
  }

  @Test public void simpleToSchema() {
    ExtendElement extend = ExtendElement.builder(location)
        .name("Name")
        .addField(FieldElement.builder(location)
            .label(REQUIRED)
            .type("string")
            .name("name")
            .tag(1)
            .build())
        .build();
    String expected = ""
        + "extend Name {\n"
        + "  required string name = 1;\n"
        + "}\n";
    assertThat(extend.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleFields() {
    FieldElement firstName = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("first_name")
        .tag(1)
        .build();
    FieldElement lastName = FieldElement.builder(location)
        .label(REQUIRED)
        .type("string")
        .name("last_name")
        .tag(2)
        .build();
    ExtendElement extend = ExtendElement.builder(location)
        .name("Name")
        .addFields(Arrays.asList(firstName, lastName))
        .build();
    assertThat(extend.fields()).hasSize(2);
  }

  @Test public void simpleWithDocumentationToSchema() {
    ExtendElement extend = ExtendElement.builder(location)
        .name("Name")
        .documentation("Hello")
        .addField(FieldElement.builder(location)
            .label(REQUIRED)
            .type("string")
            .name("name")
            .tag(1)
            .build())
        .build();
    String expected = ""
        + "// Hello\n"
        + "extend Name {\n"
        + "  required string name = 1;\n"
        + "}\n";
    assertThat(extend.toSchema()).isEqualTo(expected);
  }
}
