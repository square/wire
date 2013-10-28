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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WireCompilerTest {

  private File testDir;

  @Before public void setUp() {
    System.out.println("cwd = " + new File(".").getAbsolutePath());
    testDir = makeTestDirectory("WireCompilerTest");
  }

  private File makeTestDirectory(String path) {
    File dir = new File(path);
    dir.mkdir();
    cleanup(dir);
    List<String> filesBefore = getAllFiles(dir);
    assertEquals(0, filesBefore.size());
    return dir;
  }

  @After public void tearDown() {
    cleanupAndDelete(testDir);
  }

  private void cleanupAndDelete(File dir) {
    cleanup(dir);
    if (!dir.delete()) {
      System.err.println("Couldn't delete " + dir.getAbsolutePath());
    }
  }

  private void testProto(String[] sources, String[] outputs) throws Exception {
    String[] args = new String[2 + sources.length];
    args[0] = "--proto_path=../wire-runtime/src/test/proto";
    args[1] = "--java_out=" + testDir.getAbsolutePath();
    System.arraycopy(sources, 0, args, 2, sources.length);

    WireCompiler.main(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals(outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertFilesMatch(testDir, output);
    }
  }

  private void testProtoWithRegistry(String[] sources, String registryClass, String[] outputs)
      throws Exception {
    String[] args = new String[3 + sources.length];
    args[0] = "--proto_path=../wire-runtime/src/test/proto";
    args[1] = "--java_out=" + testDir.getAbsolutePath();
    args[2] = "--registry_class=" + registryClass;
    System.arraycopy(sources, 0, args, 3, sources.length);

    WireCompiler.main(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals(outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertFilesMatch(testDir, output);
    }
  }

  private void testProtoWithRoots(String[] sources, String roots, String[] outputs)
      throws Exception {
    String[] args = new String[3 + sources.length];
    args[0] = "--proto_path=../wire-runtime/src/test/proto";
    args[1] = "--java_out=" + testDir.getAbsolutePath();
    args[2] = "--roots=" + roots;
    System.arraycopy(sources, 0, args, 3, sources.length);

    WireCompiler.main(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals(outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertFilesMatch(testDir, output);
    }
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

  @Test public void testSimple() throws Exception {
    String[] sources = {
        "simple_message.proto",
        "external_message.proto",
        "foreign.proto"
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

  @Test public void testRegistry() throws Exception {
    String[] sources = {
        "simple_message.proto",
        "external_message.proto",
        "foreign.proto"
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
        "child_pkg.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/ChildPackage.java",
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
        "com/squareup/wire/protos/edgecases/OneBytesField.java"
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
        "custom_options.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/custom_options/FooBar.java",
        "com/squareup/wire/protos/custom_options/Ext_custom_options.java",
        "com/squareup/wire/protos/custom_options/MessageWithOptions.java"
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

  @Test public void sanitizeJavadocStripsTrailingWhitespace() {
    String input = "The quick brown fox  \nJumps over  \n\t \t\nThe lazy dog  ";
    String expected = "The quick brown fox\nJumps over\n\nThe lazy dog";
    assertEquals(expected, MessageWriter.sanitizeJavadoc(input));
  }

  @Test public void sanitizeJavadocGuardsFormatCharacters() {
    String input = "This is 12% of %s%d%f%c!";
    String expected = "This is 12%% of %%s%%d%%f%%c!";
    assertEquals(expected, MessageWriter.sanitizeJavadoc(input));
  }

  @Test public void sanitizeJavadocWrapsSeeLinks() {
    String input = "Google query.\n\n@see http://google.com";
    String expected = "Google query.\n\n@see <a href=\"http://google.com\">http://google.com</a>";
    assertEquals(expected, MessageWriter.sanitizeJavadoc(input));
  }

  private void cleanup(File dir) {
    Assert.assertNotNull(dir);
    Assert.assertTrue(dir.isDirectory());
    File[] files = dir.listFiles();
    if (files != null) {
      for (File f : files) {
        cleanupHelper(f);
      }
    }
  }

  private void cleanupHelper(File f) {
    Assert.assertNotNull(f);
    if (f.isDirectory()) {
      File[] files = f.listFiles();
      if (files != null) {
        for (File ff : files) {
          cleanupHelper(ff);
        }
      }
      f.delete();
    } else {
      f.delete();
    }
  }

  private List<String> getAllFiles(File root) {
    List<String> files = new ArrayList<String>();
    getAllFilesHelper(root, files);
    return files;
  }

  private void getAllFilesHelper(File root, List<String> files) {
    if (root.isFile()) {
      files.add(root.getAbsolutePath());
    }
    File[] allFiles = root.listFiles();
    if (allFiles != null) {
      for (File f : allFiles) {
        getAllFilesHelper(f, files);
      }
    }
  }

  private void assertFilesMatch(File outputDir, String path) throws FileNotFoundException {
    File expectedFile = new File("../wire-runtime/src/test/java/" + path);
    String expected = new Scanner(expectedFile).useDelimiter("\\A").next();
    File actualFile = new File(outputDir, path);
    String actual = new Scanner(actualFile).useDelimiter("\\A").next();
    // Normalize CRLF -> LF
    expected = expected.replace("\r\n", "\n");
    actual = actual.replace("\r\n", "\n");
    assertEquals(expected, actual);
  }
}
