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
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WireCompilerErrorTest {

  static class StringIO implements IO {
    private final String protoFileName;
    private final String source;
    private final Map<String, StringWriter> writers = new LinkedHashMap<String, StringWriter>();

    public StringIO(String protoFileName, String source) {
      this.protoFileName = "./" + protoFileName;
      this.source = source;
    }

    @Override
    public ProtoFile parse(String filename) throws IOException {
      if (filename.equals(protoFileName)) {
        return ProtoSchemaParser.parse(filename, new StringReader(source));
      } else {
        throw new FileNotFoundException();
      }
    }

    @Override
    public JavaWriter getJavaWriter(String javaOut, String javaPackage, String className)
        throws IOException {
      StringWriter writer = new StringWriter();
      writers.put(javaPackage + "." + className, writer);
      return new JavaWriter(writer);
    }

    public Map<String, String> getOutput() {
      Map<String, String> output = new LinkedHashMap<String, String>();
      for (Map.Entry<String, StringWriter> entry : writers.entrySet()) {
        output.put(entry.getKey(), entry.getValue().toString());
      }
      return output;
    }
  }

  /**
   * Compile a .proto containing in a String and returns the contents of each output file,
   * indexed by class name.
   */
  private Map<String, String> compile(String source) {
    StringIO io = new StringIO("test.proto", source);

    CommandLineOptions options = new CommandLineOptions(".", Arrays.asList("test.proto"),
        new ArrayList<String>(), ".", null, true, Collections.EMPTY_SET, null,
        Collections.EMPTY_LIST);

    @SuppressWarnings("unchecked")
    WireCompiler compiler = new WireCompiler(options, io);
    try {
      compiler.compile();
    } catch (IOException e) {
      fail();
    }
    return io.getOutput();
  }

  @Test public void testCorrect() throws IOException {
    Map<String, String> output = compile("package com.squareup.protos.test;\n"
        + "message Simple {\n"
        + "  optional int32 f = 1;\n"
        + "}\n");
    String generatedSource = output.get("com.squareup.protos.test.Simple");
    assertTrue(generatedSource.contains("public final class Simple extends Message {"));
  }

  @Test public void testZeroTag() {
    try {
      compile("package com.squareup.protos.test;\n"
          + "message Simple {\n"
          + "  optional int32 f = 0;\n"
          + "}\n");
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("Illegal tag value: 0", e.getMessage());
    }
  }

  @Test public void testDuplicateTag() {
    try {
      compile("package com.squareup.protos.test;\n"
          + "message Simple {\n"
          + "  optional int32 f = 1;\n"
          + "  optional int32 g = 1;\n"
          + "}\n");
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Duplicate tag 1 in com.squareup.protos.test.Simple", e.getMessage());
    }
  }

  @Test public void testEnumNamespace() {
    try {
      compile("package com.squareup.protos.test;\n"
          + "  message Foo {\n"
          + "    enum Bar {\n"
          + "      QUIX = 0;\n"
          + "      FOO = 1;\n"
          + "    }\n"
          + "\n"
          + "    enum Bar2 {\n"
          + "      BAZ = 0;\n"
          + "      QUIX = 1;\n"
          + "    }\n"
          + "}\n");
      fail();
    } catch (IllegalStateException e) {
      assertEquals("Duplicate enum name QUIX in scope com.squareup.protos.test.Foo", e.getMessage());
    }
  }

  @Test public void testNoPackageNameIsLegal() {
    Map<String, String> output = compile("message Simple { optional int32 f = 1; }");
    assertTrue(output.containsKey(".Simple"));
    // Output should not have a 'package' declaration.
    assertFalse(output.get(".Simple").contains("package"));
  }
}
