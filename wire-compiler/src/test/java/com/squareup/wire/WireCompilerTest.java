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

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.Service;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class WireCompilerTest {
  private StringWireLogger logger;
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
    testProto(sources, outputs, null);
  }

  private void testProto(String[] sources, String[] outputs, String serviceWriter,
      String... serviceWriterOption) throws Exception {
    int numFlags = 3;
    if (serviceWriter != null) ++numFlags;
    if (serviceWriterOption != null) numFlags += serviceWriterOption.length;
    String[] args = new String[numFlags + sources.length];
    args[0] = "--proto_path=../wire-runtime/src/test/proto";
    args[1] = "--java_out=" + testDir.getAbsolutePath();
    args[2] = "--enum_options=squareup.protos.custom_options.enum_value_option,"
        + "squareup.protos.custom_options.complex_enum_value_option,"
        + "squareup.protos.foreign.foreign_enum_value_option";
    if (serviceWriter != null) {
      args[3] = "--service_writer=" + serviceWriter;
      if (serviceWriterOption != null) {
        for (int i = 0; i < serviceWriterOption.length; i++) {
          args[4 + i] = "--service_writer_opt=" + serviceWriterOption[i];
        }
      }
    }
    System.arraycopy(sources, 0, args, numFlags, sources.length);

    invokeCompiler(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals(filesAfter.toString(), outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertFilesMatch(testDir, output);
    }
  }

  private void testProtoNoOptions(String[] sources, String[] outputs) throws Exception {
    int numFlags = 4;
    String[] args = new String[numFlags + sources.length];
    args[0] = "--proto_path=../wire-runtime/src/test/proto";
    args[1] = "--no_options";
    // Emit one of the enum options anyway.
    args[2] = "--enum_options=squareup.protos.custom_options.enum_value_option";
    args[3] = "--java_out=" + testDir.getAbsolutePath();
    System.arraycopy(sources, 0, args, numFlags, sources.length);

    invokeCompiler(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals(outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertFilesMatchNoOptions(testDir, output);
    }
  }

  private void testProtoWithRegistry(String[] sources, String registryClass, String[] outputs)
      throws Exception {
    int numFlags = 3;
    String[] args = new String[numFlags + sources.length];
    args[0] = "--proto_path=../wire-runtime/src/test/proto";
    args[1] = "--java_out=" + testDir.getAbsolutePath();
    args[2] = "--registry_class=" + registryClass;
    System.arraycopy(sources, 0, args, numFlags, sources.length);

    invokeCompiler(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals(outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertFilesMatch(testDir, output);
    }
  }

  private void testProtoWithRoots(String[] sources, String roots, String[] outputs)
      throws Exception {
    String[] extraArgs = {};
    this.testProtoWithRoots(sources, roots, outputs, extraArgs);
  }

  private void testProtoWithRoots(String[] sources, String roots, String[] outputs,
      String[] extraArgs)
      throws Exception {
    int numFlags = 4;
    String[] args = new String[numFlags + sources.length + extraArgs.length];
    int index = 0;
    args[index++] = "--proto_path=../wire-runtime/src/test/proto";
    args[index++] = "--java_out=" + testDir.getAbsolutePath();
    args[index++] = "--service_writer=com.squareup.wire.SimpleServiceWriter";
    args[index++] = "--roots=" + roots;
    for (int i = 0; i < extraArgs.length; i++) {
      args[index++] = extraArgs[i];
    }
    System.arraycopy(sources, 0, args, index, sources.length);

    invokeCompiler(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals("Wrong number of files written", outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertFilesMatch(testDir, output);
    }
  }

  private void testLimitedServiceGeneration(String[] sources, String roots, String[] outputs,
      String serviceSuffix) throws Exception {
    int numFlags = 5;
    String[] args = new String[numFlags + sources.length];
    args[0] = "--proto_path=../wire-runtime/src/test/proto";
    args[1] = "--java_out=" + testDir.getAbsolutePath();
    args[2] = "--service_writer=com.squareup.wire.TestRxJavaServiceWriter";
    args[3] = "--service_writer_opt=" + serviceSuffix;
    args[4] = "--roots=" + roots;
    System.arraycopy(sources, 0, args, numFlags, sources.length);

    invokeCompiler(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals(filesAfter.toString(), outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertJavaFilesMatchWithSuffix(testDir, output, serviceSuffix);
    }
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

  @Test public void testSimpleService() throws Exception {
    String[] sources = {
        "request_response.proto",
        "service.proto"
    };
    String[] outputs = {
        "com/squareup/services/anotherpackage/SendDataRequest.java",
        "com/squareup/services/anotherpackage/SendDataResponse.java",
        "com/squareup/services/ExampleService.java"
    };
    testProto(sources, outputs, "com.squareup.wire.SimpleServiceWriter");
  }

  @Test public void testRetrofitService() throws Exception {
    String[] sources = {
        "request_response.proto",
        "retrofit_service.proto"
    };
    String[] outputs = {
        "com/squareup/services/anotherpackage/SendDataRequest.java",
        "com/squareup/services/anotherpackage/SendDataResponse.java",
        "com/squareup/services/RetrofitService.java"
    };
    testProto(sources, outputs, "com.squareup.wire.RetrofitServiceWriter");
  }

  @Test public void testRxJavaService() throws Exception {
    String[] sources = {
        "request_response.proto",
        "rxjava_service.proto"
    };
    String[] outputs = {
        "com/squareup/services/anotherpackage/SendDataRequest.java",
        "com/squareup/services/anotherpackage/SendDataResponse.java",
        "com/squareup/services/RxJavaService.java"
    };
    testProto(sources, outputs, "com.squareup.wire.RxJavaServiceWriter");
  }

  @Test
  public void testUnlimitedRxJavaService() throws Exception {
    String[] sources = {
        "request_response.proto",
        "rxjava_service.proto",
        "rxjava_service2.proto"
    };
    String[] outputs = {
        "com/squareup/services/anotherpackage/SendDataRequest.java",
        "com/squareup/services/anotherpackage/SendDataResponse.java",
        "com/squareup/services/HeresAllTheDataRequest.java",
        "com/squareup/services/HeresAllTheDataResponse.java",
        "com/squareup/services/LetsDataRequest.java",
        "com/squareup/services/LetsDataResponse.java",
        "com/squareup/services/RxJavaService.java",
        "com/squareup/services/RxJavaService2.java"
    };
    String roots = "com.squareup.services.RxJavaService,com.squareup.services.RxJavaService2";
    testLimitedServiceGeneration(sources, roots, outputs, "");
  }

  @Test
  public void testLimitedRxJavaService() throws Exception {
    String[] sources = {
        "request_response.proto",
        "rxjava_service.proto",
        "rxjava_service2.proto"
    };
    String[] outputs = {
        "com/squareup/services/anotherpackage/SendDataRequest.java",
        "com/squareup/services/anotherpackage/SendDataResponse.java",
        "com/squareup/services/LetsDataRequest.java",
        "com/squareup/services/LetsDataResponse.java",
        "com/squareup/services/RxJavaService.java",
        "com/squareup/services/RxJavaService2.java"
    };
    String roots = "com.squareup.services.RxJavaService#SendSomeData,"
        + "com.squareup.services.RxJavaService2#LetsData,"
        + "com.squareup.services.RxJavaService2#SendSomeMoreData";
    testLimitedServiceGeneration(sources, roots, outputs, "SomeEndpoints");
  }

  // Verify that the --service_writer_opt flag works correctly.
  @SuppressWarnings("UnusedDeclaration")
  public static class TestServiceWriter extends SimpleServiceWriter {
    public TestServiceWriter(JavaWriter writer, List<String> options) {
      super(writer, options);
      if (!Arrays.asList("OPTION1", "OPTION2").equals(options)) {
        fail();
      }
    }

    @Override public void emitService(Service service, Set<String> importedTypes)
        throws IOException {
      super.emitService(service, importedTypes);
    }
  }

  @Test public void testSimpleServiceOption() throws Exception {
    String[] sources = {
        "request_response.proto",
        "service.proto"
    };
    String[] outputs = {
        "com/squareup/services/anotherpackage/SendDataRequest.java",
        "com/squareup/services/anotherpackage/SendDataResponse.java",
        "com/squareup/services/ExampleService.java"
    };
    testProto(sources, outputs, "com.squareup.wire.WireCompilerTest$TestServiceWriter",
        "OPTION1", "OPTION2");
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
        "custom_options.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/custom_options/FooBar.java",
        "com/squareup/wire/protos/custom_options/Ext_custom_options.java",
        "com/squareup/wire/protos/custom_options/MessageWithOptions.java"
    };
    testProto(sources, outputs);
  }

  @Test public void testCustomOptionsNoOptions() throws Exception {
    String[] sources = {
        "custom_options.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/custom_options/FooBar.java",
        "com/squareup/wire/protos/custom_options/Ext_custom_options.java",
        "com/squareup/wire/protos/custom_options/MessageWithOptions.java"
    };

    testProtoNoOptions(sources, outputs);
  }

  @Test public void testRedacted() throws Exception {
    String[] sources = {
        "redacted_test.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/redacted/Ext_redacted_test.java",
        "com/squareup/wire/protos/redacted/NotRedacted.java",
        "com/squareup/wire/protos/redacted/Redacted.java",
        "com/squareup/wire/protos/redacted/RedactedChild.java",
        "com/squareup/wire/protos/redacted/RedactedCycleA.java",
        "com/squareup/wire/protos/redacted/RedactedCycleB.java",
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

  @Test public void testServiceRoots1() throws Exception {
    String[] sources = {
        "service_root.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/roots/TheRequest.java",
        "com/squareup/wire/protos/roots/TheResponse.java",
        "com/squareup/wire/protos/roots/UnnecessaryResponse.java",
        "com/squareup/wire/protos/roots/TheService.java",
        "com/squareup/wire/protos/roots/UnnecessaryService.java"
    };
    String roots =
        "squareup.wire.protos.roots.TheService,squareup.wire.protos.roots.UnnecessaryService";
    testProtoWithRoots(sources, roots, outputs);
  }

  @Test public void testServiceRoots2() throws Exception {
    String[] sources = {
        "service_root.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/roots/TheRequest.java",
        "com/squareup/wire/protos/roots/TheResponse.java",
        "com/squareup/wire/protos/roots/TheService.java"
    };
    String roots = "squareup.wire.protos.roots.TheService";
    testProtoWithRoots(sources, roots, outputs);
  }

  @Test public void testDryRun() throws Exception {
    String[] sources = {
        "service_root.proto"
    };

    String[] outputs = { };
    String roots = "squareup.wire.protos.roots.TheService";
    // When running with the --dry_run flag and --quiet, only the names of the output
    // files should be printed to the log.
    String[] extraArgs = {
        "--dry_run",
        "--quiet"
    };
    testProtoWithRoots(sources, roots, outputs, extraArgs);
    assertEquals(testDir.getAbsolutePath().toString() + "/com/squareup/wire/protos/roots/TheRequest.java\n"
            + testDir.getAbsolutePath().toString() + "/com/squareup/wire/protos/roots/TheResponse.java\n"
            + testDir.getAbsolutePath().toString() + "/com/squareup/wire/protos/roots/TheService.java\n",
        logger.getLog());
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

  private void invokeCompiler(String[] args) throws WireException {
    CommandLineOptions options = new CommandLineOptions(args);
    logger = new StringWireLogger(options.quiet);
    new WireCompiler(options, new IO.FileIO(), logger).compile();
  }

  private void assertFilesMatch(File outputDir, String path) throws FileNotFoundException {
    File expectedFile = new File("../wire-runtime/src/test/java/" + path);
    File actualFile = new File(outputDir, path);
    assertFilesMatch(expectedFile, actualFile);
  }

  private void assertFilesMatchNoOptions(File outputDir, String path)
      throws FileNotFoundException {
    // Compare against file with .noOptions suffix if present
    File expectedFile = new File("../wire-runtime/src/test/java/" + path + ".noOptions");
    if (expectedFile.exists()) {
      System.out.println("Comparing against expected output " + expectedFile.getName());
    } else {
      expectedFile = new File("../wire-runtime/src/test/java/" + path);
    }
    File actualFile = new File(outputDir, path);
    assertFilesMatch(expectedFile, actualFile);
  }

  private void assertJavaFilesMatchWithSuffix(File outputDir, String path, String suffix)
      throws FileNotFoundException {
    path = path.substring(0, path.indexOf(".java"));
    // Compare against file with .noOptions suffix if present
    File expectedFile = new File("../wire-runtime/src/test/java/" + path + suffix + ".java");
    if (expectedFile.exists()) {
      System.out.println("Comparing against expected output " + expectedFile.getName());
    } else {
      expectedFile = new File("../wire-runtime/src/test/java/" + path + ".java");
    }
    File actualFile = new File(outputDir, path + ".java");
    assertFilesMatch(expectedFile, actualFile);
  }

  private void assertFilesMatch(File expectedFile, File actualFile) throws FileNotFoundException {
    String expected = new Scanner(expectedFile).useDelimiter("\\A").next();
    String actual = new Scanner(actualFile).useDelimiter("\\A").next();

    // Normalize CRLF -> LF
    expected = expected.replace("\r\n", "\n");
    actual = actual.replace("\r\n", "\n");
    assertEquals(expected, actual);
  }
}
