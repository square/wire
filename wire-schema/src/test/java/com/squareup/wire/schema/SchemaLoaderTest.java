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

public final class SchemaLoaderTest {
  @Rule public final TemporaryFolder tempFolder1 = new TemporaryFolder();
  @Rule public final TemporaryFolder tempFolder2 = new TemporaryFolder();

  @Test public void locateInMultiplePaths() throws IOException {
    File file1 = tempFolder1.newFile();
    Files.write(file1.toPath(), "message Message1 {}".getBytes(UTF_8));
    File file2 = tempFolder2.newFile();
    Files.write(file2.toPath(), "message Message2 {}".getBytes(UTF_8));

    Schema schema = new SchemaLoader()
        .addSource(tempFolder1.getRoot())
        .addSource(tempFolder2.getRoot())
        .load();
    assertThat(schema.getType("Message1")).isNotNull();
    assertThat(schema.getType("Message2")).isNotNull();
  }

  @Test public void locateInZipFile() throws IOException {
    File file = tempFolder1.newFile();
    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
    zipOutputStream.putNextEntry(new ZipEntry("a/b/message.proto"));
    zipOutputStream.write("message Message {}".getBytes(UTF_8));
    zipOutputStream.close();

    Schema schema = new SchemaLoader()
        .addSource(file)
        .load();
    assertThat(schema.getType("Message")).isNotNull();
  }
}
