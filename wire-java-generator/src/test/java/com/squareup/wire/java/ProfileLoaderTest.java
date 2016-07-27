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
import com.squareup.javapoet.ClassName;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.assertj.core.data.MapEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public final class ProfileLoaderTest {
  @Rule public final TemporaryFolder temp = new TemporaryFolder();

  @Test public void test() throws IOException {
    temp.newFolder("a", "b", "c");
    writeFile(temp.newFile("a/b/message1.proto"), ""
        + "package a.b;\n"
        + "message Message1 {\n"
        + "}\n");
    writeFile(temp.newFile("a/b/c/message2.proto"), ""
        + "package a.b.c;\n"
        + "message Message1 {\n"
        + "}\n");
    writeFile(temp.newFile("android.wire"), ""
        + "syntax = \"wire2\";\n"
        + "type a.b.Message1 {\n"
        + "  target java.lang.Object using com.example.Message1#OBJECT_ADAPTER;\n"
        + "}\n");
    writeFile(temp.newFile("a/b/c/android.wire"), ""
        + "syntax = \"wire2\";\n"
        + "package a.b.c;\n"
        + "type a.b.c.Message2 {\n"
        + "  target java.lang.String using com.example.Message2#STRING_ADAPTER;\n"
        + "}\n");

    Schema schema = new SchemaLoader()
        .addSource(temp.getRoot())
        .load();
    Profile profile = new ProfileLoader("android")
        .addSchema(schema)
        .load();

    ProtoType message1 = ProtoType.get("a.b.Message1");
    assertThat(profile.getTarget(message1)).isEqualTo(ClassName.OBJECT);
    assertThat(profile.getAdapter(message1)).isEqualTo("com.example.Message1#OBJECT_ADAPTER");

    ProtoType message2 = ProtoType.get("a.b.c.Message2");
    assertThat(profile.getTarget(message2)).isEqualTo(ClassName.get(String.class));
    assertThat(profile.getAdapter(message2)).isEqualTo("com.example.Message2#STRING_ADAPTER");
  }

  @Test public void profileInZip() throws IOException {
    File file = temp.newFile();
    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
    writeFile(zipOutputStream, "a/b/message.proto", ""
        + "package a.b;\n"
        + "message Message {"
        + "}");
    writeFile(zipOutputStream, "a/b/android.wire", ""
        + "syntax = \"wire2\";\n"
        + "package a.b;\n"
        + "type a.b.Message {\n"
        + "  target java.lang.Object using com.example.Message#ADAPTER;\n"
        + "}");
    zipOutputStream.close();

    Schema schema = new SchemaLoader()
        .addSource(file)
        .load();
    Profile profile = new ProfileLoader("android")
        .addSchema(schema)
        .load();

    ProtoType message = ProtoType.get("a.b.Message");
    assertThat(profile.getTarget(message)).isEqualTo(ClassName.OBJECT);
    assertThat(profile.getAdapter(message)).isEqualTo("com.example.Message#ADAPTER");
  }

  @Test public void pathsToAttempt() throws Exception {
    ProfileLoader loader = new ProfileLoader("android")
        .addProtoLocation(Location.get("/a/b", "c/d/e.proto"));
    assertThat(loader.pathsToAttempt().asMap()).containsExactly(
        MapEntry.entry(Paths.get("/a/b"), ImmutableSet.of(
            "c/d/android.wire",
            "c/android.wire",
            "android.wire")));
  }

  @Test public void pathsToAttemptMultipleRoots() throws Exception {
    ProfileLoader loader = new ProfileLoader("android")
        .addProtoLocation(Location.get("/a/b", "c/d/e.proto"))
        .addProtoLocation(Location.get("/a/b", "c/f/g/h.proto"))
        .addProtoLocation(Location.get("/i/j.zip", "k/l/m.proto"))
        .addProtoLocation(Location.get("/i/j.zip", "k/l/m/n.proto"));
    assertThat(loader.pathsToAttempt().asMap()).containsExactly(
        MapEntry.entry(Paths.get("/a/b"), ImmutableSet.of(
            "c/d/android.wire",
            "c/android.wire",
            "android.wire",
            "c/f/g/android.wire",
            "c/f/android.wire")),
        MapEntry.entry(Paths.get("/i/j.zip"), ImmutableSet.of(
            "k/l/android.wire",
            "k/android.wire",
            "android.wire",
            "k/l/m/android.wire")));
  }

  private void writeFile(File file, String content) throws IOException {
    Files.write(file.toPath(), content.getBytes(UTF_8));
  }

  private void writeFile(ZipOutputStream out, String file, String content) throws IOException {
    out.putNextEntry(new ZipEntry(file));
    out.write(content.getBytes(UTF_8));
  }
}
