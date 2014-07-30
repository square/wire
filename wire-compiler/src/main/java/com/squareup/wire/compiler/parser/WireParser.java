package com.squareup.wire.compiler.parser;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;
import com.squareup.protoparser.Type;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.squareup.wire.compiler.parser.ProtoQualifier.fullyQualifyProtos;

/**
 * Intelligently parse {@code .proto} files into an object model which represents a set of types
 * and all of their transitive dependencies.
 * <p>
 * There are three sets of methods which control parsing:
 * <ul>
 * <li>{@link #addDirectory(Path) addDirectory} or {@link #addDirectories(Iterable)
 * addDirectories} specifies a directory under which proto files reside. Directories are used to
 * resolve {@code include} declarations. If no directories are specified, the current working
 * directory will be used.</li>
 * <li>{@link #addProto(Path) addProto} or {@link #addProtos(Iterable) addProtos} specifies which
 * proto files to parse. If no proto files are specified, all files under every directory will be
 * used.</li>
 * <li>{@link #addTypeRoot(String) addTypeRoot} specifies which types to include. If no types are
 * specified, all types in every proto file will be used.</li>
 * </ul>
 * Given no data, an instance of this class will recursively find all files in the current working
 * directory, attempt to parse them as protocol buffer definitions, and verify that all of the
 * dependencies of the types contained within those definitions are met.
 * <p>
 * The API of this class is meant to mimic the builder pattern and should be used as such.
 */
public final class WireParser {

  public static class ParsedInput {
    public final Set<ProtoFile> protoFiles;
    public final Set<String> allTypes;
    public final Map<String, String> enumTypes;
    public final List<String> enumOptions;

    public ParsedInput(Set<ProtoFile> protoFiles, Set<String> allTypes,
        Map<String, String> enumTypes, List<String> enumOptions) {
      this.protoFiles = Collections.unmodifiableSet(protoFiles);
      this.allTypes = Collections.unmodifiableSet(allTypes);
      this.enumTypes = Collections.unmodifiableMap(enumTypes);
      this.enumOptions = Collections.unmodifiableList(enumOptions);
    }
  }

  /**
   * Create an instance of {@link WireParser} using the {@link FileSystems#getDefault() default
   * file system}.
   */
  public static WireParser create() {
    return new WireParser(FileSystems.getDefault());
  }

  /** Create an instance of {@link WireParser} using the supplied {@link FileSystem file system}. */
  public static WireParser createWithFileSystem(FileSystem fs) {
    return new WireParser(fs);
  }

  private final FileSystem fs;
  private final Set<Path> directories = new LinkedHashSet<>();
  private final Set<Path> protos = new LinkedHashSet<>();
  private final Set<String> types = new LinkedHashSet<>();
  private final List<String> enumOptions = new ArrayList<>();
  private boolean emitOptions = true;

  private WireParser(FileSystem fs) {
    this.fs = fs;
  }

  /**
   * Add a directory under which proto files reside. {@code include} declarations will be resolved
   * from these directories.
   */
  public WireParser addDirectory(Path directory) {
    checkNotNull(directory, "Directory must not be null.");
    directories.add(directory);
    return this;
  }

  /**
   * Add directories under which proto files reside. {@code include} declarations will be resolved
   * from these directories.
   */
  public WireParser addDirectories(Iterable<Path> directories) {
    checkNotNull(directories, "Directories must not be null.");
    for (Path directory : directories) {
      addDirectory(directory);
    }
    return this;
  }

  /** Add a proto file to parse. */
  public WireParser addProto(Path proto) {
    checkNotNull(proto, "Proto must not be null.");
    protos.add(proto);
    return this;
  }

  /** Add proto files to parse. */
  public WireParser addProtos(Iterable<Path> protos) {
    checkNotNull(protos, "Protos must not be null.");
    for (Path proto : protos) {
      addProto(proto);
    }
    return this;
  }

  public WireParser addEnumOption(String enumOption) {
    enumOptions.add(enumOption);
    return this;
  }

