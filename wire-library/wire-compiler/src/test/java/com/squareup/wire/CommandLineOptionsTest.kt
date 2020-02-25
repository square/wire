/*
 * Copyright 2015 Square Inc.
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

import com.squareup.wire.schema.ProtoType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.ArrayList
import kotlin.test.assertFailsWith

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
        "--java_out=.", "--proto_path=foo/bar", "--proto_path=one/two", "--proto_path=three/four")
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
    assertThat(compiler.pruningRules.isEmpty).isTrue()

    compiler = parseArgs("--java_out=.", "--includes=com.example.Foo")
    assertThat(compiler.pruningRules.isRoot(ProtoType.get("com.example.Foo"))).isTrue()
    assertThat(compiler.pruningRules.isRoot(ProtoType.get("com.example.Bar"))).isFalse()

    compiler = parseArgs("--java_out=.", "--includes=com.example.Foo,com.example.Bar")
    assertThat(compiler.pruningRules.isRoot(ProtoType.get("com.example.Foo"))).isTrue()
    assertThat(compiler.pruningRules.isRoot(ProtoType.get("com.example.Bar"))).isTrue()
  }

  private fun parseArgs(vararg args: String) = WireCompiler.forArgs(args = *args)
}
