// Copyright 2013 Square, Inc.
package com.squareup.wire.compiler;

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

  File testAllTypesDir;
  File testSimpleDir;

  @Before public void setUp() {
    System.out.println("cwd = " + new File(".").getAbsolutePath());

    testAllTypesDir = new File("WireCompilerTest_testAllTypes");
    testAllTypesDir.mkdir();
    cleanup(testAllTypesDir);
    List<String> filesBefore = getAllFiles(testAllTypesDir);
    Assert.assertEquals(0, filesBefore.size());

    testSimpleDir = new File("WireCompilerTest_testSimple");
    testSimpleDir.mkdir();
    cleanup(testSimpleDir);
    filesBefore = getAllFiles(testSimpleDir);
    Assert.assertEquals(0, filesBefore.size());
  }

  @After public void tearDown() {
    cleanup(testAllTypesDir);
    if (!testAllTypesDir.delete()) {
      System.err.println("Couldn't delete " + testAllTypesDir.getAbsolutePath());
    }

    cleanup(testSimpleDir);
    testSimpleDir.delete();
  }

  @Test public void testAllTypes() throws Exception {
    WireCompiler.main(
      "--proto_path=../wire-runtime/src/test/proto",
      "--java_out=" + testAllTypesDir.getAbsolutePath(),
      "all_types.proto"
    );


    List<String> filesAfter = getAllFiles(testAllTypesDir);
    Assert.assertEquals(2, filesAfter.size());

    filesMatch(testAllTypesDir, "com/squareup/wire/protos/alltypes/Ext_all_types.java");
    filesMatch(testAllTypesDir, "com/squareup/wire/protos/alltypes/AllTypes.java");
  }

  @Test public void testEdgeCases() throws Exception {
    WireCompiler.main(
        "--proto_path=../wire-runtime/src/test/proto",
        "--java_out=" + testAllTypesDir.getAbsolutePath(),
        "edge_cases.proto"
    );

    List<String> filesAfter = getAllFiles(testAllTypesDir);
    Assert.assertEquals(3, filesAfter.size());

    filesMatch(testAllTypesDir, "com/squareup/wire/protos/edgecases/NoFields.java");
    filesMatch(testAllTypesDir, "com/squareup/wire/protos/edgecases/OneField.java");
    filesMatch(testAllTypesDir, "com/squareup/wire/protos/edgecases/OneBytesField.java");
  }

  @Test public void testSimple() throws Exception {
    WireCompiler.main(
        "--proto_path=../wire-runtime/src/test/proto",
        "--java_out=" + testSimpleDir.getAbsolutePath(),
        "simple_message.proto",
        "external_message.proto"
    );

    List<String> filesAfter = getAllFiles(testSimpleDir);
    Assert.assertEquals(3, filesAfter.size());

    filesMatch(testSimpleDir,
        "com/squareup/wire/protos/simple/Ext_simple_message.java");
    filesMatch(testSimpleDir, "com/squareup/wire/protos/simple/SimpleMessage.java");
    filesMatch(testSimpleDir, "com/squareup/wire/protos/simple/ExternalMessage.java");
  }

  private void cleanup(File dir) {
    Assert.assertTrue(dir.isDirectory());
    if (dir.listFiles() != null) {
      for (File f : dir.listFiles()) {
        cleanupHelper(f);
      }
    }
  }

  private void cleanupHelper(File f) {
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

  private void filesMatch(File outputDir, String path) throws FileNotFoundException {
    File expectedFile = new File("../wire-runtime/src/test/java/" + path);
    String expected = new Scanner(expectedFile).useDelimiter("\\A").next();
    File actualFile = new File(outputDir, path);
    String actual = new Scanner(actualFile).useDelimiter("\\A").next();
    Assert.assertEquals(expected, actual);
  }
}
