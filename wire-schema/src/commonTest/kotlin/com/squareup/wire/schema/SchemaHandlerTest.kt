/*
 * Copyright (C) 2026 Square, Inc.
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
package com.squareup.wire.schema

import kotlin.test.Test
import kotlin.test.assertFailsWith
import okio.Path
import okio.Path.Companion.toPath

class SchemaHandlerTest {
  private val schemaHandler = TestSchemaHandler()

  @Test
  fun generatedPathCanUseSameUncRoot() {
    val outDirectory = """\\trusted\generated""".toPath()
    val generatedPath = """\\trusted\generated\Message.java""".toPath()

    schemaHandler.checkGeneratedPath(generatedPath, outDirectory)
  }

  @Test
  fun generatedPathCannotEscapeToAnotherUncRoot() {
    val outDirectory = """\\trusted\generated""".toPath()
    val generatedPath = """\\attacker\generated\Message.java""".toPath()

    assertFailsWith<IllegalArgumentException> {
      schemaHandler.checkGeneratedPath(generatedPath, outDirectory)
    }
  }

  private class TestSchemaHandler : SchemaHandler() {
    fun checkGeneratedPath(filePath: Path, outDirectory: Path) = checkPathInOutDirectory(filePath, outDirectory)

    override fun handle(type: Type, context: Context) = null

    override fun handle(service: Service, context: Context) = listOf<Path>()

    override fun handle(extend: Extend, field: Field, context: Context) = null
  }
}
