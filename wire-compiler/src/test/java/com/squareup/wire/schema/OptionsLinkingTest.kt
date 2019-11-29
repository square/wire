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
package com.squareup.wire.schema

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.squareup.wire.testing.add
import com.squareup.wire.testing.exists
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class OptionsLinkingTest {
  private val fs = Jimfs.newFileSystem(Configuration.unix())

  @Test
  fun extensionOnTheSourcePathIsApplied() {
    fs.add("source-path/a.proto", """
             |import "formatting_options.proto";
             |message A {
             |  optional string s = 1 [formatting_options.language = "English"];
             |}
            """.trimMargin())
    fs.add("source-path/formatting_options.proto", """
             |import "google/protobuf/descriptor.proto";
             |
             |message FormattingOptions {
             |  optional string language = 1;
             |}
             |
             |extend google.protobuf.FieldOptions {
             |  optional FormattingOptions formatting_options = 22001;
             |}
            """.trimMargin())
    val schema = loadAndLinkSchema()

    val typeA = schema.getType("A") as MessageType
    assertThat(typeA.field("s").options().map()).isEqualTo(
        mapOf(
            formattingOptionsField to mapOf(
                languageField to "English"
            )
        )
    )
  }

  @Test
  fun extensionOnTheProtoPathIsApplied() {
    fs.add("source-path/a.proto", """
             |import "formatting_options.proto";
             |message A {
             |  optional string s = 1 [formatting_options.language = "English"];
             |}
            """.trimMargin())
    fs.add("proto-path/formatting_options.proto", """
             |import "google/protobuf/descriptor.proto";
             |
             |message FormattingOptions {
             |  optional string language = 1;
             |}
             |
             |extend google.protobuf.FieldOptions {
             |  optional FormattingOptions formatting_options = 22001;
             |}
            """.trimMargin())
    val schema = loadAndLinkSchema()

    val typeA = schema.getType("A") as MessageType
    assertThat(typeA.field("s").options().map()).isEqualTo(
        mapOf(
            formattingOptionsField to mapOf(
                languageField to "English"
            )
        )
    )
  }

  @Test
  fun fieldsOfExtensions() {
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

    val typeA = schema.getType("A") as MessageType
    assertThat(typeA.field("s").options().map()).isEqualTo(
        mapOf(
            formattingOptionsField to mapOf(
                languageField to mapOf(
                    nameField to "English"
                )
            )
        )
    )
  }

  private fun loadAndLinkSchema(): Schema {
    return NewSchemaLoader(fs).use { loader ->
      val protoPath = when {
        fs.exists("proto-path") -> listOf(Location.get("proto-path"))
        else -> listOf()
      }

      loader.initRoots(
          sourcePath = listOf(Location.get("source-path")),
          protoPath = protoPath
      )
      val sourceProtoFiles = loader.loadSourcePathFiles()
      Schema.fromFiles(sourceProtoFiles, loader)
    }
  }

  companion object {
    private val fieldOptions = ProtoType.get("google.protobuf.FieldOptions")
    private val formattingOptionsField = ProtoMember.get(fieldOptions, "formatting_options")

    private val formattingOptionsType = ProtoType.get("FormattingOptions")
    private val languageField = ProtoMember.get(formattingOptionsType, "language")

    private val languageType = ProtoType.get("Language")
    private val nameField = ProtoMember.get(languageType, "name")
  }
}
