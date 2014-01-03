package com.squareup.wire.parser;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;
import com.squareup.protoparser.Type;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.squareup.wire.parser.ProtoQualifier.fullyQualifyProtos;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;

/**
 * Intelligently parse {@code .proto} files into an object model which represents a set of types
 * and all of their transitive dependencies.
 * <p>
 * There are three sets of methods which control parsing:
 * <ul>
 * <li>{@link #addDirectory(File) addDirectory} or {@link #addDirectories(Collection)
 * addDirectories} specifies a directory under which proto files reside. Directories are used to
 * resolve {@code include} declarations. If no directories are specified, the current working
 * directory will be used.</li>
 * <li>{@link #addProto(File) addProto} or {@link #addProtos(Collection) addProtos} specifies which
 * proto files to parse. If no proto files are specified, all files under every directory will be
 * used.</li>
 * <li>{@link #addTypeRoot(String) addTypeRoot} or {@link #addTypeRoots(Collection) addTypeRoots}
 * specifies which types to include. If no types are specified, all types in every proto file will
 * be used.</li>
 * </ul>
 * Given no data, an instance of this class will recursively find all files in the current working
 * directory, attempt to parse them as protocol buffer definitions, and verify that all of the
 * dependencies of the types contained within those definitions are met.
 * <p>
 * The API of this class is meant to mimic the builder pattern and should be used as such.
 */
public class WireParser {
  private final Set<File> directories = new LinkedHashSet<File>();
  private final Set<File> protos = new LinkedHashSet<File>();
  private final Set<String> types = new LinkedHashSet<String>();

  private final Filesystem fs;

  public WireParser() {
    this(Filesystem.SYSTEM);
  }

  WireParser(Filesystem fs) {
    this.fs = fs;
  }

  /**
   * Add a directory under which proto files reside. {@code include} declarations will be resolved
   * from these directories.
   */
  public WireParser addDirectory(File directory) {
    checkNotNull(directory, "Directory must not be null.");
    directories.add(directory);
    return this;
  }

  /**
   * Add directories under which proto files reside. {@code include} declarations will be resolved
   * from these directories.
   */
  public WireParser addDirectories(Collection<File> directories) {
    checkNotNull(directories, "Directories must not be null.");
    checkArgument(!directories.isEmpty(), "Directories must not be empty.");
    for (File directory : directories) {
      addDirectory(directory);
    }
    return this;
  }

  /** Add a proto file to parse. */
  public WireParser addProto(File proto) {
    checkNotNull(proto, "Proto must not be null.");
    protos.add(proto);
    return this;
  }

  /** Add proto files to parse. */
  public WireParser addProtos(Collection<File> protos) {
    checkNotNull(protos, "Protos must not be null.");
    checkArgument(!protos.isEmpty(), "Protos must not be empty.");
    for (File proto : protos) {
      addProto(proto);
    }
    return this;
  }

  /**
   * Add a fully-qualified type to include in the parsed data. If specified, only these types and
   * their dependencies will be included. This allows for filtering message-heavy proto files such
   * that only desired message types are generated.
   */
  public WireParser addTypeRoot(String type) {
    checkNotNull(type, "Type must not be null.");
    checkArgument(!type.trim().isEmpty(), "Type must not be blank.");
    types.add(type);
    return this;
  }

  /**
   * Add fully-qualified types to include in the parsed data. If specified, only these types and
   * their dependencies will be included. This allows for filtering message-heavy proto files such
   * that only desired message types are generated.
   */
  public WireParser addTypeRoots(Collection<String> types) {
    checkNotNull(types, "Types must not be null.");
    checkArgument(!types.isEmpty(), "Types must not be empty.");
    for (String type : types) {
      addTypeRoot(type);
    }
    return this;
  }

  /**
   * Parse the supplied protos into an object model using the supplied information or their
   * respective defaults.
   * <p>
   * If no directories have been specified, the current working directory will be used. If no proto
   * files have been specified, every file in the specified directories will be used. If no types
   * have been specified, every type in the specified proto files will be used.
   */
  public Set<ProtoFile> parse() throws IOException {
    validateInputFiles();

    Set<File> directories = getOrFindDirectories();
    Set<File> protos = getOrFindProtos(directories);

    Set<ProtoFile> protoFiles = loadProtos(directories, protos);
    Set<String> allTypes = collectAllTypes(protoFiles);
    protoFiles = fullyQualifyProtos(protoFiles, allTypes);

    if (!types.isEmpty()) {
      protoFiles = RootsFilter.filter(protoFiles, types);
    }
    return protoFiles;
  }

