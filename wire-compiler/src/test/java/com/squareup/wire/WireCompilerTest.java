/*
 * Copyright 2013 Square Inc.
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
package com.squareup.wire;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import okio.Okio;
import okio.Source;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class WireCompilerTest {
  @Rule public final TemporaryFolder temp = new TemporaryFolder();

  private Path protoDir;
  private Path outputDir;
  private StringWireLogger logger;

  @Before public void setUp() throws IOException {
    protoDir = temp.newFolder().toPath();
    outputDir = temp.newFolder().toPath();
  }

  private void testProto(String[] sources, String[] outputs, String[] flags, String suffix)
      throws Exception {
    // Copy the listed sources to the proto directory.
    Path sourceDir = Paths.get("../wire-runtime/src/test/proto/");
    for (String source : sources) {
      Path sourceFile = sourceDir.resolve(source);
      Path protoFile = protoDir.resolve(source);

      Path parent = protoFile.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      Files.copy(sourceFile, protoFile);
    }

    List<String> args = new ArrayList<>();
    args.add("--proto_path=" + protoDir);
    args.add("--java_out=" + outputDir);
    Collections.addAll(args, flags);

    CommandLineOptions options = new CommandLineOptions(args);
    logger = new StringWireLogger(options.quiet);
    FileSystem fs = FileSystems.getDefault();
    new WireCompiler(options, fs, logger).compile();

    List<Path> filesAfter = getAllFiles();
    assertThat(filesAfter.size())
        .overridingErrorMessage(filesAfter.toString())
        .isEqualTo(outputs.length);

    for (String output : outputs) {
      assertFilesMatch(output, suffix);
    }
  }

  private void testProto(String[] sources, String[] outputs) throws Exception {
    String[] flags = {
        "--enum_options=squareup.protos.custom_options.enum_value_option,"
            + "squareup.protos.custom_options.complex_enum_value_option,"
            + "squareup.protos.foreign.foreign_enum_value_option"
    };
    testProto(sources, outputs, flags, "");
  }

  private void testProtoAndroid(String[] sources, String[] outputs) throws Exception {
    String[] flags = {
        "--enum_options=squareup.protos.custom_options.enum_value_option,"
            + "squareup.protos.custom_options.complex_enum_value_option,"
            + "squareup.protos.foreign.foreign_enum_value_option",
        "--android"
    };
    testProto(sources, outputs, flags, ".android");
  }

  private void testProtoNoOptions(String[] sources, String[] outputs) throws Exception {
    String[] flags = {
        "--no_options",
        // Emit one of the enum options anyway.
        "--enum_options=squareup.protos.custom_options.enum_value_option"
    };
    testProto(sources, outputs, flags, ".noOptions");
  }

  private void testProtoWithRegistry(String[] sources, String registryClass, String[] outputs)
      throws Exception {
    String[] flags = {
        "--registry_class=" + registryClass
    };
    testProto(sources, outputs, flags, "");
  }

  private void testProtoWithRoots(String[] sources, String roots, String[] outputs)
      throws Exception {
    String[] flags = {
        "--roots=" + roots
    };
    testProto(sources, outputs, flags, "");
  }

  @Test public void testFooBar() throws Exception {
    String[] sources = {
        "foo.proto",
        "bar.proto"
    };
    String[] outputs = {
        "com/squareup/foobar/protos/bar/Bar.java",
        "com/squareup/foobar/protos/foo/Foo.java"
    };
    testProto(sources, outputs);
  }

  @Test public void testDifferentPackageFooBar() throws Exception {
    String[] sources = {
        "differentpackage/foo.proto",
        "differentpackage/bar.proto"
    };
    String[] outputs = {
        "com/squareup/differentpackage/protos/bar/Bar.java",
        "com/squareup/differentpackage/protos/foo/Foo.java"
    };
    testProto(sources, outputs);
  }

  @Test public void testPerson() throws Exception {
    String[] sources = {
        "person.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/person/Person.java"
    };
    testProto(sources, outputs);
  }

  @Test public void testPersonAndroid() throws Exception {
    String[] sources = {
        "person.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/person/Person.java"
    };
    testProtoAndroid(sources, outputs);
  }

  @Test public void testSimple() throws Exception {
    String[] sources = {
        "simple_message.proto",
        "external_message.proto",
        "foreign.proto",
        "google/protobuf/descriptor.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/simple/Ext_simple_message.java",
        "com/squareup/wire/protos/simple/SimpleMessage.java",
        "com/squareup/wire/protos/simple/ExternalMessage.java",
        "com/squareup/wire/protos/foreign/Ext_foreign.java",
        "com/squareup/wire/protos/foreign/ForeignEnum.java",
        "com/squareup/wire/protos/foreign/ForeignMessage.java"
    };
    testProto(sources, outputs);
  }

  @Test public void testOneOf() throws Exception {
    String[] sources = {
        "one_of.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/oneof/OneOfMessage.java"
    };
    testProto(sources, outputs);
  }

  @Test public void testRegistry() throws Exception {
    String[] sources = {
        "simple_message.proto",
        "external_message.proto",
        "foreign.proto",
        "google/protobuf/descriptor.proto"
    };
    String registry = "com.squareup.wire.protos.ProtoRegistry";
    String[] outputs = {
        "com/squareup/wire/protos/ProtoRegistry.java",
        "com/squareup/wire/protos/simple/Ext_simple_message.java",
        "com/squareup/wire/protos/simple/SimpleMessage.java",
        "com/squareup/wire/protos/simple/ExternalMessage.java",
        "com/squareup/wire/protos/foreign/Ext_foreign.java",
        "com/squareup/wire/protos/foreign/ForeignEnum.java",
        "com/squareup/wire/protos/foreign/ForeignMessage.java"
    };
    testProtoWithRegistry(sources, registry, outputs);
  }

  @Test public void testEmptyRegistry() throws Exception {
    String[] sources = {
        "person.proto"
    };
    String registry = "com.squareup.wire.protos.person.EmptyRegistry";
    String[] outputs = {
        "com/squareup/wire/protos/person/EmptyRegistry.java",
        "com/squareup/wire/protos/person/Person.java"
    };
    testProtoWithRegistry(sources, registry, outputs);
  }

  @Test public void testOneClassRegistry() throws Exception {
    String[] sources = {
        "one_extension.proto"
    };
    String registry = "com.squareup.wire.protos.one_extension.OneExtensionRegistry";
    String[] outputs = {
        "com/squareup/wire/protos/one_extension/Ext_one_extension.java",
        "com/squareup/wire/protos/one_extension/Foo.java",
        "com/squareup/wire/protos/one_extension/OneExtension.java",
        "com/squareup/wire/protos/one_extension/OneExtensionRegistry.java"
    };
    testProtoWithRegistry(sources, registry, outputs);
  }

  @Test public void testSingleLevel() throws Exception {
    String[] sources = {
        "single_level.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/single_level/Foo.java",
        "com/squareup/wire/protos/single_level/Foos.java",
    };
    testProto(sources, outputs);
  }

  @Test public void testSameBasename() throws Exception {
    String[] sources = {
        "single_level.proto",
        "samebasename/single_level.proto" };
    String[] outputs = {
        "com/squareup/wire/protos/single_level/Foo.java",
        "com/squareup/wire/protos/single_level/Foos.java",
        "com/squareup/wire/protos/single_level/Bar.java",
        "com/squareup/wire/protos/single_level/Bars.java",
    };
    testProto(sources, outputs);
  }

  @Test public void testChildPackage() throws Exception {
    String[] sources = {
        "child_pkg.proto",
        "foreign.proto",
        "google/protobuf/descriptor.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/ChildPackage.java",
        "com/squareup/wire/protos/foreign/Ext_foreign.java",
        "com/squareup/wire/protos/foreign/ForeignEnum.java",
        "com/squareup/wire/protos/foreign/ForeignMessage.java"
    };
    testProto(sources, outputs);
  }

  @Test public void testAllTypes() throws Exception {
    String[] sources = {
        "all_types.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/alltypes/Ext_all_types.java",
        "com/squareup/wire/protos/alltypes/AllTypes.java"
    };
    testProto(sources, outputs);
  }

  @Test public void testEdgeCases() throws Exception {
    String[] sources = {
        "edge_cases.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/edgecases/NoFields.java",
        "com/squareup/wire/protos/edgecases/OneField.java",
        "com/squareup/wire/protos/edgecases/OneBytesField.java",
        "com/squareup/wire/protos/edgecases/Recursive.java"
    };
    testProto(sources, outputs);
  }

  @Test public void testUnknownFields() throws Exception {
    String[] sources = {
        "unknown_fields.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/unknownfields/VersionOne.java",
        "com/squareup/wire/protos/unknownfields/VersionTwo.java"
    };
    testProto(sources, outputs);
  }

  @Test public void testCustomOptions() throws Exception {
    String[] sources = {
        "custom_options.proto",
        "foreign.proto",
        "google/protobuf/descriptor.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/custom_options/FooBar.java",
        "com/squareup/wire/protos/custom_options/Ext_custom_options.java",
        "com/squareup/wire/protos/custom_options/MessageWithOptions.java",
        "com/squareup/wire/protos/foreign/Ext_foreign.java",
        "com/squareup/wire/protos/foreign/ForeignEnum.java",
        "com/squareup/wire/protos/foreign/ForeignMessage.java"
    };
    testProto(sources, outputs);
  }

  @Test public void testCustomOptionsNoOptions() throws Exception {
    String[] sources = {
        "custom_options.proto",
        "foreign.proto",
        "google/protobuf/descriptor.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/custom_options/FooBar.java",
        "com/squareup/wire/protos/custom_options/Ext_custom_options.java",
        "com/squareup/wire/protos/custom_options/MessageWithOptions.java",
        "com/squareup/wire/protos/foreign/Ext_foreign.java",
        "com/squareup/wire/protos/foreign/ForeignEnum.java",
        "com/squareup/wire/protos/foreign/ForeignMessage.java"
    };
    testProtoNoOptions(sources, outputs);
  }

  @Test public void testRedacted() throws Exception {
    String[] sources = {
        "redacted_test.proto",
        "google/protobuf/descriptor.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/redacted/Ext_redacted_test.java",
        "com/squareup/wire/protos/redacted/NotRedacted.java",
        "com/squareup/wire/protos/redacted/Redacted.java",
        "com/squareup/wire/protos/redacted/RedactedChild.java",
        "com/squareup/wire/protos/redacted/RedactedCycleA.java",
        "com/squareup/wire/protos/redacted/RedactedCycleB.java",
        "com/squareup/wire/protos/redacted/RedactedExtension.java",
        "com/squareup/wire/protos/redacted/RedactedRepeated.java",
        "com/squareup/wire/protos/redacted/RedactedRequired.java",
    };
    testProto(sources, outputs);
  }

  @Test public void testNoRoots() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/roots/A.java",
        "com/squareup/wire/protos/roots/B.java",
        "com/squareup/wire/protos/roots/C.java",
        "com/squareup/wire/protos/roots/D.java",
        "com/squareup/wire/protos/roots/E.java",
        "com/squareup/wire/protos/roots/G.java",
        "com/squareup/wire/protos/roots/H.java",
        "com/squareup/wire/protos/roots/I.java",
        "com/squareup/wire/protos/roots/J.java",
        "com/squareup/wire/protos/roots/K.java",
        "com/squareup/wire/protos/roots/Ext_roots.java"
    };
    testProto(sources, outputs);
  }

  @Test public void testRootsA() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/roots/A.java",
        "com/squareup/wire/protos/roots/B.java",
        "com/squareup/wire/protos/roots/C.java",
        "com/squareup/wire/protos/roots/D.java",
        "com/squareup/wire/protos/roots/I.java",
        "com/squareup/wire/protos/roots/J.java",
        "com/squareup/wire/protos/roots/K.java",
        "com/squareup/wire/protos/roots/Ext_roots.java"
    };
    String roots = "squareup.protos.roots.A";
    testProtoWithRoots(sources, roots, outputs);
  }

  @Test public void testRootsB() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/roots/B.java",
        "com/squareup/wire/protos/roots/C.java",
        "com/squareup/wire/protos/roots/I.java",
        "com/squareup/wire/protos/roots/J.java",
        "com/squareup/wire/protos/roots/K.java",
        "com/squareup/wire/protos/roots/Ext_roots.java"
    };
    String roots = "squareup.protos.roots.B";
    testProtoWithRoots(sources, roots, outputs);
  }

  @Test public void testRootsE() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/roots/E.java",
        "com/squareup/wire/protos/roots/G.java",
        "com/squareup/wire/protos/roots/I.java",
        "com/squareup/wire/protos/roots/J.java",
        "com/squareup/wire/protos/roots/K.java",
        "com/squareup/wire/protos/roots/Ext_roots.java"
    };
    String roots = "squareup.protos.roots.E";
    testProtoWithRoots(sources, roots, outputs);
  }

  @Test public void testRootsH() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/roots/E.java",
        "com/squareup/wire/protos/roots/G.java",
        "com/squareup/wire/protos/roots/H.java",
        "com/squareup/wire/protos/roots/I.java",
        "com/squareup/wire/protos/roots/J.java",
        "com/squareup/wire/protos/roots/K.java",
        "com/squareup/wire/protos/roots/Ext_roots.java"
    };
    String roots = "squareup.protos.roots.H";
    testProtoWithRoots(sources, roots, outputs);
  }

  @Test public void testRootsI() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/roots/I.java",
        "com/squareup/wire/protos/roots/J.java",
        "com/squareup/wire/protos/roots/K.java",
        "com/squareup/wire/protos/roots/Ext_roots.java"
    };
    String roots = "squareup.protos.roots.I";
    testProtoWithRoots(sources, roots, outputs);
  }

  @Test public void testDryRun() throws Exception {
    String[] sources = {
        "service_root.proto"
    };

    String[] outputs = { };
    // When running with the --dry_run flag and --quiet, only the names of the output
    // files should be printed to the log.
    String[] flags = {
        "--dry_run",
        "--quiet",
        "--roots=squareup.wire.protos.roots.TheService"
    };
    testProto(sources, outputs, flags, "");
    assertThat(logger.getLog()).isEqualTo(""
            + outputDir.toAbsolutePath() + " com.squareup.wire.protos.roots.TheRequest\n"
            + outputDir.toAbsolutePath() + " com.squareup.wire.protos.roots.TheResponse\n");
  }

  private List<Path> getAllFiles() throws IOException {
    final List<Path> files = new ArrayList<>();
    final Path descriptor = outputDir.resolve("com/google/protobuf/");
    Files.walkFileTree(outputDir, new SimpleFileVisitor<Path>() {
      @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
          throws IOException {
        if (!file.startsWith(descriptor)) {
          files.add(file);
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return files;
  }

  private void assertFilesMatch(String path, String suffix) throws IOException {
    Path expectedDir = Paths.get("../wire-runtime/src/test/proto-java/");
    Path expected = expectedDir.resolve(path + suffix);
    if (!Files.isRegularFile(expected)) {
      expected = expectedDir.resolve(path);
    }

    Path actual = outputDir.resolve(path);

    assertFilesMatch(expected, actual);
  }

  private void assertFilesMatch(Path expected, Path actual) throws IOException {
    String expectedContent;
    try (Source source = Okio.source(expected)) {
      expectedContent = Okio.buffer(source).readUtf8();
    }

    String actualContent;
    try (Source source = Okio.source(actual)) {
      actualContent = Okio.buffer(source).readUtf8();
    }

    // Normalize CRLF -> LF
    expectedContent = expectedContent.replace("\r\n", "\n");
    actualContent = actualContent.replace("\r\n", "\n");
    assertThat(actualContent).isEqualTo(expectedContent);
  }
}
