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
package com.squareup.wire.java;

import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Test utils
 */
public class JavaGeneratorTestUtils
{
  public static Schema buildSchema(TemporaryFolder tempFolder, String fileName, String content) throws IOException {
    writeFile(tempFolder.newFile(fileName), content);
    return new SchemaLoader().addSource(tempFolder.getRoot()).load();
  }

  public static Schema buildSchemaFromResource(TemporaryFolder tempFolder, String resourceName) throws IOException {
    File file = new File(JavaGeneratorTestUtils.class.getClassLoader().getResource(resourceName).getFile());
    writeFile(tempFolder.newFile(file.getName()), Files.readAllBytes(file.toPath()));

    return new SchemaLoader().addSource(tempFolder.getRoot()).load();
  }

  public static void writeFile(File file, String content) throws IOException {
    Files.write(file.toPath(), content.getBytes(UTF_8));
  }

  public static void writeFile(File file, byte[] content) throws IOException {
    Files.write(file.toPath(), content);
  }
}
