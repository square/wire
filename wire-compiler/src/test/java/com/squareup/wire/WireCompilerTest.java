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
import com.squareup.wire.compiler.Main;
import com.squareup.wire.compiler.plugin.java.services.SimpleServiceWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import org.fest.util.Strings;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Ignore
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

  private void testProto(String[] sources, String[] roots, String[] outputs) throws Exception {
    testProto(sources, roots, outputs, null, (String[]) null);
  }

  private void testProto(String[] sources, String[] roots, String[] outputs, String serviceWriter,
      String... serviceWriterOption) throws Exception {
    int numFlags = 5;
    if (serviceWriter != null) {
      numFlags++;
      if (!isEmpty(serviceWriterOption)) numFlags++;
    }
    numFlags += roots.length;
    numFlags += sources.length;
    String[] args = new String[numFlags];
    args[0] = "-Djava_out=" + testDir.getAbsolutePath();
    args[1] = "--path=../wire-runtime/src/test/proto";
    args[2] = "--enum_option=squareup.protos.custom_options.enum_value_option";
    args[3] = "--enum_option=squareup.protos.custom_options.complex_enum_value_option";
    args[4] = "--enum_option=squareup.protos.foreign.foreign_enum_value_option";
    int index = 5;
    if (serviceWriter != null) {
      args[index++] = "-Dservice_writer=" + serviceWriter;
      if (!isEmpty(serviceWriterOption)) {
        args[index++] = "-Dservice_writer_opt=" + join(serviceWriterOption, ",");
      }
    }
    for (String root : roots) {
      args[index++] = "--root=" + root;
    }
    for (String source : sources) {
      args[index++] = "../wire-runtime/src/test/proto/" + source;
    }

    Main.main(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals(filesAfter.toString(), outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertFilesMatch(testDir, output);
    }
  }

  private void testProtoNoOptions(String[] sources, String[] roots, String[] outputs)
      throws Exception {
    int numFlags = 4 + roots.length + sources.length;
    String[] args = new String[numFlags];
    args[0] = "-Djava_out=" + testDir.getAbsolutePath();
    args[1] = "--path=../wire-runtime/src/test/proto";
    args[2] = "--no_options";
    // Emit one of the enum options anyway.
    args[3] = "--enum_option=squareup.protos.custom_options.enum_value_option";
    int index = 4;
    for (String root : roots) {
      args[index++] = "--root=" + root;
    }
    for (String source : sources) {
      args[index++] = "../wire-runtime/src/test/proto/" + source;
    }

    Main.main(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals(outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertFilesMatchNoOptions(testDir, output);
    }
  }

  private void testProtoWithRegistry(String[] sources, String[] roots, String registryClass,
      String[] outputs) throws Exception {
    int numFlags = 3 + roots.length + sources.length;
    String[] args = new String[numFlags];
    args[0] = "-Djava_out=" + testDir.getAbsolutePath();
    args[1] = "-Dregistry_class=" + registryClass;
    args[2] = "--path=../wire-runtime/src/test/proto";
    int index = 3;
    for (String root : roots) {
      args[index++] = "--root=" + root;
    }
    for (String source : sources) {
      args[index++] = "../wire-runtime/src/test/proto/" + source;
    }

    Main.main(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals(outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertFilesMatch(testDir, output);
    }
  }

  private void testProtoWithRoots(String[] sources, String[] roots, String[] outputs)
      throws Exception {
    int numFlags = 3 + roots.length + sources.length;
    String[] args = new String[numFlags];
    args[0] = "-Djava_out=" + testDir.getAbsolutePath();
    args[1] = "-Dservice_writer=" + SimpleServiceWriter.class.getName();
    args[2] = "--path=../wire-runtime/src/test/proto";
    int index = 3;
    for (String root : roots) {
      args[index++] = "--root=" + root;
    }
    for (String source : sources) {
      args[index++] = "../wire-runtime/src/test/proto/" + source;
    }

    Main.main(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals(outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertFilesMatch(testDir, output);
    }
  }

  private void testLimitedServiceGeneration(String[] sources, String[] roots, String[] outputs,
      String serviceSuffix) throws Exception {
    int numFlags = 4 + roots.length + sources.length;
    String[] args = new String[numFlags];
    args[0] = "-Djava_out=" + testDir.getAbsolutePath();
    args[1] = "-Dservice_writer=com.squareup.wire.TestRxJavaServiceWriter";
    args[2] = "-Dservice_writer_opt=" + serviceSuffix;
    args[3] = "--path=../wire-runtime/src/test/proto";
    int index = 4;
    for (String root : roots) {
      args[index++] = "--root=" + root;
    }
    for (String source : sources) {
      args[index++] = "../wire-runtime/src/test/proto/" + source;
    }

    Main.main(args);

    List<String> filesAfter = getAllFiles(testDir);
    assertEquals(filesAfter.toString(), outputs.length, filesAfter.size());

    for (String output : outputs) {
      assertJavaFilesMatchWithSuffix(testDir, output, serviceSuffix);
    }
  }

  private String join(String[] strings, String separator) {
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for (String s : strings) {
      sb.append(sep);
      sb.append(s);
      sep = separator;
    }
    return sb.toString();
  }

  private boolean isEmpty(String[] serviceWriterOption) {
    return serviceWriterOption == null || serviceWriterOption.length == 0;
  }

  @Test public void testPerson() throws Exception {
    String[] sources = {
        "person.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/person/Person.java"
    };
    String[] roots = { "squareup.protos.person.Person" };
    testProto(sources, roots, outputs);
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
        "com/squareup/wire/protos/foreign/ForeignMessage.java",
        "com/google/protobuf/EnumValueOptions.java",
        "com/google/protobuf/MessageOptions.java",
        "com/google/protobuf/UninterpretedOption.java"
    };
    String[] roots = {
        "squareup.protos.simple.SimpleMessage",
        "squareup.protos.simple.ExternalMessage",
        "squareup.protos.foreign.ForeignMessage",
        "google.protobuf.MessageOptions",
        "google.protobuf.EnumValueOptions"
    };

    testProto(sources, roots, outputs);
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
    String[] roots = { "com.squareup.services.ExampleService" };

    testProto(sources, roots, outputs,
        "com.squareup.wire.compiler.plugin.java.services.SimpleServiceWriter");
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
    String[] roots = { "com.squareup.services.RetrofitService" };

    testProto(sources, roots, outputs,
        "com.squareup.wire.compiler.plugin.java.services.RetrofitServiceWriter");
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
    String[] roots = { "com.squareup.services.RxJavaService" };

    testProto(sources, roots, outputs,
        "com.squareup.wire.compiler.plugin.java.services.RxJavaServiceWriter");
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
    String[] roots = {
        "com.squareup.services.RxJavaService",
        "com.squareup.services.RxJavaService2"
    };
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
    String[] roots = {
        "com.squareup.services.RxJavaService#SendSomeData",
        "com.squareup.services.RxJavaService2#LetsData",
        "com.squareup.services.RxJavaService2#SendSomeMoreData"
    };
    testLimitedServiceGeneration(sources, roots, outputs, "SomeEndpoints");
  }

  // Verify that the -Dservice_writer_opt flag works correctly.
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
    String[] roots = { "com.squareup.services.ExampleService" };

    testProto(sources, roots, outputs, "com.squareup.wire.WireCompilerTest$TestServiceWriter",
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
    String[] roots = {
        "squareup.protos.simple.SimpleMessage",
        "squareup.protos.foreign.ForeignMessage",
        "google.protobuf.MessageOptions"
    };

    testProtoWithRegistry(sources, roots, registry, outputs);
  }

  @Test public void testSingleLevel() throws Exception {
    String[] sources = {
        "single_level.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/single_level/Foo.java",
        "com/squareup/wire/protos/single_level/Foos.java",
    };
    String[] roots = new String[0];
    testProto(sources, roots, outputs);
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
    String[] roots = new String[0];
    testProto(sources, roots, outputs);
  }

  @Test public void testChildPackage() throws Exception {
    String[] sources = {
        "child_pkg.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/ChildPackage.java",
        "com/squareup/wire/protos/foreign/Ext_Foreign.java",
        "com/squareup/wire/protos/foreign/ForeignEnum.java",
        "com/squareup/wire/protos/foreign/ForeignMessage.java",
        "com/google/protobuf/EnumValueOptions.java",
        "com/google/protobuf/MessageOptions.java",
        "com/google/protobuf/UninterpretedOption.java"
    };
    String[] roots = {
        "squareup.protos.ChildPackage",
        "squareup.protos.foreign.ForeignMessage",
        "google.protobuf.EnumValueOptions",
        "google.protobuf.MessageOptions",
        "google.protobuf.UninterpretedOption"
    };
    testProto(sources, roots, outputs);
  }

  @Test public void testAllTypes() throws Exception {
    String[] sources = {
        "all_types.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/alltypes/Ext_all_types.java",
        "com/squareup/wire/protos/alltypes/AllTypes.java"
    };
    String[] roots = { "squareup.protos.alltypes.AllTypes" };
    testProto(sources, roots, outputs);
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
    String[] roots = new String[0];
    testProto(sources, roots, outputs);
  }

  @Test public void testUnknownFields() throws Exception {
    String[] sources = {
        "unknown_fields.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/unknownfields/VersionOne.java",
        "com/squareup/wire/protos/unknownfields/VersionTwo.java"
    };
    String[] roots = new String[0];
    testProto(sources, roots, outputs);
  }

  @Test public void testCustomOptions() throws Exception {
    String[] sources = {
        "custom_options.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/custom_options/Ext_custom_options.java",
        "com/squareup/wire/protos/custom_options/FooBar.java",
        "com/squareup/wire/protos/custom_options/MessageWithOptions.java",
        "com/squareup/wire/protos/foreign/Ext_foreign.java",
        "com/squareup/wire/protos/foreign/ForeignMessage.java",
        "com/google/protobuf/EnumOptions.java",
        "com/google/protobuf/EnumValueOptions.java",
        "com/google/protobuf/FieldOptions.java",
        "com/google/protobuf/MessageOptions.java",
        "com/google/protobuf/UniterpretedOption.java",
    };
    String[] roots = {
        "squareup.protos.custom_options.FooBar",
        "squareup.protos.custom_options.FooBar.More",
        "squareup.protos.custom_options.MessageWithOptions",
        "google.protobuf.EnumOptions",
        "google.protobuf.EnumValueOptions",
        "google.protobuf.FieldOptions",
        "google.protobuf.MessageOptions",
        "extend@google.protobuf.EnumOptions",
        "extend@google.protobuf.EnumValueOptions",
        "extend@google.protobuf.FieldOptions",
        "extend@google.protobuf.MessageOptions"
    };
    testProto(sources, roots, outputs);
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
    String[] roots = {
        "squareup.protos.custom_options.FooBar",
        "squareup.protos.custom_options.MessageWithOptions",
        "google.protobuf.EnumOptions",
        "google.protobuf.EnumValueOptions",
        "google.protobuf.FieldOptions",
        "google.protobuf.MessageOptions"
    };

    testProtoNoOptions(sources, roots, outputs);
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
        "com/google/protobuf/FieldOptions.java",
        "com/google/protobuf/UninterpretedOption.java"
    };
    String[] roots = {
        "squareup.protos.redacted_test.Redacted",
        "squareup.protos.redacted_test.NotRedacted",
        "squareup.protos.redacted_test.RedactedChild",
        "squareup.protos.redacted_test.RedactedCycleA",
        "squareup.protos.redacted_test.RedactedCycleB",
        "squareup.protos.redacted_test.RedactedRepeated",
        "squareup.protos.redacted_test.RedactedRequired",
        "google.protobuf.FieldOptions"
    };
    testProto(sources, roots, outputs);
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
    String[] roots = new String[0];
    testProto(sources, roots, outputs);
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
    };
    String[] roots = { "squareup.protos.roots.A" };
    testProtoWithRoots(sources, roots, outputs);
  }

  @Test public void testRootsB() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/roots/B.java",
        "com/squareup/wire/protos/roots/C.java"
    };
    String[] roots = { "squareup.protos.roots.B" };
    testProtoWithRoots(sources, roots, outputs);
  }

  @Test public void testRootsE() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/roots/E.java",
        "com/squareup/wire/protos/roots/G.java"
    };
    String[] roots = { "squareup.protos.roots.E" };
    testProtoWithRoots(sources, roots, outputs);
  }

  @Test public void testRootsH() throws Exception {
    String[] sources = {
        "roots.proto"
    };
    String[] outputs = {
        "com/squareup/wire/protos/roots/E.java",
        "com/squareup/wire/protos/roots/G.java",
        "com/squareup/wire/protos/roots/H.java"
    };
    String[] roots = { "squareup.protos.roots.H" };
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
    String[] roots = { "squareup.protos.roots.I" };
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
    String[] roots = {
        "squareup.wire.protos.roots.TheService", "squareup.wire.protos.roots.UnnecessaryService"
    };
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
    String[] roots = { "squareup.wire.protos.roots.TheService" };
    testProtoWithRoots(sources, roots, outputs);
  }

  //@Test public void sanitizeJavadocStripsTrailingWhitespace() {
  //  String input = "The quick brown fox  \nJumps over  \n\t \t\nThe lazy dog  ";
  //  String expected = "The quick brown fox\nJumps over\n\nThe lazy dog";
  //  assertEquals(expected, MessageWriter.sanitizeJavadoc(input));
  //}
  //
  //@Test public void sanitizeJavadocGuardsFormatCharacters() {
  //  String input = "This is 12% of %s%d%f%c!";
  //  String expected = "This is 12%% of %%s%%d%%f%%c!";
  //  assertEquals(expected, MessageWriter.sanitizeJavadoc(input));
  //}
  //
  //@Test public void sanitizeJavadocWrapsSeeLinks() {
  //  String input = "Google query.\n\n@see http://google.com";
  //  String expected = "Google query.\n\n@see <a href=\"http://google.com\">http://google.com</a>";
  //  assertEquals(expected, MessageWriter.sanitizeJavadoc(input));
  //}

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
