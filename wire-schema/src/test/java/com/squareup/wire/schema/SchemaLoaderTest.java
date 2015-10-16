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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class SchemaLoaderTest {
  @Rule public final TemporaryFolder tempFolder1 = new TemporaryFolder();
  @Rule public final TemporaryFolder tempFolder2 = new TemporaryFolder();

  @Test public void loadAllFilesWhenNoneSpecified() throws IOException {
    writeFile(tempFolder1.newFile("message1.proto"), "message Message1 {}");
    writeFile(tempFolder1.newFile("message2.proto"), "message Message2 {}");
    writeFile(tempFolder1.newFile("readme.txt"), "Here be protos!");

    Schema schema = new SchemaLoader()
        .addSource(tempFolder1.getRoot())
        .load();

    Type message1 = schema.getType("Message1");
    assertThat(message1).isNotNull();
    assertThat(message1.location().base()).isEqualTo(tempFolder1.getRoot().getPath());
    assertThat(message1.location().path()).isEqualTo("message1.proto");

    Type message2 = schema.getType("Message2");
    assertThat(message2).isNotNull();
    assertThat(message2.location().base()).isEqualTo(tempFolder1.getRoot().getPath());
    assertThat(message2.location().path()).isEqualTo("message2.proto");
  }

  @Test public void locateInMultiplePaths() throws IOException {
    File file1 = tempFolder1.newFile();
    File file2 = tempFolder2.newFile();

    new SchemaLoader()
        .addSource(tempFolder1.getRoot())
        .addSource(tempFolder2.getRoot())
        .addProto(file1.getName())
        .addProto(file2.getName())
        .load();
  }

  @Test public void failLocate() throws IOException {
    File file = tempFolder2.newFile();

    SchemaLoader loader = new SchemaLoader()
        .addSource(tempFolder1.getRoot())
        .addProto(file.getName());
    try {
      loader.load();
      fail();
    } catch (FileNotFoundException expected) {
    }
  }

  @Test public void locateInZipFile() throws IOException {
    File file = tempFolder1.newFile();
    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
    zipOutputStream.putNextEntry(new ZipEntry("a/b/message.proto"));
    zipOutputStream.write("message Message {}".getBytes(UTF_8));
    zipOutputStream.close();

    Schema schema = new SchemaLoader()
        .addSource(file)
        .addProto("a/b/message.proto")
        .load();

    Type message = schema.getType("Message");
    assertThat(message).isNotNull();
    assertThat(message.location().base()).isEqualTo(file.getPath());
    assertThat(message.location().path()).isEqualTo("a/b/message.proto");
  }

  @Test public void failLocateInZipFile() throws IOException {
    File file = tempFolder1.newFile();
    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
    zipOutputStream.putNextEntry(new ZipEntry("a/b/trix.proto"));
    zipOutputStream.write("message Trix {}".getBytes(UTF_8));
    zipOutputStream.close();

    try {
      new SchemaLoader()
          .addSource(file)
          .addProto("a/b/message.proto")
          .load();
      fail();
    } catch (FileNotFoundException expected) {
    }
  }

  @Test public void earlierSourcesTakePrecedenceOverLaterSources() throws IOException {
    File message1 = tempFolder1.newFile("message.proto");
    writeFile(message1, ""
        + "message Message {\n"
        + "  optional string a = 1;\n"
        + "}");
    File message2 = tempFolder2.newFile("message.proto");
    writeFile(message2, ""
        + "message Message {\n"
        + "  optional string b = 2;\n"
        + "}");

    Schema schema = new SchemaLoader()
        .addSource(tempFolder1.getRoot())
        .addSource(tempFolder2.getRoot())
        .load();
    MessageType message = (MessageType) schema.getType("Message");
    assertThat(message.field("a")).isNotNull();
  }

  private void writeFile(File file, String content) throws IOException {
    Files.write(file.toPath(), content.getBytes(UTF_8));
  }
}
