/*
 * Copyright (C) 2015 Square, Inc.
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SchemaLoaderTest {
  private var fileSystem = Jimfs.newFileSystem(Configuration.unix())

  @Test
  @Throws(IOException::class)
  fun loadAllFilesWhenNoneSpecified() {
    Files.createDirectories(fileSystem.getPath("/source"))

    fileSystem.getPath("/source/message1.proto").writeText("message Message1 {}")
    fileSystem.getPath("/source/message2.proto").writeText("message Message2 {}")
    fileSystem.getPath("/source/readme.txt").writeText("Here be protos!")

    val schema = SchemaLoader()
        .addSource(fileSystem.getPath("/source"))
        .load()

    val message1 = schema.getType("Message1")!!
    assertThat(message1).isNotNull
    assertThat(message1.location.base).isEqualTo("/source")
    assertThat(message1.location.path).isEqualTo("message1.proto")

    val message2 = schema.getType("Message2")!!
    assertThat(message2).isNotNull
    assertThat(message2.location.base).isEqualTo("/source")
    assertThat(message2.location.path).isEqualTo("message2.proto")
  }

  @Test
  @Throws(IOException::class)
  fun locateInMultiplePaths() {
    Files.createDirectories(fileSystem.getPath("/source1"))
    Files.createDirectories(fileSystem.getPath("/source2"))
    Files.createFile(fileSystem.getPath("/source1/file1.proto"))
    Files.createFile(fileSystem.getPath("/source2/file2.proto"))

    SchemaLoader()
        .addSource(fileSystem.getPath("/source1"))
        .addSource(fileSystem.getPath("/source2"))
        .addProto("file1.proto")
        .addProto("file2.proto")
        .load()
  }

  @Test
  @Throws(IOException::class)
  fun failLocate() {
    Files.createDirectories(fileSystem.getPath("/source1"))
    Files.createDirectories(fileSystem.getPath("/source2"))
    Files.createFile(fileSystem.getPath("/source2/file2.proto"))

    val loader = SchemaLoader()
        .addSource(fileSystem.getPath("/source1"))
        .addProto("file2.proto")
    try {
      loader.load()
      fail()
    } catch (expected: FileNotFoundException) {
    }
  }

  @Test
  @Throws(IOException::class)
  fun locateInZipFile() {
    Files.createDirectories(fileSystem.getPath("/source"))
    val zip = fileSystem.getPath("/source/protos.zip")
    val zipOutputStream = ZipOutputStream(Files.newOutputStream(zip))
    zipOutputStream.putNextEntry(ZipEntry("a/b/message.proto"))
    zipOutputStream.write("message Message {}".toByteArray(UTF_8))
    zipOutputStream.close()

    val schema = SchemaLoader()
        .addSource(zip)
        .addProto("a/b/message.proto")
        .load()

    val message = schema.getType("Message")!!
    assertThat(message).isNotNull
    assertThat(message.location.base).isEqualTo("/source/protos.zip")
    assertThat(message.location.path).isEqualTo("a/b/message.proto")
  }

  @Test
  @Throws(IOException::class)
  fun failLocateInZipFile() {
    Files.createDirectories(fileSystem.getPath("/source"))
    val zip = fileSystem.getPath("/source/protos.zip")
    val zipOutputStream = ZipOutputStream(Files.newOutputStream(zip))
    zipOutputStream.putNextEntry(ZipEntry("a/b/trix.proto"))
    zipOutputStream.write("message Trix {}".toByteArray(UTF_8))
    zipOutputStream.close()

    try {
      SchemaLoader()
          .addSource(zip)
          .addProto("a/b/message.proto")
          .load()
      fail()
    } catch (expected: FileNotFoundException) {
    }
  }

  @Test
  @Throws(IOException::class)
  fun earlierSourcesTakePrecedenceOverLaterSources() {
    Files.createDirectories(fileSystem.getPath("/source1"))
    Files.createDirectories(fileSystem.getPath("/source2"))

    fileSystem.getPath("/source1/message.proto")
        .writeText("""
        |message Message {
        |  optional string a = 1;
        |}
        """.trimMargin())

    fileSystem.getPath("/source2/message.proto")
        .writeText("""
        |message Message {
        |  optional string b = 2;
        |}
        """.trimMargin())

    val schema = SchemaLoader()
        .addSource(fileSystem.getPath("/source1"))
        .addSource(fileSystem.getPath("/source2"))
        .load()
    val message = schema.getType("Message") as MessageType
    assertThat(message.field("a")).isNotNull
  }

  @Throws(IOException::class)
  private fun Path.writeText(content: String) {
    Files.write(this, content.toByteArray(UTF_8))
  }
}