  public WireParser setNoOptions() {
    emitOptions = false;
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
   * Parse the supplied protos into an object model using the supplied information or their
   * respective defaults.
   * <p>
   * If no directories have been specified, the current working directory will be used. If no proto
   * files have been specified, every file in the specified directories will be used. If no types
   * have been specified, every type in the specified proto files will be used.
   */
  public ParsedInput parse() throws IOException {
    validateInputFiles();

    Set<Path> directories = getOrFindDirectories();
    Set<Path> protos = getOrFindProtos(directories);

    Set<ProtoFile> protoFiles = loadProtos(directories, protos);
    Set<String> allTypes = collectAllTypes(protoFiles);
    protoFiles = fullyQualifyProtos(protoFiles, allTypes);

    if (!types.isEmpty()) {
      protoFiles = RootsFilter.filter(protoFiles, types);
    }

    Map<String, String> enumTypes = collectEnumTypes(protoFiles);
    return new ParsedInput(protoFiles, allTypes, enumTypes, enumOptions);
  }

  /** Verify all directories and protos exist and are valid file types. */
  void validateInputFiles() {
    // Validate all directories exist and are actually directories.
    for (Path directory : directories) {
      checkState(Files.exists(directory), "Directory \"%s\" does not exist.", directory);
      checkState(Files.isDirectory(directory), "\"%s\" is not a directory.", directory);
    }
    // Validate all protos exist and are files.
    for (Path proto : protos) {
      checkState(Files.exists(proto), "Proto \"%s\" does not exist.", proto);
      checkState(Files.isRegularFile(proto), "Proto \"%s\" is not a file.", proto);
    }
  }

  /** Returns the set of supplied directories or only the current working directory. */
  Set<Path> getOrFindDirectories() {
    if (!directories.isEmpty()) {
      return ImmutableSet.copyOf(directories);
    }

    // No directories given. Use the current directory.
    return ImmutableSet.of(fs.getPath("."));
  }

  /** Returns the set of supplied proto files or all files under every directory. */
  Set<Path> getOrFindProtos(Set<Path> directories) throws IOException {
    if (!protos.isEmpty()) {
      return ImmutableSet.copyOf(protos);
    }

    // No protos were explicitly given. Find all .proto files in each available directory.
    final Set<Path> protos = new LinkedHashSet<>();
    for (Path directory : directories) {
      Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException {
          if (file.getFileName().toString().endsWith(".proto")) {
            protos.add(file);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    }
    return ImmutableSet.copyOf(protos);
  }

  /**
   * Returns a set of all protos from the supplied set parsed into an object model, searching in
   * the supplied directories for any dependencies.
   */
  private Set<ProtoFile> loadProtos(Set<Path> directories, Set<Path> protos) throws IOException {
    Set<ProtoFile> protoFiles = new LinkedHashSet<>();

    Deque<Path> protoQueue = new ArrayDeque<>(protos);
    Set<Path> seenProtos = new LinkedHashSet<>();
    while (!protoQueue.isEmpty()) {
      Path proto = protoQueue.removeFirst();
      seenProtos.add(proto);

      String protoContent = new String(Files.readAllBytes(proto), Charsets.UTF_8);
      ProtoFile protoFile = ProtoSchemaParser.parse(proto.toString(), protoContent);
      protoFiles.add(protoFile);

      // Queue all unseen dependencies to be resolved.
      for (String dependency : protoFile.getDependencies()) {
        Path dependencyFile = resolveDependency(proto, directories, dependency);
        if (!seenProtos.contains(dependencyFile)) {
          protoQueue.addLast(dependencyFile);
        }
      }
    }
    return protoFiles;
  }

  /** Attempts to find a dependency's proto file in the supplied directories. */
  Path resolveDependency(Path proto, Set<Path> directories, String dependency) {
    for (Path directory : directories) {
      Path dependencyPath = directory.resolve(dependency);
      if (Files.exists(dependencyPath)) {
        return dependencyPath;
      }
    }

    StringBuilder error = new StringBuilder() //
        .append("Cannot resolve dependency \"")
        .append(dependency)
        .append("\" from \"")
        .append(proto.toAbsolutePath())
        .append("\" in:");
    for (Path directory : directories) {
      error.append("\n  * ").append(directory.toAbsolutePath());
    }
    throw new IllegalStateException(error.toString());
  }

  /** Aggregate a set of all fully-qualified types contained in the supplied proto files. */
  static Set<String> collectAllTypes(final Set<ProtoFile> protoFiles) {
    final Set<String> types = new LinkedHashSet<>();
    new AllTypesVisitor(protoFiles) {
      @Override public void visitType(Type type) {
        String typeFqName = type.getFullyQualifiedName();

        // Check for fully-qualified type name collisions.
        if (types.contains(typeFqName)) {
          throw new IllegalStateException(
              "Duplicate type " + typeFqName + " defined in " + Joiner.on(", ")
                  .join(Iterables.transform(protoFiles, new Function<ProtoFile, String>() {
                    @Override public String apply(ProtoFile input) {
                      return input.getFileName();
                    }
                  }))
          );
        }
        types.add(typeFqName);
      }
    }.visit();

    return ImmutableSet.copyOf(types);
  }

  static Map<String, String> collectEnumTypes(Set<ProtoFile> protoFiles) {
    final Map<String, String> enumTypes = new LinkedHashMap<>();
    new AllTypesVisitor(protoFiles) {
      @Override public void visitEnumType(EnumType enumType) {
        enumTypes.put(enumType.getFullyQualifiedName(),
            ((EnumType) enumType).getValues().get(0).getName());
      }
    }.visit();

    return ImmutableMap.copyOf(enumTypes);
  }
}
