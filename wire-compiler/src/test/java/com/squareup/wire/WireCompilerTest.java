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

public class WireCompilerTest {

  private File testPersonDir;
  private File testSimpleDir;
  private File testAllTypesDir;
  private File testEdgeCasesDir;
  private File testUnknownFieldsDir;

  @Before public void setUp() {
    System.out.println("cwd = " + new File(".").getAbsolutePath());
    testPersonDir = makeTestDirectory("WireCompilerTest_testPerson");
    testSimpleDir = makeTestDirectory("WireCompilerTest_testSimple");
    testAllTypesDir = makeTestDirectory("WireCompilerTest_testAllTypes");
    testEdgeCasesDir = makeTestDirectory("WireCompilerTest_testEdgeCases");
    testUnknownFieldsDir = makeTestDirectory("WireCompilerTest_testUnknownFields");
  }

  private File makeTestDirectory(String path) {
    File dir = new File(path);
    dir.mkdir();
    cleanup(dir);
    List<String> filesBefore = getAllFiles(dir);
    Assert.assertEquals(0, filesBefore.size());
    return dir;
  }

  @After public void tearDown() {
    cleanupAndDelete(testPersonDir);
    cleanupAndDelete(testSimpleDir);
    cleanupAndDelete(testAllTypesDir);
    cleanupAndDelete(testEdgeCasesDir);
    cleanupAndDelete(testUnknownFieldsDir);
  }

  private void cleanupAndDelete(File dir) {
    cleanup(dir);
    if (!dir.delete()) {
      System.err.println("Couldn't delete " + dir.getAbsolutePath());
    }
  }

  private void testProto(File dir, String[] sources, String[] outputs) throws Exception {
    String[] args = new String[2 + sources.length];
    args[0] = "--proto_path=../wire-runtime/src/test/proto";
    args[1] = "--java_out=" + dir.getAbsolutePath();
    System.arraycopy(sources, 0, args, 2, sources.length);

    WireCompiler.main(args);

    List<String> filesAfter = getAllFiles(dir);
    Assert.assertEquals(outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertFilesMatch(dir, output);
    }
  }

  @Test public void testPerson() throws Exception {
    String[] sources = {
        "person.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/person/Person.java"
    };
    testProto(testPersonDir, sources, outputs);
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
        "com/squareup/wire/protos/foreign/ForeignEnum.java"
    };
    testProto(testSimpleDir, sources, outputs);
  }

  @Test public void testAllTypes() throws Exception {
    String[] sources = {
        "all_types.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/alltypes/Ext_all_types.java",
        "com/squareup/wire/protos/alltypes/AllTypes.java"
    };
    testProto(testAllTypesDir, sources, outputs);
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
    testProto(testEdgeCasesDir, sources, outputs);
  }

  @Test public void testUnknownFields() throws Exception {
    String[] sources = {
        "unknown_fields.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/unknownfields/VersionOne.java",
        "com/squareup/wire/protos/unknownfields/VersionTwo.java"
    };
    testProto(testUnknownFieldsDir, sources, outputs);
  }

  private void cleanup(File dir) {
    Assert.assertNotNull(dir);
    Assert.assertTrue(dir.isDirectory());
    if (dir.listFiles() != null) {
      for (File f : dir.listFiles()) {
        cleanupHelper(f);
      }
    }
  }

  private void cleanupHelper(File f) {
    Assert.assertNotNull(f);
    if (f.isDirectory()) {
      if (f.listFiles() != null) {
        for (File ff : f.listFiles()) {
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
    Assert.assertEquals(expected, actual);
  }
}
