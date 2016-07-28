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
package com.squareup.wire.schema;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class SchemaLoaderTest {
  FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());

  @Test public void loadAllFilesWhenNoneSpecified() throws IOException {
    Files.createDirectories(fileSystem.getPath("/source"));
    writeFile("/source/message1.proto", "message Message1 {}");
    writeFile("/source/message2.proto", "message Message2 {}");
    writeFile("/source/readme.txt", "Here be protos!");

    Schema schema = new SchemaLoader()
        .addSource(fileSystem.getPath("/source"))
        .load();

    Type message1 = schema.getType("Message1");
    assertThat(message1).isNotNull();
    assertThat(message1.location().base()).isEqualTo("/source");
    assertThat(message1.location().path()).isEqualTo("message1.proto");

    Type message2 = schema.getType("Message2");
    assertThat(message2).isNotNull();
    assertThat(message2.location().base()).isEqualTo("/source");
    assertThat(message2.location().path()).isEqualTo("message2.proto");
  }

  @Test public void locateInMultiplePaths() throws IOException {
    Files.createDirectories(fileSystem.getPath("/source1"));
    Files.createDirectories(fileSystem.getPath("/source2"));
    Files.createFile(fileSystem.getPath("/source1/file1.proto"));
    Files.createFile(fileSystem.getPath("/source2/file2.proto"));

    new SchemaLoader()
        .addSource(fileSystem.getPath("/source1"))
        .addSource(fileSystem.getPath("/source2"))
        .addProto("file1.proto")
        .addProto("file2.proto")
        .load();
  }

  @Test public void failLocate() throws IOException {
    Files.createDirectories(fileSystem.getPath("/source1"));
    Files.createDirectories(fileSystem.getPath("/source2"));
    Files.createFile(fileSystem.getPath("/source2/file2.proto"));

    SchemaLoader loader = new SchemaLoader()
        .addSource(fileSystem.getPath("/source1"))
        .addProto("file2.proto");
    try {
      loader.load();
      fail();
    } catch (FileNotFoundException expected) {
    }
  }

  @Test public void locateInZipFile() throws IOException {
    Files.createDirectories(fileSystem.getPath("/source"));
    Path zip = fileSystem.getPath("/source/protos.zip");
    ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zip));
    zipOutputStream.putNextEntry(new ZipEntry("a/b/message.proto"));
    zipOutputStream.write("message Message {}".getBytes(UTF_8));
    zipOutputStream.close();

    Schema schema = new SchemaLoader()
        .addSource(zip)
        .addProto("a/b/message.proto")
        .load();

    Type message = schema.getType("Message");
    assertThat(message).isNotNull();
    assertThat(message.location().base()).isEqualTo("/source/protos.zip");
    assertThat(message.location().path()).isEqualTo("a/b/message.proto");
  }

  @Test public void failLocateInZipFile() throws IOException {
    Files.createDirectories(fileSystem.getPath("/source"));
    Path zip = fileSystem.getPath("/source/protos.zip");
    ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zip));
    zipOutputStream.putNextEntry(new ZipEntry("a/b/trix.proto"));
    zipOutputStream.write("message Trix {}".getBytes(UTF_8));
    zipOutputStream.close();

    try {
      new SchemaLoader()
          .addSource(zip)
          .addProto("a/b/message.proto")
          .load();
      fail();
    } catch (FileNotFoundException expected) {
    }
  }

  @Test public void earlierSourcesTakePrecedenceOverLaterSources() throws IOException {
    Files.createDirectories(fileSystem.getPath("/source1"));
    Files.createDirectories(fileSystem.getPath("/source2"));
    writeFile("/source1/message.proto", ""
        + "message Message {\n"
        + "  optional string a = 1;\n"
        + "}");
    writeFile("/source2/message.proto", ""
        + "message Message {\n"
        + "  optional string b = 2;\n"
        + "}");

    Schema schema = new SchemaLoader()
        .addSource(fileSystem.getPath("/source1"))
        .addSource(fileSystem.getPath("/source2"))
        .load();
    MessageType message = (MessageType) schema.getType("Message");
    assertThat(message.field("a")).isNotNull();
  }

  private void writeFile(String path, String content) throws IOException {
    Files.write(fileSystem.getPath(path), content.getBytes(UTF_8));
  }
}
