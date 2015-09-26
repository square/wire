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

import com.google.common.collect.Iterables;
import com.google.common.io.Closer;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import okio.Okio;
import okio.Source;

/**
 * Load proto files and their transitive dependencies, parse them, and link them together.
 *
 * <p>To find proto files to load, a non-empty set of sources are searched. Each source is
 * either a regular directory or a ZIP file. Within ZIP files, proto files are expected to be found
 * relative to the root of the archive.
 */
public final class SchemaLoader {
  private final List<Path> sources = new ArrayList<>();
  private final List<String> protos = new ArrayList<>();

  /** Add directory or zip file source from which proto files will be loaded. */
  public SchemaLoader addSource(File file) {
    return addSource(file.toPath());
  }

  /** Add directory or zip file source from which proto files will be loaded. */
  public SchemaLoader addSource(Path path) {
    sources.add(path);
    return this;
  }

  /** Returns a mutable list of the sources that this loader will load from. */
  public List<Path> sources() {
    return sources;
  }

  /**
   * Add one or more proto files to load. Dependencies will be loaded automatically from the
   * configured sources.
   */
  public SchemaLoader addProto(String proto) {
    protos.add(proto);
    return this;
  }

  /** Returns a mutable list of the protos that this loader will load. */
  public List<String> protos() {
    return protos;
  }

  public Schema load() throws IOException {
    if (sources.isEmpty()) {
      throw new IllegalStateException("No sources added.");
    }

    try (Closer closer = Closer.create()) {
      List<Path> directories = new ArrayList<>();
      for (Path source : sources) {
        if (Files.isRegularFile(source)) {
          FileSystem sourceFs = FileSystems.newFileSystem(source, getClass().getClassLoader());
          closer.register(sourceFs);
          Iterables.addAll(directories, sourceFs.getRootDirectories());
        } else {
          directories.add(source);
        }
      }
      return loadZipsAndDirectories(directories);
    }
  }

  private Schema loadZipsAndDirectories(List<Path> directories) throws IOException {
    Deque<String> protos = new ArrayDeque<>(this.protos);

    Map<String, ProtoFile> loaded = new LinkedHashMap<>();
    while (!protos.isEmpty()) {
      String proto = protos.removeFirst();
      if (loaded.containsKey(proto)) {
        continue;
      }

      ProtoFileElement element = null;
      for (Path directory : directories) {
        Source source = source(proto, directory);
        if (source == null) {
          continue;
        }

        try {
          Location location = Location.get(directory.toString(), proto);
          String data = Okio.buffer(source).readUtf8();
          element = ProtoParser.parse(location, data);
        } catch (IOException e) {
          throw new IOException("Failed to load " + proto + " from " + directory, e);
        } finally {
          source.close();
        }
      }
      if (element == null) {
        throw new FileNotFoundException("Failed to locate " + proto + " in " + sources);
      }

      ProtoFile protoFile = ProtoFile.get(element);
      loaded.put(proto, protoFile);

      // Queue dependencies to be loaded.
      for (String importPath : element.imports()) {
        protos.addLast(importPath);
      }
    }

    return new Linker(loaded.values()).link();
  }

  private static Source source(String proto, Path directory) throws IOException {
    Path resolvedPath = directory.resolve(proto);
    if (Files.exists(resolvedPath)) {
      return Okio.source(resolvedPath);
    }
    return null;
  }
}
