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

import com.squareup.wire.internal.protoparser.ProtoFileElement;
import com.squareup.wire.internal.protoparser.ProtoParser;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import okio.Okio;
import okio.Source;

/** Load proto files and their transitive dependencies, parse them, and link them together. */
public final class Loader {
  private final IO io;
  private final Map<String, ProtoFile> loaded = new LinkedHashMap<>();

  public Loader(IO io) {
    this.io = io;
  }

  /** Returns a loader that loads all files from {@code base}. */
  public static Loader forBaseDirectory(final String base) {
    IO io = new IO() {
      @Override public Location locate(String path) throws IOException {
        return Location.get(base, path);
      }

      @Override public Source open(Location location) throws IOException {
        String sourcePath = location.base() + File.separator + location.path();
        return Okio.source(new File(sourcePath));
      }

      @Override public String toString() {
        return String.format("Loader(%s)", base);
      }
    };
    return new Loader(io);
  }

  public Schema load(Iterable<String> sourceFiles) throws IOException {
    for (String path : sourceFiles) {
      load(path);
    }
    return new Linker(loaded.values()).link();
  }

  private void load(String path) throws IOException {
    if (loaded.containsKey(path)) {
      return;
    }

    Location location;
    try {
      location = io.locate(path);
    } catch (IOException e) {
      throw new IOException("Failed to locate " + path + " with " + io, e);
    }

    ProtoFileElement element;
    try (Source source = io.open(location)) {
      String data = Okio.buffer(source).readUtf8();
      element = ProtoParser.parse(location, data);
    } catch (IOException e) {
      throw new IOException("Failed to load " + location + " with " + io, e);
    }

    ProtoFile protoFile = ProtoFile.get(element);
    loaded.put(path, protoFile);

    // Recursively load dependencies.
    for (String dependency : element.dependencies()) {
      load(dependency);
    }
  }

  public interface IO {
    /**
     * Search for the location that holds the {@code .proto} file with path {@code path}. This may
     * resolve using a single directory, a .jar file, or potentially a search path with both.
     */
    Location locate(String path) throws IOException;

    /**
     * Returns a source to read the {@code .proto} file at {@code location}. The location argument
     * is always a return value from a previous call to {@code #locate}.
     */
    Source open(Location location) throws IOException;
  }
}
