/*
 * Copyright (C) 2019 Square, Inc.
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
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire.schema

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.squareup.wire.testing.add
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.test.fail

class LinkerTest {
  private val fs = Jimfs.newFileSystem(Configuration.unix())

  @Test
  fun usedProtoPathFileIncludedInSchema() {
    fs.add("source-path/a.proto", """
            |import "b.proto";
            |message A {
            |  optional B b = 1;
            |}
            """.trimMargin())
    fs.add("proto-path/b.proto", """
            |message B {
            |}
            """.trimMargin())
    val schema = loadAndLinkSchema()

    assertThat(schema.protoFiles.map { it.location }).containsExactly(
        Location.get("source-path", "a.proto"),
        Location.get("proto-path", "b.proto"),
        Location.get("google/protobuf/descriptor.proto")
    )
  }

  @Test
  fun unusedProtoPathFileExcludedFromSchema() {
    fs.add("source-path/a.proto", """
            |import "b.proto";
            |message A {
            |}
            """.trimMargin())
    fs.add("proto-path/b.proto", """
            |message B {
            |}
            """.trimMargin())
    val schema = loadAndLinkSchema()

    assertThat(schema.protoFiles.map { it.location }).containsExactly(
        Location.get("source-path", "a.proto"),
        Location.get("google/protobuf/descriptor.proto")
    )
  }

  @Test
  fun onlyProtoPathTypesAreIncludedInSchema() {
    fs.add("source-path/a.proto", """
            |import "b.proto";
            |message A {
            |  optional B b = 1;
            |}
            """.trimMargin())
    fs.add("proto-path/b.proto", """
            |message B {
            |}
            |message C {
            |}
            """.trimMargin())
    val schema = loadAndLinkSchema()

    assertThat(schema.getType("B")).isNotNull()
    assertThat(schema.getType("C")).isNull()
  }

  @Test
  fun protoPathMembersAreIncludedInSchemaIfTheyAreUsedInOptions() {
    fs.add("source-path/a.proto", """
             |import "formatting_options.proto";
             |message A {
             |  optional string s = 1 [formatting_options.language.name = "English"];
             |}
            """.trimMargin())
    fs.add("proto-path/formatting_options.proto", """
             |import "google/protobuf/descriptor.proto";
             |
             |message FormattingOptions {
             |  optional Language language = 1;
             |  optional StringCasing string_casing = 2;
             |}
             |
             |extend google.protobuf.FieldOptions {
             |  optional FormattingOptions formatting_options = 22001;
             |}
             |
             |message Language {
             |  optional string name = 1;
             |  optional string locale = 2;
             |}
             |
             |enum StringCasing {
             |  LOWER_CASE = 1;
             |  TITLE_CASE = 2;
             |  SENTENCE_CASE = 3;
             |}
            """.trimMargin())
    val schema = loadAndLinkSchema()

    assertThat(schema.getType("FormattingOptions")).isNotNull()
    assertThat(schema.getType("Language")).isNotNull()
    assertThat(schema.getType("StringCasing")).isNotNull()

    assertThat(schema.getField("FormattingOptions", "language")).isNotNull()
    assertThat(schema.getField("FormattingOptions", "string_casing")).isNotNull()
    assertThat(schema.getField("Language", "name")).isNotNull()

    val fieldOptionsType = ProtoType.get("google.protobuf", "FieldOptions")
    assertThat(schema.getField(fieldOptionsType, "formatting_options")).isNotNull()
  }

  @Test
  fun protoPathMembersAreNotIncludedInSchemaIfTheyAreNotUsedInOptions() {
    fs.add("source-path/a.proto", """
            |import "b.proto";
            |message A {
            |  optional B b = 1;
            |}
            """.trimMargin())
    fs.add("proto-path/b.proto", """
            |message B {
            |  optional C c = 1;
            |}
            |message C {
            |}
            """.trimMargin())
    val schema = loadAndLinkSchema()

    assertThat(schema.getType("B")).isNotNull()
    assertThat(schema.getType("C")).isNull()
    assertThat(schema.getField("B", "c")).isNull()
  }

  @Test
  fun javaPackageIsSetOnProtoPathFiles() {
    fs.add("source-path/a.proto", """
            |import "b.proto";
            |message A {
            |  optional B b = 1;
            |}
            """.trimMargin())
    fs.add("proto-path/b.proto", """
            |option java_package = "com.squareup.b";
            |message B {
            |}
            """.trimMargin())
    val schema = loadAndLinkSchema()

    assertThat(schema.protoFile("b.proto")!!.javaPackage()).isEqualTo("com.squareup.b")
  }

  private fun loadAndLinkSchema(): Schema {
    NewSchemaLoader(fs).use { loader ->
      loader.initRoots(
          sourcePath = listOf(Location.get("source-path")),
          protoPath = listOf(Location.get("proto-path"))
      )
      return loader.loadSchema()
    }
  }
}
