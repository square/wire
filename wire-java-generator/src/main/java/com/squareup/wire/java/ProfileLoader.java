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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.Closer;
import com.squareup.wire.java.internal.ProfileFileElement;
import com.squareup.wire.java.internal.ProfileParser;
import com.squareup.wire.java.internal.TypeConfigElement;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaException;
import com.squareup.wire.schema.Type;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okio.Okio;
import okio.Source;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;

/**
 * Load files with the {@code .wire} suffix for a schema. The file name is the profile name; for
 * example the {@code android} profile loads {@code android.wire} files.
 */
public final class ProfileLoader {
  private final FileSystem fileSystem;
  private final String name;
  private Schema schema;

  public ProfileLoader(FileSystem fileSystem, String name) {
    this.fileSystem = fileSystem;
    this.name = name;
  }

  public ProfileLoader(String name) {
    this(FileSystems.getDefault(), name);
  }

  public ProfileLoader schema(Schema schema) {
    checkState(this.schema == null);
    this.schema = schema;
    return this;
  }

  public Profile load() throws IOException {
    Set<Location> protoLocations = new LinkedHashSet<>();
    for (ProtoFile protoFile : schema.protoFiles()) {
      protoLocations.add(protoFile.location());
    }
    Multimap<Path, String> pathsToAttempt = pathsToAttempt(protoLocations);

    ImmutableList<ProfileFileElement> profileFiles = loadProfileFiles(pathsToAttempt);
    Profile profile = new Profile(profileFiles);
    validate(schema, profileFiles);
    return profile;
  }

  private ImmutableList<ProfileFileElement> loadProfileFiles(Multimap<Path, String> pathsToAttempt)
      throws IOException {
    ImmutableList.Builder<ProfileFileElement> result = ImmutableList.builder();
    try (Closer closer = Closer.create()) {
      for (Map.Entry<Path, Collection<String>> entry : pathsToAttempt.asMap().entrySet()) {
        Path base = entry.getKey();
        if (Files.isRegularFile(base)) {
          FileSystem sourceFs = FileSystems.newFileSystem(base, getClass().getClassLoader());
          closer.register(sourceFs);
          base = getOnlyElement(sourceFs.getRootDirectories());
        }
        for (String path : entry.getValue()) {
          ProfileFileElement element = loadProfileFile(base, path);
          if (element != null) result.add(element);
        }
      }
    }
    return result.build();
  }

  /**
   * Returns a multimap whose keys are base directories and whose values are potential locations of
   * wire profile files.
   */
  Multimap<Path, String> pathsToAttempt(Set<Location> protoLocations) {
    Multimap<Path, String> result = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();
    for (Location location : protoLocations) {
      pathsToAttempt(result, location);
    }
    return result;
  }

  /**
   * Computes all possible {@code .wire} profile files for the {@code .proto} at {@code location}
   * and adds them to {@code result}.
   */
  void pathsToAttempt(Multimap<Path, String> sink, Location location) {
    Path base = fileSystem.getPath(location.base());

    String path = location.path();
    while (!path.isEmpty()) {
      String parent = path.substring(0, path.lastIndexOf('/', path.length() - 2) + 1);
      String profilePath = parent + name + ".wire";
      sink.put(base, profilePath);
      path = parent;
    }
  }

  /**
   * Parses the {@code .wire} file at {@code base/path} and returns it. Returns null if no such
   * file exists.
   */
  private ProfileFileElement loadProfileFile(Path base, String path) throws IOException {
    Source source = source(base, path);
    if (source == null) return null;
    try {
      Location location = Location.get(base.toString(), path);
      String data = Okio.buffer(source).readUtf8();
      return new ProfileParser(location, data).read();
    } catch (IOException e) {
      throw new IOException("Failed to load " + source + " from " + base, e);
    } finally {
      source.close();
    }
  }

  /** Confirms that {@code protoFiles} link correctly against {@code schema}. */
  void validate(Schema schema, ImmutableList<ProfileFileElement> profileFiles) {
    List<String> errors = new ArrayList<>();

    for (ProfileFileElement profileFile : profileFiles) {
      for (TypeConfigElement typeConfig : profileFile.typeConfigs()) {
        ProtoType type = importedType(ProtoType.get(typeConfig.type()));
        if (type == null) continue;

        Type resolvedType = schema.getType(type);
        if (resolvedType == null) {
          errors.add(String.format("unable to resolve %s (%s)",
              type, typeConfig.location()));
          continue;
        }

        String requiredImport = resolvedType.location().path();
        if (!profileFile.imports().contains(requiredImport)) {
          errors.add(String.format("%s needs to import %s (%s)",
              typeConfig.location().path(), requiredImport, typeConfig.location()));
        }
      }
    }

    if (!errors.isEmpty()) {
      throw new SchemaException(errors);
    }
  }

  /** Returns the type to import for {@code type}. */
  private ProtoType importedType(ProtoType type) {
    // Map key type is always scalar.
    if (type.isMap()) type = type.valueType();
    return type.isScalar() ? null : type;
  }

  private static Source source(Path base, String path) throws IOException {
    Path resolvedPath = base.resolve(path);
    if (Files.exists(resolvedPath)) {
      return Okio.source(resolvedPath);
    }
    return null;
  }
}
