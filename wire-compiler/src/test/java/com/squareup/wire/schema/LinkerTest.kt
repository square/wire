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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

class LinkerTest {
  private val fs = Jimfs.newFileSystem(Configuration.unix())

  @Ignore("TODO: implement")
  @Test
  fun unusedPathFileExcludedFromSchema() {
    fs.add("proto-source/a.proto", """
            |import "b.proto";
            |message A {
            |}
            """.trimMargin())
    fs.add("proto-path/b.proto", """
            |message B {
            |}
            """.trimMargin())
    val schema = loadAndLinkSchema()

    assertThat(schema.protoFiles().map { it.location() })
        .containsExactly(Location.get("src", "a.proto"))
  }

  private fun loadAndLinkSchema(): Schema {
    return NewSchemaLoader(fs).use { loader ->
      loader.initRoots(
          sourcePath = listOf(Location.get("proto-source")),
          protoPath = listOf(Location.get("proto-path"))
      )
      val sourceProtoFiles = loader.loadSourcePathFiles()
      Schema.fromFiles(sourceProtoFiles, loader)
    }
  }
}