  /** Verify all directories and protos exist and are valid file types. */
  void validateInputFiles() {
    // Validate all directories exist and are actually directories.
    for (File directory : directories) {
      checkState(fs.exists(directory), "Directory \"%s\" does not exist.", directory);
      checkState(fs.isDirectory(directory), "\"%s\" is not a directory.", directory);
    }
    // Validate all protos exist and are files.
    for (File proto : protos) {
      checkState(fs.exists(proto), "Proto \"%s\" does not exist.", proto);
      checkState(fs.isFile(proto), "Proto \"%s\" is not a file.", proto);
    }
  }

  /** Returns the set of supplied directories or only the current working directory. */
  Set<File> getOrFindDirectories() {
    if (!directories.isEmpty()) {
      return unmodifiableSet(directories);
    }

    // No directories given. Grab the user's working directory as the sole directory.
    String userDir = System.getProperty("user.dir");
    checkNotNull(userDir, "Unable to determine working directory.");
    return unmodifiableSet(singleton(new File(userDir)));
  }

  /** Returns the set of supplied proto files or all files under every directory. */
  Set<File> getOrFindProtos(Set<File> directories) {
    if (!protos.isEmpty()) {
      return unmodifiableSet(protos);
    }

    // No protos were explicitly given. Find all .proto files in each available directory.
    Set<File> protos = new LinkedHashSet<File>();

    Set<File> seenDirs = new LinkedHashSet<File>();
    Deque<File> dirQueue = new ArrayDeque<File>(directories);
    while (!dirQueue.isEmpty()) {
      File visitDir = dirQueue.removeLast();
      File[] files = fs.listFiles(visitDir);
      if (files != null) {
        for (File file : files) {
          if (fs.isDirectory(file) && !seenDirs.contains(file)) {
            seenDirs.add(file); // Prevent infinite recursion due to links.
            dirQueue.addLast(file);
          } else if (file.getName().endsWith(".proto")) {
            protos.add(file);
          }
        }
      }
    }
    return unmodifiableSet(protos);
  }

  /**
   * Returns a set of all protos from the supplied set parsed into an object model, searching in
   * the supplied directories for any dependencies.
   */
  private Set<ProtoFile> loadProtos(Set<File> directories, Set<File> protos) throws IOException {
    Set<ProtoFile> protoFiles = new LinkedHashSet<ProtoFile>();

    Deque<File> protoQueue = new ArrayDeque<File>(protos);
    Set<File> seenProtos = new LinkedHashSet<File>();
    while (!protoQueue.isEmpty()) {
      File proto = protoQueue.removeFirst();
      seenProtos.add(proto);

      String protoContent = fs.contentsUtf8(proto);
      ProtoFile protoFile = ProtoSchemaParser.parse(proto.getName(), protoContent);
      protoFiles.add(protoFile);

      // Queue all unseen dependencies to be resolved.
      for (String dependency : protoFile.getDependencies()) {
        File dependencyFile = resolveDependency(proto, directories, dependency);
        if (!seenProtos.contains(dependencyFile)) {
          protoQueue.addLast(dependencyFile);
        }
      }
    }
    return protoFiles;
  }

  /** Attempts to find a dependency's proto file in the supplied directories. */
  File resolveDependency(File proto, Set<File> directories, String dependency) {
    for (File directory : directories) {
      File dependencyFile = new File(directory, dependency);
      if (fs.exists(dependencyFile)) {
        return dependencyFile;
      }
    }

    StringBuilder error = new StringBuilder() //
        .append("Cannot resolve dependency \"")
        .append(dependency)
        .append("\" from \"")
        .append(proto.getAbsolutePath())
        .append("\" in:");
    for (File directory : directories) {
      error.append("\n  * ").append(directory.getAbsolutePath());
    }
    throw new IllegalStateException(error.toString());
  }

  /** Aggregate a set of all fully-qualified types contained in the supplied proto files. */
  static Set<String> collectAllTypes(Set<ProtoFile> protoFiles) {
    Set<String> types = new LinkedHashSet<String>();

    // Seed the type resolution queue with all the top-level types from each proto file.
    Deque<Type> typeQueue = new ArrayDeque<Type>();
    for (ProtoFile protoFile : protoFiles) {
      typeQueue.addAll(protoFile.getTypes());
    }

    while (!typeQueue.isEmpty()) {
      Type type = typeQueue.removeFirst();
      String typeFqName = type.getFullyQualifiedName();

      // Check for fully-qualified type name collisions.
      if (types.contains(typeFqName)) {
        throw new IllegalStateException(
            "Duplicate type " + typeFqName + " defined in " + Joiner.on(", ")
                .join(Iterables.transform(protoFiles, new Function<ProtoFile, String>() {
                  @Override public String apply(ProtoFile input) {
                    return input.getFileName();
                  }
                })));
      }
      types.add(typeFqName);

      typeQueue.addAll(type.getNestedTypes());
    }

    return unmodifiableSet(types);
  }
}
