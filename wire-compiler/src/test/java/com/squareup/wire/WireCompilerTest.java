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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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

  private StringWireLogger logger;
  private File testDir;

  @Before public void setUp() {
    testDir = temp.getRoot();
  }

  @Test public void testFooBar() throws Exception {
    String[] sources = {
        "foo.proto",
        "bar.proto"
    };
    invokeCompiler(sources);

    String[] outputs = {
        "com/squareup/foobar/protos/bar/Bar.java",
        "com/squareup/foobar/protos/foo/Foo.java"
    };
    assertOutputs(outputs);
  }

  @Test public void testDifferentPackageFooBar() throws Exception {
    String[] sources = {
        "differentpackage/foo.proto",
        "differentpackage/bar.proto"
    };
    invokeCompiler(sources);

    String[] outputs = {
        "com/squareup/differentpackage/protos/bar/Bar.java",
        "com/squareup/differentpackage/protos/foo/Foo.java"
    };
    assertOutputs(outputs);
  }

  @Test public void testPerson() throws Exception {
    String[] sources = {
        "person.proto"
    };
    invokeCompiler(sources);

    String[] outputs = {
        "com/squareup/wire/protos/person/Person.java"
    };
    assertOutputs(outputs);
  }

  @Test public void testPersonAndroid() throws Exception {
    String[] sources = {
        "person.proto"
    };
    invokeCompiler(sources, "--android");

    String[] outputs = {
        "com/squareup/wire/protos/person/Person.java"
    };
    assertOutputs(outputs, ".android");
  }

  @Test public void testPersonCompact() throws Exception {
    String[] sources = {
        "all_types.proto"
    };
    invokeCompiler(sources, "--compact");

    String[] outputs = {
        "com/squareup/wire/protos/alltypes/AllTypes.java"
    };
    assertOutputs(outputs, ".compact");
  }

  @Test public void testSimple() throws Exception {
    String[] sources = {
        "simple_message.proto",
        "external_message.proto",
        "foreign.proto"
    };
    invokeCompiler(sources);

    String[] outputs = {
        "com/squareup/wire/protos/simple/SimpleMessage.java",
        "com/squareup/wire/protos/simple/ExternalMessage.java",
        "com/squareup/wire/protos/foreign/ForeignEnum.java",
        "com/squareup/wire/protos/foreign/ForeignMessage.java"
    };
    assertOutputs(outputs);
  }

  @Test public void testOneOf() throws Exception {
    String[] sources = {
        "one_of.proto"
    };
    invokeCompiler(sources);

    String[] outputs = {
        "com/squareup/wire/protos/oneof/OneOfMessage.java"
    };
    assertOutputs(outputs);
  }

  @Test public void testSingleLevel() throws Exception {
    String[] sources = {
        "single_level.proto"
    };
    invokeCompiler(sources);

    String[] outputs = {
        "com/squareup/wire/protos/single_level/Foo.java",
        "com/squareup/wire/protos/single_level/Foos.java",
    };
    assertOutputs(outputs);
  }

  @Test public void testSameBasename() throws Exception {
    String[] sources = {
        "single_level.proto",
        "samebasename/single_level.proto" };
    invokeCompiler(sources);

    String[] outputs = {
        "com/squareup/wire/protos/single_level/Foo.java",
        "com/squareup/wire/protos/single_level/Foos.java",
        "com/squareup/wire/protos/single_level/Bar.java",
        "com/squareup/wire/protos/single_level/Bars.java",
    };
    assertOutputs(outputs);
  }

  @Test public void testChildPackage() throws Exception {
    String[] sources = {
        "child_pkg.proto"
    };
    invokeCompiler(sources, "--named_files_only");

    String[] outputs = {
        "com/squareup/wire/protos/ChildPackage.java",
    };
    assertOutputs(outputs);
  }

  @Test public void testAllTypes() throws Exception {
    String[] sources = {
        "all_types.proto"
    };
    invokeCompiler(sources);

    String[] outputs = {
        "com/squareup/wire/protos/alltypes/AllTypes.java"
    };
    assertOutputs(outputs);
  }

  @Test public void testEdgeCases() throws Exception {
    String[] sources = {
        "edge_cases.proto"
    };
    invokeCompiler(sources);

    String[] outputs = {
        "com/squareup/wire/protos/edgecases/NoFields.java",
        "com/squareup/wire/protos/edgecases/OneField.java",
        "com/squareup/wire/protos/edgecases/OneBytesField.java",
        "com/squareup/wire/protos/edgecases/Recursive.java"
    };
    assertOutputs(outputs);
  }

  @Test public void testUnknownFields() throws Exception {
    String[] sources = {
        "unknown_fields.proto"
    };
    invokeCompiler(sources);

    String[] outputs = {
        "com/squareup/wire/protos/unknownfields/VersionOne.java",
        "com/squareup/wire/protos/unknownfields/VersionTwo.java"
    };
    assertOutputs(outputs);
  }

  @Test public void testCustomOptions() throws Exception {
    String[] sources = {
        "custom_options.proto",
        "option_redacted.proto"
    };
    invokeCompiler(sources, "--named_files_only");

    String[] outputs = {
        "com/squareup/wire/protos/custom_options/FooBar.java",
        "com/squareup/wire/protos/custom_options/MessageWithOptions.java",
    };
    assertOutputs(outputs);
  }

  @Test public void testCustomOptionsNoOptions() throws Exception {
    String[] sources = {
        "custom_options.proto",
        "option_redacted.proto"
    };
    invokeCompiler(sources, "--excludes=google.protobuf.*", "--named_files_only");

    String[] outputs = {
        "com/squareup/wire/protos/custom_options/FooBar.java",
        "com/squareup/wire/protos/custom_options/MessageWithOptions.java"
    };
    assertOutputs(outputs, ".noOptions");
  }

  @Test public void testRedacted() throws Exception {
    String[] sources = {
        "redacted_test.proto",
        "option_redacted.proto"
    };
    invokeCompiler(sources);

    String[] outputs = {
        "com/squareup/wire/protos/redacted/NotRedacted.java",
        "com/squareup/wire/protos/redacted/Redacted.java",
        "com/squareup/wire/protos/redacted/RedactedChild.java",
        "com/squareup/wire/protos/redacted/RedactedCycleA.java",
        "com/squareup/wire/protos/redacted/RedactedCycleB.java",
        "com/squareup/wire/protos/redacted/RedactedExtension.java",
        "com/squareup/wire/protos/redacted/RedactedRepeated.java",
        "com/squareup/wire/protos/redacted/RedactedRequired.java",
    };
    assertOutputs(outputs);
  }

  @Test public void testNoRoots() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    invokeCompiler(sources);

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
    };
    assertOutputs(outputs);
  }

  @Test public void testExcludes() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    invokeCompiler(sources,
        "--includes=squareup.protos.roots.A",
        "--excludes=squareup.protos.roots.B");

    String[] outputs = {
        "com/squareup/wire/protos/roots/A.java",
        "com/squareup/wire/protos/roots/D.java"
    };
    assertOutputs(outputs, ".pruned");
  }

  @Test public void testRootsA() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    invokeCompiler(sources, "--includes=squareup.protos.roots.A");

    String[] outputs = {
        "com/squareup/wire/protos/roots/A.java",
        "com/squareup/wire/protos/roots/B.java",
        "com/squareup/wire/protos/roots/C.java",
        "com/squareup/wire/protos/roots/D.java"
    };
    assertOutputs(outputs);
  }

  @Test public void testRootsB() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    invokeCompiler(sources, "--includes=squareup.protos.roots.B");

    String[] outputs = {
        "com/squareup/wire/protos/roots/B.java",
        "com/squareup/wire/protos/roots/C.java"
    };
    assertOutputs(outputs);
  }

  @Test public void testRootsE() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    invokeCompiler(sources, "--includes=squareup.protos.roots.E");

    String[] outputs = {
        "com/squareup/wire/protos/roots/E.java",
        "com/squareup/wire/protos/roots/G.java"
    };
    assertOutputs(outputs);
  }

  @Test public void testRootsH() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    invokeCompiler(sources, "--includes=squareup.protos.roots.H");

    String[] outputs = {
        "com/squareup/wire/protos/roots/E.java",
        "com/squareup/wire/protos/roots/H.java"
    };
    assertOutputs(outputs, ".pruned");
  }

  @Test public void testRootsI() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    invokeCompiler(sources, "--includes=squareup.protos.roots.I");

    String[] outputs = {
        "com/squareup/wire/protos/roots/I.java",
        "com/squareup/wire/protos/roots/J.java",
        "com/squareup/wire/protos/roots/K.java"
    };
    assertOutputs(outputs);
  }

  @Test public void testDryRun() throws Exception {
    String[] sources = {
        "service_root.proto"
    };
    invokeCompiler(sources, "--includes=squareup.wire.protos.roots.TheService", "--dry_run", "--quiet");

    assertThat(logger.getLog()).isEqualTo(""
        + testDir.getAbsolutePath() + " com.squareup.wire.protos.roots.TheRequest\n"
        + testDir.getAbsolutePath() + " com.squareup.wire.protos.roots.TheResponse\n");
  }

  @Test public void noFiles() throws Exception {
    String[] sources = new String[0];
    invokeCompiler(sources);

    assertThat(getPaths()).isNotEmpty();
  }

  private void invokeCompiler(String[] sources, String... extraArgs) throws Exception {
    List<String> args = new ArrayList<>();
    args.add("--proto_path=../wire-runtime/src/test/proto");
    args.add("--java_out=" + testDir.getAbsolutePath());
    Collections.addAll(args, extraArgs);
    Collections.addAll(args, sources);

    logger = new StringWireLogger();
    FileSystem fs = FileSystems.getDefault();
    WireCompiler compiler = WireCompiler.forArgs(fs, logger, args.toArray(new String[args.size()]));
    compiler.compile();
  }

  private void assertOutputs(String[] outputs) throws IOException {
    assertOutputs(outputs, "");
  }

  private void assertOutputs(String[] outputs, String suffix) throws IOException {
    List<String> filesAfter = getPaths();
    assertThat(filesAfter.size())
        .overridingErrorMessage(filesAfter.toString())
        .isEqualTo(outputs.length);

    for (String output : outputs) {
      assertFilesMatch(testDir, output, suffix);
    }
  }

  /** Returns all paths within {@code root}, and relative to {@code root}. */
  private List<String> getPaths() {
    List<String> paths = new ArrayList<>();
    getPathsRecursive(testDir.getAbsoluteFile(), "", paths);
    return paths;
  }

  private void getPathsRecursive(File base, String path, List<String> paths) {
    File file = new File(base, path);

    String[] children = file.list();
    if (children == null) return;

    for (String child : children) {
      File childFile = new File(file, child);
      if (childFile.isFile()) {
        paths.add(path + child);
      } else {
        getPathsRecursive(base, path + child + "/", paths);
      }
    }
  }

  private void assertFilesMatch(File outputDir, String path, String suffix) throws IOException {
    // Compare against file with suffix if present
    File expectedFile = new File("../wire-runtime/src/test/proto-java/" + path + suffix);
    if (expectedFile.exists()) {
      System.out.println("Comparing against expected output " + expectedFile.getName());
    } else {
      expectedFile = new File("../wire-runtime/src/test/proto-java/" + path);
    }
    File actualFile = new File(outputDir, path);
    assertFilesMatch(expectedFile, actualFile);
  }

  private void assertFilesMatch(File expectedFile, File actualFile) throws IOException {
    String expected;
    try (Source source = Okio.source(expectedFile)) {
      expected = Okio.buffer(source).readUtf8();
    }

    String actual;
    try (Source source = Okio.source(actualFile)) {
      actual = Okio.buffer(source).readUtf8();
    }

    // Normalize CRLF -> LF
    expected = expected.replace("\r\n", "\n");
    actual = actual.replace("\r\n", "\n");
    assertThat(actual).isEqualTo(expected);
  }
}
