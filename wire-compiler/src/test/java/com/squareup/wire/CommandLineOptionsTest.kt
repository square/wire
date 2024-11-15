/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.hasMessage
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.squareup.wire.schema.WireRun
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import kotlin.test.assertFailsWith
import org.junit.Test

class CommandLineOptionsTest {
  @Test
  fun unknownArgumentFails() {
    val e = assertFailsWith<IllegalArgumentException> {
      parseArgs("--do-work")
    }
    assertThat(e).hasMessage("Unknown argument '--do-work'.")
  }

  @Test
  fun protoPaths() {
    var compiler = parseArgs("--java_out=.")
    assertThat(compiler.protoPaths).isEmpty()

    compiler = parseArgs("--java_out=.", "--proto_path=foo/bar")
    assertThat(compiler.protoPaths).containsOnly("foo/bar")

    compiler = parseArgs(
      "--java_out=.",
      "--proto_path=foo/bar",
      "--proto_path=one/two",
      "--proto_path=three/four",
    )
    assertThat(compiler.protoPaths).containsExactly("foo/bar", "one/two", "three/four")
  }

  @Test
  fun javaOut() {
    assertFailsWith<WireException> {
      WireCompiler.forArgs()
    }

    val compiler = parseArgs("--java_out=baz/qux")
    assertThat(compiler.javaOut).isEqualTo("baz/qux")
  }

  @Test
  fun sourceFileNames() {
    var compiler = parseArgs("--java_out=.")
    assertThat(compiler.sourceFileNames).isEmpty()

    val expected = ArrayList<String>()
    compiler = parseArgs("--java_out=.", "baz", "qux")
    expected.add("baz")
    expected.add("qux")
    assertThat(compiler.sourceFileNames).isEqualTo(expected)
  }

  @Test
  fun sourceFileNamesFromInclude() {
    val tmpFile = File.createTempFile("proto", ".include")
    try {
      val out = PrintWriter(FileOutputStream(tmpFile))
      out.println("foo")
      out.println("bar")
      out.close()

      var compiler = parseArgs("--java_out=.", "--files=" + tmpFile.absolutePath)
      val expected = ArrayList<String>()
      expected.add("foo")
      expected.add("bar")
      assertThat(compiler.sourceFileNames).isEqualTo(expected)

      // Test both --files and bare filenames together
      compiler = parseArgs("--java_out=.", "--files=" + tmpFile.absolutePath, "baz")
      expected.add("baz")
      assertThat(compiler.sourceFileNames).isEqualTo(expected)
    } finally {
      tmpFile.delete()
    }
  }

  @Test
  fun roots() {
    var compiler = parseArgs("--java_out=.")
    assertThat(compiler.treeShakingRoots).containsExactly("*")
    assertThat(compiler.treeShakingRubbish).isEmpty()

    compiler = parseArgs("--java_out=.", "--includes=com.example.Foo")
    assertThat(compiler.treeShakingRoots).containsExactly("com.example.Foo")

    compiler = parseArgs("--java_out=.", "--includes=com.example.Foo,com.example.Bar")
    assertThat(compiler.treeShakingRoots).containsExactly("com.example.Foo", "com.example.Bar")
  }

  @Test
  fun customOptions() {
    var compiler = parseArgs(
      "--custom_out=src/custom/out",
      "--schema_handler_factory_class=com.squareup.wire.MyCustomHandlerFactory",
      "--custom_option=key1,one",
      "--custom_option=key2,value1,value2",
      "--custom_option=key3,three",
      "--custom_option=key1,override",
    )
    assertThat(compiler.customOut).isEqualTo("src/custom/out")
    assertThat(compiler.schemaHandlerFactoryClass).isEqualTo("com.squareup.wire.MyCustomHandlerFactory")
    assertThat(compiler.customOptions).isEqualTo(
      mapOf(
        "key1" to "override",
        "key2" to "value1,value2",
        "key3" to "three",
      ),
    )
  }

  @Test
  fun manifestModules() {
    val tmpFile = File.createTempFile("proto", ".yaml")
    tmpFile.writeText(
      """
      |a: {}
      |b:
      |  dependencies:
      |   - a
      |
      """.trimMargin(),
    )

    val compiler =
      parseArgs("--java_out=.", "--experimental-module-manifest=${tmpFile.absolutePath}")

    assertThat(compiler.modules).isEqualTo(
      mapOf("a" to WireRun.Module(), "b" to WireRun.Module(dependencies = setOf("a"))),
    )
  }

  private fun parseArgs(vararg args: String) = WireCompiler.forArgs(args = *args)
}
