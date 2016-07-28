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
package com.squareup.wire.java;

import com.google.common.collect.ImmutableSet;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.squareup.javapoet.ClassName;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaException;
import com.squareup.wire.schema.SchemaLoader;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.assertj.core.data.MapEntry;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class ProfileLoaderTest {
  FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());

  @Test public void test() throws IOException {
    Files.createDirectories(fileSystem.getPath("/source/a/b/c"));
    writeFile("/source/a/b/message1.proto", ""
        + "package a.b;\n"
        + "message Message1 {\n"
        + "}\n");
    writeFile("/source/a/b/c/message2.proto", ""
        + "package a.b.c;\n"
        + "message Message2 {\n"
        + "}\n");
    writeFile("/source/android.wire", ""
        + "syntax = \"wire2\";\n"
        + "import \"a/b/message1.proto\";\n"
        + "type a.b.Message1 {\n"
        + "  target java.lang.Object using com.example.Message1#OBJECT_ADAPTER;\n"
        + "}\n");
    writeFile("/source/a/b/c/android.wire", ""
        + "syntax = \"wire2\";\n"
        + "import \"a/b/c/message2.proto\";\n"
        + "package a.b.c;\n"
        + "type a.b.c.Message2 {\n"
        + "  target java.lang.String using com.example.Message2#STRING_ADAPTER;\n"
        + "}\n");

    Schema schema = new SchemaLoader()
        .addSource(fileSystem.getPath("/source"))
        .load();
    Profile profile = new ProfileLoader(fileSystem, "android")
        .schema(schema)
        .load();

    ProtoType message1 = ProtoType.get("a.b.Message1");
    assertThat(profile.getTarget(message1)).isEqualTo(ClassName.OBJECT);
    assertThat(profile.getAdapter(message1)).isEqualTo("com.example.Message1#OBJECT_ADAPTER");

    ProtoType message2 = ProtoType.get("a.b.c.Message2");
    assertThat(profile.getTarget(message2)).isEqualTo(ClassName.get(String.class));
    assertThat(profile.getAdapter(message2)).isEqualTo("com.example.Message2#STRING_ADAPTER");
  }

  @Test public void profileInZip() throws IOException {
    Files.createDirectories(fileSystem.getPath("/source"));
    Path zip = fileSystem.getPath("/source/protos.zip");
    ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zip));
    writeFile(zipOutputStream, "a/b/message.proto", ""
        + "package a.b;\n"
        + "message Message {"
        + "}");
    writeFile(zipOutputStream, "a/b/android.wire", ""
        + "syntax = \"wire2\";\n"
        + "package a.b;\n"
        + "import \"a/b/message.proto\";\n"
        + "type a.b.Message {\n"
        + "  target java.lang.Object using com.example.Message#ADAPTER;\n"
        + "}");
    zipOutputStream.close();

    Schema schema = new SchemaLoader()
        .addSource(zip)
        .load();
    Profile profile = new ProfileLoader(fileSystem, "android")
        .schema(schema)
        .load();

    ProtoType message = ProtoType.get("a.b.Message");
    assertThat(profile.getTarget(message)).isEqualTo(ClassName.OBJECT);
    assertThat(profile.getAdapter(message)).isEqualTo("com.example.Message#ADAPTER");
  }

  @Test public void pathsToAttempt() throws Exception {
    ImmutableSet<Location> locations = ImmutableSet.of(
        Location.get("/a/b", "c/d/e.proto"));
    ProfileLoader loader = new ProfileLoader(fileSystem, "android");
    assertThat(loader.pathsToAttempt(locations).asMap()).containsExactly(
        MapEntry.entry(fileSystem.getPath("/a/b"), ImmutableSet.of(
            "c/d/android.wire",
            "c/android.wire",
            "android.wire")));
  }

  @Test public void pathsToAttemptMultipleRoots() throws Exception {
    ImmutableSet<Location> locations = ImmutableSet.of(
        Location.get("/a/b", "c/d/e.proto"),
        Location.get("/a/b", "c/f/g/h.proto"),
        Location.get("/i/j.zip", "k/l/m.proto"),
        Location.get("/i/j.zip", "k/l/m/n.proto"));
    ProfileLoader loader = new ProfileLoader(fileSystem, "android");
    assertThat(loader.pathsToAttempt(locations).asMap()).containsExactly(
        MapEntry.entry(fileSystem.getPath("/a/b"), ImmutableSet.of(
            "c/d/android.wire",
            "c/android.wire",
            "android.wire",
            "c/f/g/android.wire",
            "c/f/android.wire")),
        MapEntry.entry(fileSystem.getPath("/i/j.zip"), ImmutableSet.of(
            "k/l/android.wire",
            "k/android.wire",
            "android.wire",
            "k/l/m/android.wire")));
  }

  @Test public void unknownType() throws Exception {
    Files.createDirectories(fileSystem.getPath("/source/a/b"));
    writeFile("/source/a/b/message.proto", ""
        + "package a.b;\n"
        + "message Message {\n"
        + "}\n");
    writeFile("/source/a/b/android.wire", ""
        + "syntax = \"wire2\";\n"
        + "type a.b.Message2 {\n"
        + "  target java.lang.Object using com.example.Message#OBJECT_ADAPTER;\n"
        + "}\n");

    Schema schema = new SchemaLoader()
        .addSource(fileSystem.getPath("/source"))
        .load();
    try {
      new ProfileLoader(fileSystem, "android")
          .schema(schema)
          .load();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage(
          "unable to resolve a.b.Message2 (/source/a/b/android.wire at 2:1)");
    }
  }

  @Test public void missingImport() throws Exception {
    Files.createDirectories(fileSystem.getPath("/source/a/b"));
    writeFile("/source/a/b/message.proto", ""
        + "package a.b;\n"
        + "message Message {\n"
        + "}\n");
    writeFile("/source/a/b/android.wire", ""
        + "syntax = \"wire2\";\n"
        + "type a.b.Message {\n"
        + "  target java.lang.Object using com.example.Message#OBJECT_ADAPTER;\n"
        + "}\n");

    Schema schema = new SchemaLoader()
        .addSource(fileSystem.getPath("/source"))
        .load();
    try {
      new ProfileLoader(fileSystem, "android")
          .schema(schema)
          .load();
      fail();
    } catch (SchemaException expected) {
      assertThat(expected).hasMessage(
          "a/b/android.wire needs to import a/b/message.proto (/source/a/b/android.wire at 2:1)");
    }
  }

  private void writeFile(String path, String content) throws IOException {
    Files.write(fileSystem.getPath(path), content.getBytes(UTF_8));
  }

  private void writeFile(ZipOutputStream out, String file, String content) throws IOException {
    out.putNextEntry(new ZipEntry(file));
    out.write(content.getBytes(UTF_8));
  }
}
