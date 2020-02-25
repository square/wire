/*
 * Copyright 2013 Square Inc.
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
package com.squareup.wire

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.squareup.wire.schema.SchemaException
import okio.buffer
import okio.source
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import kotlin.test.assertFailsWith

class WireCompilerErrorTest {
  private var fileSystem = Jimfs.newFileSystem(Configuration.unix())

  /**
   * Compile a .proto containing in a String and returns the contents of each output file,
   * indexed by class name.
   */
  private fun compile(source: String) {
    val test = fileSystem.getPath("/source/test.proto")
    Files.createDirectory(fileSystem.getPath("/source"))
    Files.createDirectory(fileSystem.getPath("/target"))
    Files.write(test, source.toByteArray(UTF_8))

    val compiler = WireCompiler.forArgs(fileSystem, StringWireLogger(),
        "--proto_path=/source", "--java_out=/target", "test.proto")
    compiler.compile()
  }

  @Test
  fun testCorrect() {
    compile("""
        |package com.squareup.protos.test;
        |message Simple {
        |  optional int32 f = 1;
        |}
        """.trimMargin())
    val generatedSource = readFile("/target/com/squareup/protos/test/Simple.java")
    assertThat(generatedSource).contains(
        "public final class Simple extends Message<Simple, Simple.Builder> {")
  }

  @Test
  fun testZeroTag() {
    val e = assertFailsWith<SchemaException> {
      compile("""
          |package com.squareup.protos.test;
          |message Simple {
          |  optional int32 f = 0;
          |}
          """.trimMargin())
    }
    assertThat(e).hasMessage("""
          |tag is out of range: 0
          |  for field f (/source/test.proto at 3:3)
          |  in message com.squareup.protos.test.Simple (/source/test.proto at 2:1)
          """.trimMargin())
  }

  @Test
  fun testDuplicateTag() {
    val e = assertFailsWith<SchemaException> {
      compile("""
          |package com.squareup.protos.test;
          |message Simple {
          |  optional int32 f = 1;
          |  optional int32 g = 1;
          |}
          """.trimMargin())
    }
    assertThat(e).hasMessage("""
          |multiple fields share tag 1:
          |  1. f (/source/test.proto at 3:3)
          |  2. g (/source/test.proto at 4:3)
          |  for message com.squareup.protos.test.Simple (/source/test.proto at 2:1)
          """.trimMargin())
  }

  @Test
  fun testEnumNamespaceType() {
    val e = assertFailsWith<SchemaException> {
      compile("""
          |package com.squareup.protos.test;
          |message Foo {
          |  enum Bar {
          |    QUIX = 0;
          |    FOO = 1;
          |  }
          |
          |  enum Bar2 {
          |    BAZ = 0;
          |    QUIX = 1;
          |  }
          |}
          """.trimMargin())
    }
    assertThat(e).hasMessage("""
          |multiple enums share constant QUIX:
          |  1. com.squareup.protos.test.Foo.Bar.QUIX (/source/test.proto at 4:5)
          |  2. com.squareup.protos.test.Foo.Bar2.QUIX (/source/test.proto at 10:5)
          |  for message com.squareup.protos.test.Foo (/source/test.proto at 2:1)
          """.trimMargin())
  }

  @Test
  fun testEnumNamespaceFile() {
    val e = assertFailsWith<SchemaException> {
      compile("""
          |package com.squareup.protos.test;
          |
          |enum Bar {
          |  QUIX = 0;
          |  FOO = 1;
          |}
          |
          |enum Bar2 {
          |  BAZ = 0;
          |  QUIX = 1;
          |}
          """.trimMargin())
    }
    assertThat(e).hasMessage("""
          |multiple enums share constant QUIX:
          |  1. com.squareup.protos.test.Bar.QUIX (/source/test.proto at 4:3)
          |  2. com.squareup.protos.test.Bar2.QUIX (/source/test.proto at 10:3)
          """.trimMargin())
  }

  @Test
  fun testNoPackageNameIsLegal() {
    compile("message Simple { optional int32 f = 1; }")
    // Output should not have a 'package' declaration.
    assertThat(readFile("/target/Simple.java")).doesNotContain("package")
  }

  private fun readFile(path: String): String {
    return fileSystem.getPath(path).source().buffer().use {
      it.readUtf8()
    }
  }
}
