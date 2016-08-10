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
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.java.JavaGenerator;
import com.squareup.wire.java.Profile;
import com.squareup.wire.java.ProfileLoader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import okio.Okio;
import okio.Source;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Builds a repository of {@code .proto} and {@code .wire} files to create schemas, profiles, and
 * adapters for testing.
 */
public final class RepoBuilder {
  final FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
  final Path root = fs.getPath("/source");
  final SchemaLoader schemaLoader = new SchemaLoader().addSource(root);

  public RepoBuilder add(String name, String protoFile) {
    if (name.endsWith(".proto")) {
      schemaLoader.addProto(name);
    } else if (!name.endsWith(".wire")) {
      throw new IllegalArgumentException("unexpected file extension: " + name);
    }

    Path relativePath = fs.getPath(name);
    try {
      Path resolvedPath = root.resolve(relativePath);
      Path parent = resolvedPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(resolvedPath, protoFile.getBytes(UTF_8));
    } catch (IOException e) {
      throw new AssertionError(e);
    }

    return this;
  }

  public RepoBuilder add(String path) throws IOException {
    File file = new File("../wire-runtime/src/test/proto/" + path);
    try (Source source = Okio.source(file)) {
      String protoFile = Okio.buffer(source).readUtf8();
      return add(path, protoFile);
    }
  }

  public Schema schema() {
    try {
      return schemaLoader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Profile profile(String name) throws IOException {
    return new ProfileLoader(fs, name)
        .schema(schema())
        .load();
  }

  public ProtoAdapter<Object> protoAdapter(String messageTypeName) throws IOException {
    Schema schema = schema();
    return schema.protoAdapter(messageTypeName, true);
  }

  public String generateCode(String typeName) throws IOException {
    return generateCode(typeName, null);
  }

  public String generateCode(String typeName, String profile) throws IOException {
    Schema schema = schema();
    JavaGenerator javaGenerator = JavaGenerator.get(schema);
    if (profile != null) {
      javaGenerator = javaGenerator.withProfile(profile(profile));
    }
    Type type = schema.getType(typeName);
    TypeSpec typeSpec = javaGenerator.generateType(type);
    ClassName typeName1 = javaGenerator.generatedTypeName(type);
    return JavaFile.builder(typeName1.packageName(), typeSpec).build().toString();
  }
}
