/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.java

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.squareup.javapoet.ClassName
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.RepoBuilder
import com.squareup.wire.schema.SchemaLoader
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.MapEntry
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProfileLoaderTest {
  @Test @Throws(IOException::class)
  fun test() {
    val repoBuilder = RepoBuilder()
        .add(
            "a/b/message1.proto",
            """
              |package a.b;
              |message Message1 {
              |}
              """.trimMargin()
        )
        .add(
            "a/b/c/message2.proto",
            """
              |package a.b.c;
              |message Message2 {
              |}
              """.trimMargin()
        )
        .add(
            "android.wire",
            """
              |syntax = "wire2";
              |import "a/b/message1.proto";
              |type a.b.Message1 {
              |  target java.lang.Object using com.example.Message1#OBJECT_ADAPTER;
              |}
              """.trimMargin()
        )
        .add(
            "a/b/c/android.wire",
            """
              |syntax = "wire2";
              |import "a/b/c/message2.proto";
              |package a.b.c;
              |type a.b.c.Message2 {
              |  target java.lang.String using com.example.Message2#STRING_ADAPTER;
              |}
              """.trimMargin()
        )

    val profile = repoBuilder.profile("android")

    val message1 = ProtoType.get("a.b.Message1")
    assertThat(profile.getTarget(message1)).isEqualTo(ClassName.OBJECT)
    assertThat(profile.getAdapter(message1))
        .isEqualTo(AdapterConstant("com.example.Message1#OBJECT_ADAPTER"))

    val message2 = ProtoType.get("a.b.c.Message2")
    assertThat(profile.getTarget(message2))
        .isEqualTo(ClassName.get(String::class.java))
    assertThat(profile.getAdapter(message2))
        .isEqualTo(AdapterConstant("com.example.Message2#STRING_ADAPTER"))
  }

  @Test @Throws(IOException::class)
  fun profileInZip() {
    val fileSystem = Jimfs.newFileSystem(Configuration.unix())
    Files.createDirectories(fileSystem.getPath("/source"))
    val zip = fileSystem.getPath("/source/protos.zip")
    val zipOutputStream = ZipOutputStream(Files.newOutputStream(zip))
    writeFile(
        zipOutputStream,
        "a/b/message.proto",
        """package a.b;
          |message Message {}
          """.trimMargin()
    )
    writeFile(
        zipOutputStream,
        "a/b/android.wire",
        """syntax = "wire2";
          |package a.b;
          |import "a/b/message.proto";
          |type a.b.Message {
          |  target java.lang.Object using com.example.Message#ADAPTER;
          |}
          """.trimMargin()
    )
    zipOutputStream.close()

    val schema = SchemaLoader()
        .addSource(zip)
        .load()
    val profile = ProfileLoader(fileSystem, "android")
        .schema(schema)
        .load()

    val message = ProtoType.get("a.b.Message")
    assertThat(profile.getTarget(message)).isEqualTo(ClassName.OBJECT)
    assertThat(profile.getAdapter(message))
        .isEqualTo(AdapterConstant("com.example.Message#ADAPTER"))
  }

  @Test @Throws(Exception::class)
  fun pathsToAttempt() {
    val fileSystem = FileSystems.getDefault()
    val locations = setOf(Location.get("/a/b", "c/d/e.proto"))
    val loader = ProfileLoader(fileSystem, "android")
    assertThat(loader.pathsToAttempt(locations).asMap())
        .containsExactly(
            MapEntry.entry(
                fileSystem.getPath("/a/b"),
                setOf("c/d/android.wire", "c/android.wire", "android.wire")
            )
        )
  }

  @Test @Throws(Exception::class)
  fun pathsToAttemptMultipleRoots() {
    val fileSystem = FileSystems.getDefault()
    val locations = setOf(
        Location.get("/a/b", "c/d/e.proto"),
        Location.get("/a/b", "c/f/g/h.proto"),
        Location.get("/i/j.zip", "k/l/m.proto"),
        Location.get("/i/j.zip", "k/l/m/n.proto")
    )
    val loader = ProfileLoader(fileSystem, "android")
    assertThat(loader.pathsToAttempt(locations).asMap())
        .containsExactly(
            MapEntry.entry(
                fileSystem.getPath("/a/b"),
                setOf(
                    "c/d/android.wire",
                    "c/android.wire",
                    "android.wire",
                    "c/f/g/android.wire",
                    "c/f/android.wire"
                )
            ),
            MapEntry.entry(
                fileSystem.getPath("/i/j.zip"),
                setOf(
                    "k/l/android.wire",
                    "k/android.wire",
                    "android.wire",
                    "k/l/m/android.wire"
                )
            )
        )
  }

  @Test @Throws(Exception::class)
  fun unknownType() {
    val repoBuilder = RepoBuilder()
        .add(
            "a/b/message.proto", """
              |package a.b;
              |message Message {
              |}
              """.trimMargin()
        )
        .add(
            "a/b/android.wire", """
              |syntax = "wire2";
              |type a.b.Message2 {
              |  target java.lang.Object using com.example.Message#OBJECT_ADAPTER;
              |}
              """.trimMargin()
        )
    val profile = repoBuilder.profile("android")
    val message = ProtoType.get("a.b.Message")
    assertThat(profile.getTarget(message)).isNull()
  }

  @Test @Throws(Exception::class)
  fun missingImport() {
    val repoBuilder = RepoBuilder()
        .add(
            "a/b/message.proto", """
              |package a.b;
              |message Message {
              |}
              """.trimMargin()
        )
        .add(
            "a/b/android.wire", """
              |syntax = "wire2";
              |type a.b.Message {
              |  target java.lang.Object using com.example.Message#OBJECT_ADAPTER;
              |}
              """.trimMargin()
        )
    try {
      repoBuilder.profile("android")
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected)
          .hasMessage(
              "a/b/android.wire needs to import a/b/message.proto (/source/a/b/android.wire at 2:1)"
          )
    }
  }

  @Throws(IOException::class)
  private fun writeFile(
    out: ZipOutputStream,
    file: String,
    content: String
  ) {
    out.putNextEntry(ZipEntry(file))
    out.write(content.toByteArray(UTF_8))
  }
}
