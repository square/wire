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

import com.squareup.wire.WireAdapter;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import okio.Okio;
import okio.Source;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;

/** Builds schemas for testing. */
class SchemaBuilder {
  final FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
  final Path root = fs.getPath("/");
  final List<Path> files = new ArrayList<>();

  public SchemaBuilder add(String name, String protoFile) {
    Path path = root.resolve(name);
    try {
      Path parent = path.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(path, protoFile.getBytes(UTF_8));
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    files.add(path);
    return this;
  }

  public SchemaBuilder add(String path) throws IOException {
    File file = new File("../wire-runtime/src/test/proto/" + path);
    try (Source source = Okio.source(file)) {
      String protoFile = Okio.buffer(source).readUtf8();
      return add(path, protoFile);
    }
  }

  public Schema build() {
    Loader loader = new Loader(singletonList(root));
    try {
      return loader.load(files);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public WireAdapter<Object> buildWireAdapter(String messageTypeName) {
    Schema schema = build();
    return schema.wireAdapter(messageTypeName, true);
  }
}
