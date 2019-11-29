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

import com.squareup.wire.schema.IdentifierSet
import com.squareup.wire.schema.Location
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.FileSystems

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
    val identifierSet = IdentifierSet.Builder()
        .include("squareup.foobar.Foo")
        .include("squareup.foobar.Bar")
        .build()
    invokeProtoPruner(identifierSet)

    val outputs = arrayOf(
        "bar.proto",
        "foo.proto")
    assertOutputs(outputs)
  }

  @Test
  fun testSimpleMessage() {
    val identifierSet = IdentifierSet.Builder()
        .include("squareup.protos.simple.SimpleMessage")
        .exclude("google.protobuf.*")
        .build()
    invokeProtoPruner(identifierSet)

    val outputs = arrayOf(
        "simple_message.proto",
        "foreign.proto",
        "external_message.proto"
    )
    assertOutputs(outputs)
  }

  @Test
  fun testOptions() {
    val identifierSet = IdentifierSet.Builder()
        .include("squareup.options.letter.Letter")
        .include("squareup.options.letter.Post")
        .build()
    invokeProtoPruner(identifierSet)

    val outputs = arrayOf(
        "letter.proto",
        "options.proto"
    )
    assertOutputs(outputs)
  }

  @Test @Ignore("Options are not properly pruned yet. See #1243.")
  fun testOptionsExcludingProtobuf() {
    val identifierSet = IdentifierSet.Builder()
        .include("squareup.options.poem.Poem")
        .include("squareup.options.poem.Court")
        .exclude("google.protobuf.*")
        .build()
    invokeProtoPruner(identifierSet)

    val outputs = arrayOf(
        "poem.proto"
    )
    assertOutputs(outputs)
  }

  private fun invokeProtoPruner(identifierSet: IdentifierSet) {
    val protoPruner = ProtoPruner(
        fs = FileSystems.getDefault(),
        sourcePath = listOf(Location.get("../wire-tests/src/commonTest/proto/java")),
        protoPath = listOf(),
        outPath = testDir.absolutePath,
        identifierSet = identifierSet
    )
    protoPruner.run()
  }

  private fun assertOutputs(outputs: Array<String>) {
    val filesAfter = paths
    assertThat(filesAfter.size)
        .overridingErrorMessage("${outputs.contentToString()} != $filesAfter")
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
