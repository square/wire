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

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static com.squareup.wire.internal.protoparser.DataType.ScalarType.STRING;
import static com.squareup.wire.internal.protoparser.FieldElement.Label.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ExtendElementTest {
  @Test public void nameRequired() {
    try {
      ExtendElement.builder().qualifiedName("Test").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
  }

  @Test public void nameSetsQualifiedName() {
    ExtendElement test = ExtendElement.builder().name("Test").build();
    assertThat(test.name()).isEqualTo("Test");
    assertThat(test.qualifiedName()).isEqualTo("Test");
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      ExtendElement.builder().name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
    try {
      ExtendElement.builder().qualifiedName(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("qualifiedName");
    }
    try {
      ExtendElement.builder().documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation");
    }
    try {
      ExtendElement.builder().addField(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("field");
    }
    try {
      ExtendElement.builder().addFields(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("fields");
    }
    try {
      ExtendElement.builder().addFields(Collections.<FieldElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("field");
    }
  }

  @Test public void emptyToSchema() {
    ExtendElement extend = ExtendElement.builder().name("Name").build();
    String expected = "extend Name {}\n";
    assertThat(extend.toSchema()).isEqualTo(expected);
  }

  @Test public void simpleToSchema() {
    ExtendElement extend = ExtendElement.builder()
        .name("Name")
        .addField(FieldElement.builder().label(REQUIRED).type(STRING).name("name").tag(1).build())
        .build();
    String expected = ""
        + "extend Name {\n"
        + "  required string name = 1;\n"
        + "}\n";
    assertThat(extend.toSchema()).isEqualTo(expected);
  }

  @Test public void addMultipleFields() {
    FieldElement firstName =
        FieldElement.builder().label(REQUIRED).type(STRING).name("first_name").tag(1).build();
    FieldElement lastName =
        FieldElement.builder().label(REQUIRED).type(STRING).name("last_name").tag(2).build();
    ExtendElement extend = ExtendElement.builder()
        .name("Name")
        .addFields(Arrays.asList(firstName, lastName))
        .build();
    assertThat(extend.fields()).hasSize(2);
  }

  @Test public void simpleWithDocumentationToSchema() {
    ExtendElement extend = ExtendElement.builder()
        .name("Name")
        .documentation("Hello")
        .addField(FieldElement.builder()
            .label(REQUIRED)
            .type(STRING)
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

  @Test public void duplicateTagValueThrows() {
    FieldElement field1 = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name1")
        .tag(1)
        .build();
    FieldElement field2 = FieldElement.builder()
        .label(REQUIRED)
        .type(STRING)
        .name("name2")
        .tag(1)
        .build();
    try {
      ExtendElement.builder()
          .name("Extend")
          .qualifiedName("example.Extend")
          .addField(field1)
          .addField(field2)
          .build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate tag 1 in example.Extend");
    }
  }
}
