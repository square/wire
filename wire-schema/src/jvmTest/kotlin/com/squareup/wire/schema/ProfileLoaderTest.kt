/*
 * Copyright (C) 2016 Square, Inc.
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

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.message
import com.palantir.javapoet.ClassName
import com.squareup.wire.testing.add
import com.squareup.wire.testing.addZip
import java.io.IOException
import kotlin.test.Test
import okio.Path
import okio.fakefilesystem.FakeFileSystem
import org.junit.Assert.fail

// TODO(Benoit) Move to `commonTest`.
class ProfileLoaderTest {
  private val fs = FakeFileSystem().apply {
    if (Path.DIRECTORY_SEPARATOR == "\\") emulateWindows() else emulateUnix()
  }

  @Test
  @Throws(IOException::class)
  fun test() {
    fs.add(
      "source-path/a/b/message1.proto",
      """
        |package a.b;
        |message Message1 {
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/a/b/c/message2.proto",
      """
        |package a.b.c;
        |message Message2 {
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/android.wire",
      """
        |syntax = "wire2";
        |import "a/b/message1.proto";
        |type a.b.Message1 {
        |  target java.lang.Object using com.example.Message1#OBJECT_ADAPTER;
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/a/b/c/android.wire",
      """
        |syntax = "wire2";
        |import "a/b/c/message2.proto";
        |package a.b.c;
        |type a.b.c.Message2 {
        |  target java.lang.String using com.example.Message2#STRING_ADAPTER;
        |}
      """.trimMargin(),
    )

    val profile = loadAndLinkProfile("android")

    val message1 = ProtoType.get("a.b.Message1")
    assertThat(profile.javaTarget(message1)).isEqualTo(ClassName.OBJECT)
    assertThat(profile.getAdapter(message1))
      .isEqualTo(AdapterConstant("com.example.Message1#OBJECT_ADAPTER"))

    val message2 = ProtoType.get("a.b.c.Message2")
    assertThat(profile.javaTarget(message2))
      .isEqualTo(ClassName.get(String::class.java))
    assertThat(profile.getAdapter(message2))
      .isEqualTo(AdapterConstant("com.example.Message2#STRING_ADAPTER"))
  }

  @Test
  @Throws(IOException::class)
  fun profileInZip() {
    fs.addZip(
      "/source/protos.zip",
      "a/b/message.proto" to """
          |package a.b;
          |message Message {}
      """.trimMargin(),
      "a/b/android.wire" to """
          |syntax = "wire2";
          |package a.b;
          |import "a/b/message.proto";
          |type a.b.Message {
          |  target java.lang.Object using com.example.Message#ADAPTER;
          |}
      """.trimMargin(),
    )

    val profile = loadAndLinkProfile(
      "android",
      sourcePath = listOf(Location.get("/source/protos.zip")),
    )

    val message = ProtoType.get("a.b.Message")
    assertThat(profile.javaTarget(message)).isEqualTo(ClassName.OBJECT)
    assertThat(profile.getAdapter(message))
      .isEqualTo(AdapterConstant("com.example.Message#ADAPTER"))
  }

  @Test
  fun unknownType() {
    fs.add(
      "source-path/a/b/message.proto",
      """
        |package a.b;
        |message Message {
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/a/b/android.wire",
      """
        |syntax = "wire2";
        |type a.b.Message2 {
        |  target java.lang.Object using com.example.Message#OBJECT_ADAPTER;
        |}
      """.trimMargin(),
    )
    val profile = loadAndLinkProfile("android")
    val message = ProtoType.get("a.b.Message")
    assertThat(profile.javaTarget(message)).isNull()
  }

  @Test
  fun missingImport() {
    fs.add(
      "source-path/a/b/message.proto",
      """
        |package a.b;
        |message Message {
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/a/b/android.wire",
      """
        |syntax = "wire2";
        |type a.b.Message {
        |  target java.lang.Object using com.example.Message#OBJECT_ADAPTER;
        |}
      """.trimMargin(),
    )
    try {
      loadAndLinkProfile("android")
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        "a/b/android.wire needs to import a/b/message.proto" +
          " (source-path/a/b/android.wire:2:1)",
      )
    }
  }

  private fun loadAndLinkProfile(
    name: String,
    sourcePath: List<Location> = listOf(Location.get("source-path")),
  ): Profile {
    val loader = SchemaLoader(fs)
    loader.initRoots(sourcePath)
    val schema = loader.loadSchema()
    return loader.loadProfile(name, schema)
  }
}
