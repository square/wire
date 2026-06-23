/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire.schema

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.squareup.wire.testing.add
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Assert.fail
import org.junit.Test

class OptionsLinkingTest {
  private val fs = FakeFileSystem().apply {
    if (Path.DIRECTORY_SEPARATOR == "\\") emulateWindows() else emulateUnix()
  }

  @Test
  fun extensionOnTheSourcePathIsApplied() {
    fs.add(
      "source-path/a.proto",
      """
             |import "formatting_options.proto";
             |message A {
             |  optional string s = 1 [(formatting_options).language = "English"];
             |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/formatting_options.proto",
      """
             |import "google/protobuf/descriptor.proto";
             |
             |message FormattingOptions {
             |  optional string language = 1;
             |}
             |
             |extend google.protobuf.FieldOptions {
             |  optional FormattingOptions formatting_options = 22001;
             |}
      """.trimMargin(),
    )
    val schema = loadAndLinkSchema()

    val typeA = schema.getType("A") as MessageType
    assertThat(typeA.field("s")!!.options.map).isEqualTo(
      mapOf(
        formattingOptionsField to mapOf(
          languageField to "English",
        ),
      ),
    )
  }

  @Test
  fun extensionOnTheProtoPathIsApplied() {
    fs.add(
      "source-path/a.proto",
      """
             |import "formatting_options.proto";
             |message A {
             |  optional string s = 1 [(formatting_options).language = "English"];
             |}
      """.trimMargin(),
    )
    fs.add(
      "proto-path/formatting_options.proto",
      """
             |import "google/protobuf/descriptor.proto";
             |
             |message FormattingOptions {
             |  optional string language = 1;
             |}
             |
             |extend google.protobuf.FieldOptions {
             |  optional FormattingOptions formatting_options = 22001;
             |}
      """.trimMargin(),
    )
    val schema = loadAndLinkSchema()

    val typeA = schema.getType("A") as MessageType
    assertThat(typeA.field("s")!!.options.map).isEqualTo(
      mapOf(
        formattingOptionsField to mapOf(
          languageField to "English",
        ),
      ),
    )
  }

  @Test
  fun fieldsOfExtensions() {
    fs.add(
      "source-path/a.proto",
      """
             |import "formatting_options.proto";
             |message A {
             |  optional string s = 1 [(formatting_options).language.name = "English"];
             |  optional string t = 2 [(length).max = 80];
             |}
      """.trimMargin(),
    )
    fs.add(
      "proto-path/formatting_options.proto",
      """
             |import "google/protobuf/descriptor.proto";
             |
             |message FormattingOptions {
             |  optional Language language = 1;
             |  optional StringCasing string_casing = 2;
             |}
             |
             |extend google.protobuf.FieldOptions {
             |  optional FormattingOptions formatting_options = 22001;
             |  optional Range length = 22002;
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
             |
             |message Range {
             |  optional double min = 1;
             |  optional double max = 2;
             |}
      """.trimMargin(),
    )
    val schema = loadAndLinkSchema()

    val typeA = schema.getType("A") as MessageType
    assertThat(typeA.field("s")!!.options.map).isEqualTo(
      mapOf(
        formattingOptionsField to mapOf(
          languageField to mapOf(
            nameField to "English",
          ),
        ),
      ),
    )

    val typeLanguage = schema.getType("Language") as MessageType
    assertThat(typeLanguage.field("name")).isNotNull()

    val typeRange = schema.getType("Range") as MessageType
    assertThat(typeRange.field("max")).isNotNull()
  }

  @Test
  fun rejectsInvalidOptionScalarLiterals() {
    fs.add(
      "source-path/a.proto",
      """
             |import "formatting_options.proto";
             |
             |message A {
             |  option (message_options).enabled = "false; static { } //";
             |  optional string s = 1 [
             |    (formatting_options).max = "80; static { } //",
             |    (formatting_options).casing = "LOWER_CASE; static { } //"
             |  ];
             |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/formatting_options.proto",
      """
             |import "google/protobuf/descriptor.proto";
             |
             |message MessageOptions {
             |  optional bool enabled = 1;
             |}
             |
             |message FormattingOptions {
             |  optional int32 max = 1;
             |  optional StringCasing casing = 2;
             |  optional string documentation = 3;
             |}
             |
             |enum StringCasing {
             |  LOWER_CASE = 1;
             |}
             |
             |extend google.protobuf.MessageOptions {
             |  optional MessageOptions message_options = 22001;
             |}
             |
             |extend google.protobuf.FieldOptions {
             |  optional FormattingOptions formatting_options = 22002;
             |}
      """.trimMargin(),
    )

    try {
      loadAndLinkSchema()
      fail()
    } catch (expected: SchemaException) {
      val message = expected.message!!
      assertThat(message).contains("invalid option value \"false; static { } //\" for bool")
      assertThat(message).contains("invalid option value \"80; static { } //\" for int32")
      assertThat(message).contains("invalid option value \"LOWER_CASE; static { } //\" for StringCasing")
    }
  }

  @Test
  fun extensionTypesInExternalFile() {
    fs.add(
      "source-path/a.proto",
      """
             |import "extensions.proto";
             |
             |message A {
             |  optional string s = 2 [(length).max = 80];
             |}
      """.trimMargin(),
    )
    fs.add(
      "proto-path/extensions.proto",
      """
             |import "google/protobuf/descriptor.proto";
             |import "range.proto";
             |
             |extend google.protobuf.FieldOptions {
             |  optional Range length = 22002;
             |}
      """.trimMargin(),
    )
    fs.add(
      "proto-path/range.proto",
      """
             |
             |message Range {
             |  optional double min = 1;
             |  optional double max = 2;
             |}
      """.trimMargin(),
    )
    val schema = loadAndLinkSchema()

    val typeRange = schema.getType("Range") as MessageType
    assertThat(typeRange.field("max")).isNotNull()
  }

  private fun loadAndLinkSchema(): Schema {
    val loader = SchemaLoader(fs)
    val protoPath = when {
      fs.exists("proto-path".toPath()) -> listOf(Location.get("proto-path"))
      else -> listOf()
    }

    loader.initRoots(
      sourcePath = listOf(Location.get("source-path")),
      protoPath = protoPath,
    )
    return loader.loadSchema()
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
