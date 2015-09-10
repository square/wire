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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import okio.Okio;
import okio.Source;

/**
 * Load proto files and their transitive dependencies, parse them, and link them together.
 *
 * <p>To find proto files to load, a non-empty set of directories is searched. Each directory is
 * either a regular directory or a ZIP file. Within ZIP files, proto files are expected to be found
 * relative to the root of the archive.
 */
public final class SchemaLoader {
  private static final Function<File, Path> FILE_TO_PATH = new Function<File, Path>() {
    @Override public Path apply(File file) {
      return file.toPath();
    }
  };

  private final ImmutableList.Builder<Path> directories = ImmutableList.builder();
  private final ImmutableList.Builder<Path> protos = ImmutableList.builder();

  /** Open ZIP files that need to be released. Only non-empty while loading. */
  private final Map<Path, ZipFile> zipFiles = new LinkedHashMap<>();

  /** Add one or more directories from which proto files will be loaded. */
  public SchemaLoader addDirectory(File file) {
    return addDirectory(file.toPath());
  }

  /** Add directories from which proto files will be loaded. */
  public SchemaLoader addDirectories(Collection<? extends File> files) {
    return addDirectories(Iterables.transform(files, FILE_TO_PATH));
  }

  /** Add one or more directories from which proto files will be loaded. */
  public SchemaLoader addDirectory(Path path) {
    directories.add(path);
    return this;
  }

  /** Add directories from which proto files will be loaded. */
  public SchemaLoader addDirectories(Iterable<? extends Path> paths) {
    directories.addAll(paths);
    return this;
  }

  /**
   * Add one or more proto files to load. Dependencies will be loaded automatically from the
   * configured directories.
   */
  public SchemaLoader addProto(File file) {
    return addProto(file.toPath());
  }

  /**
   * Add proto files to load. Dependencies will be loaded automatically from the configured
   * directories.
   */
  public SchemaLoader addProtos(Collection<? extends File> files) {
    return addProtos(Iterables.transform(files, FILE_TO_PATH));
  }

  /**
   * Add one or more proto files to load. Dependencies will be loaded automatically from the
   * configured directories.
   */
  public SchemaLoader addProto(Path path) {
    protos.add(path);
    return this;
  }

  /**
   * Add proto files to load. Dependencies will be loaded automatically from the configured
   * directories.
   */
  public SchemaLoader addProtos(Iterable<? extends Path> paths) {
    protos.addAll(paths);
    return this;
  }

  public Schema load() throws IOException {
    ImmutableList<Path> directories = this.directories.build();
    if (directories.isEmpty()) {
      throw new IllegalStateException("No directories added.");
    }

    try {
      // Attempt to load open each directory entry as a ZIP file. Most will fail; the rest are
      // real ZIP files. We could ask first "is this a ZIP file" but trying and failing is easier!
      for (Path directory : directories) {
        if (Files.isRegularFile(directory)) {
          try {
            ZipFile zipFile = new ZipFile(directory.toFile());
            zipFiles.put(directory, zipFile);
          } catch (IOException ignored) {
            // Not a ZIP file?!
          }
        }
      }
      return load(directories);
    } finally {
      for (ZipFile zipFile : zipFiles.values()) {
        try {
          zipFile.close();
        } catch (IOException ignored) {
        }
      }
      zipFiles.clear();
    }
  }

  private Schema load(List<Path> directories) throws IOException {
    Deque<Path> protos = new ArrayDeque<>(this.protos.build());
    if (protos.isEmpty()) {
      // TODO traverse all files in every directory.
    }

    Map<Path, ProtoFile> loaded = new LinkedHashMap<>();
    while (!protos.isEmpty()) {
      Path proto = protos.removeFirst();
      if (loaded.containsKey(proto)) {
        continue;
      }

      ProtoFileElement element = null;
      for (Path directory : directories) {
        if (proto.isAbsolute() && !proto.startsWith(directory)) {
          continue;
        }
        Source source = source(proto, directory);
        if (source == null) {
          continue;
        }

        try {
          Location location = Location.get(directory.toString(), proto.toString());
          String data = Okio.buffer(source).readUtf8();
          element = ProtoParser.parse(location, data);
        } catch (IOException e) {
          throw new IOException("Failed to load " + proto + " from " + directory, e);
        } finally {
          source.close();
        }
      }
      if (element == null) {
        throw new FileNotFoundException("Failed to locate " + proto + " in " + directories);
      }

      ProtoFile protoFile = ProtoFile.get(element);
      loaded.put(proto, protoFile);

      // Queue dependencies to be loaded.
      FileSystem fs = proto.getFileSystem();
      for (String importPath : element.imports()) {
        protos.addLast(fs.getPath(importPath));
      }
    }

    return new Linker(loaded.values()).link();
  }

  private Source source(Path proto, Path directory) throws IOException {
    ZipFile zipFile = zipFiles.get(directory);
    ZipEntry zipEntry;
    if (zipFile != null && (zipEntry = zipFile.getEntry(proto.toString())) != null) {
      return Okio.source(zipFile.getInputStream(zipEntry));
    }

    Path resolvedPath = directory.resolve(proto);
    if (Files.exists(resolvedPath)) {
      return Okio.source(resolvedPath);
    }

    return null;
  }
}
