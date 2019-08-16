/*
 * Copyright 2019 Square Inc.
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
package com.squareup.wire.prune

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.ArrayList
import java.util.Collections

class ProtoPrunerTest {
  @Rule @JvmField val temp = TemporaryFolder()

  private lateinit var testDir: File

  /** Returns all paths within `root`, and relative to `root`.  */
  private val paths: List<String>
    get() {
      val root = testDir.absoluteFile
      return root.walkTopDown().filter { it.isFile }.map { it.toRelativeString(root) }.toList()
    }

  @Before fun setUp() {
    testDir = temp.root
  }

  @Test
  fun testFooBar() {
    val sources = arrayOf("squareup.foobar.Foo", "squareup.foobar.Bar")
    invokeProtoPruner(sources)

    val outputs = arrayOf(
        "bar.proto",
        "foo.proto")
    assertOutputs(outputs)
  }

  @Test @Ignore("Failing because of wrong indention on the output files.")
  fun testSimpleMessage() {
    val sources = arrayOf("squareup.protos.simple.SimpleMessage")
    invokeProtoPruner(sources, "--excludes=google.protobuf.*")

    val outputs = arrayOf(
        "simple_message.proto",
        "foreign.proto",
        "external_message.proto"
    )
    assertOutputs(outputs)
  }

  @Test @Ignore("Failing because `file_prefix` is stripped out.")
  fun testOptions() {
    val sources = arrayOf("squareup.options.letter.Letter")
    invokeProtoPruner(sources, "--excludes=google.protobuf.*")

    val outputs = arrayOf(
        "letter.proto",
        "file_prefix.proto"
    )
    assertOutputs(outputs)
  }

  private fun invokeProtoPruner(
    sources: Array<String>,
    vararg extraArgs: String
  ) {
    val args = ArrayList<String>()
    args.add("--in=../wire-tests/src/commonTest/proto")
    args.add("--out=${testDir.absolutePath}")
    Collections.addAll(args, sources.joinToString(prefix = "--includes=", separator = ","))
    Collections.addAll(args, *extraArgs)

    ProtoPruner.main(*args.toTypedArray())
  }

  private fun assertOutputs(outputs: Array<String>) {
    val filesAfter = paths
    assertThat(filesAfter.size)
        .overridingErrorMessage(filesAfter.toString())
        .isEqualTo(outputs.size)

    for (output in outputs) {
      assertFilesMatch(testDir, output)
    }
  }

  private fun assertFilesMatch(
    outputDir: File,
    path: String
  ) {
    // Compare against file with suffix if present
    val expectedFile = File("src/test/expected/$path")
    if (expectedFile.exists()) {
      println("Comparing against expected output ${expectedFile.name}")
    } else {
      throw AssertionError("Expected file $expectedFile doesn't exist.")
    }

    val actualFile = File(outputDir, path)
    assertFilesMatch(expectedFile, actualFile)
  }

  private fun assertFilesMatch(expectedFile: File, actualFile: File) {
    val expected = expectedFile.readText()
    val actual = actualFile.readText()

    // Normalize CRLF -> LF.
    assertThat(actual.replace("\r\n", "\n")).isEqualTo(expected.replace("\r\n", "\n"))
  }
}
