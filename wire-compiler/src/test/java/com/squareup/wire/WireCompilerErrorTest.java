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

import com.squareup.javapoet.JavaFile;
import com.squareup.wire.java.JavaGenerator;
import com.squareup.wire.schema.Loader;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.SchemaException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import okio.Buffer;
import okio.Source;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class WireCompilerErrorTest {

  static class StringIO implements Loader.IO, JavaGenerator.IO {
    private final String protoFileName;
    private final String source;
    private final Map<String, StringWriter> writers = new LinkedHashMap<String, StringWriter>();

    public StringIO(String protoFileName, String source) {
      this.protoFileName = protoFileName;
      this.source = source;
    }

    @Override public Location locate(String path) throws IOException {
      if (path.equals(protoFileName)) {
        return Location.get(path);
      } else {
        throw new FileNotFoundException();
      }
    }

    @Override public Source open(Location location) throws IOException {
      return new Buffer().writeUtf8(source);
    }

    public Map<String, String> getOutput() {
      Map<String, String> output = new LinkedHashMap<String, String>();
      for (Map.Entry<String, StringWriter> entry : writers.entrySet()) {
        output.put(entry.getKey(), entry.getValue().toString());
      }
      return output;
    }

    @Override public void write(File outputDirectory, JavaFile javaFile)
        throws IOException {
      StringWriter writer = new StringWriter();
      writers.put(javaFile.packageName + "." + javaFile.typeSpec.name, writer);
      javaFile.writeTo(writer);
    }
  }

  /**
   * Compile a .proto containing in a String and returns the contents of each output file,
   * indexed by class name.
   */
  private Map<String, String> compile(String source) {
    StringIO io = new StringIO("test.proto", source);

    CommandLineOptions options = new CommandLineOptions(".",  new File("."),
        Arrays.asList("test.proto"), new ArrayList<String>(), null, true,
        Collections.<String>emptySet(), null, Collections.<String>emptyList(), false, false);

    try {
      new WireCompiler(options, new Loader(io), io, new StringWireLogger(true)).compile();
    } catch (WireException e) {
      throw new AssertionError(e);
    }
    return io.getOutput();
  }

  @Test public void testCorrect() throws IOException {
    Map<String, String> output = compile("package com.squareup.protos.test;\n"
        + "message Simple {\n"
        + "  optional int32 f = 1;\n"
        + "}\n");
    String generatedSource = output.get("com.squareup.protos.test.Simple");
    assertThat(generatedSource).contains("public final class Simple extends Message {");
  }

  @Test public void testZeroTag() {
    try {
      compile("package com.squareup.protos.test;\n"
          + "message Simple {\n"
          + "  optional int32 f = 0;\n"
          + "}\n");
      fail();
    } catch (SchemaException e) {
      assertThat(e).hasMessage("tag is out of range: 0\n"
          + "  for field f (test.proto at 3:3)\n"
          + "  in message com.squareup.protos.test.Simple (test.proto at 2:1)");
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
    } catch (SchemaException e) {
      assertThat(e).hasMessage("multiple fields share tag 1:\n"
          + "  1. f (test.proto at 3:3)\n"
          + "  2. g (test.proto at 4:3)\n"
          + "  for message com.squareup.protos.test.Simple (test.proto at 2:1)");
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
    } catch (SchemaException e) {
      assertThat(e).hasMessage("multiple enums share constant QUIX:\n"
          + "  1. com.squareup.protos.test.Foo.Bar.QUIX (test.proto at 4:7)\n"
          + "  2. com.squareup.protos.test.Foo.Bar2.QUIX (test.proto at 10:7)\n"
          + "  for message com.squareup.protos.test.Foo (test.proto at 2:3)");
    }
  }

  @Test public void testNoPackageNameIsLegal() {
    Map<String, String> output = compile("message Simple { optional int32 f = 1; }");
    assertThat(output).containsKey(".Simple");
    // Output should not have a 'package' declaration.
    assertThat(output.get(".Simple")).doesNotContain("package");
  }
}
