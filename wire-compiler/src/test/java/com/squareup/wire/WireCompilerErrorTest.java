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

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.squareup.wire.schema.SchemaException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import okio.Okio;
import okio.Source;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class WireCompilerErrorTest {
  FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());

  /**
   * Compile a .proto containing in a String and returns the contents of each output file,
   * indexed by class name.
   */
  private void compile(String source) throws Exception {
    Path test = fileSystem.getPath("/source/test.proto");
    Files.createDirectory(fileSystem.getPath("/source"));
    Files.createDirectory(fileSystem.getPath("/target"));
    Files.write(test, source.getBytes(UTF_8));

    WireCompiler compiler = WireCompiler.forArgs(fileSystem, new StringWireLogger(),
        "--proto_path=/source", "--java_out=/target", "test.proto");
    compiler.compile();
  }

  @Test public void testCorrect() throws Exception {
    compile("package com.squareup.protos.test;\n"
        + "message Simple {\n"
        + "  optional int32 f = 1;\n"
        + "}\n");
    String generatedSource = readFile("/target/com/squareup/protos/test/Simple.java");
    assertThat(generatedSource).contains(
        "public final class Simple extends Message<Simple, Simple.Builder> {");
  }

  @Test public void testZeroTag() throws Exception {
    try {
      compile("package com.squareup.protos.test;\n"
          + "message Simple {\n"
          + "  optional int32 f = 0;\n"
          + "}\n");
      fail();
    } catch (SchemaException e) {
      assertThat(e).hasMessage("tag is out of range: 0\n"
          + "  for field f (/source/test.proto at 3:3)\n"
          + "  in message com.squareup.protos.test.Simple (/source/test.proto at 2:1)");
    }
  }

  @Test public void testDuplicateTag() throws Exception {
    try {
      compile("package com.squareup.protos.test;\n"
          + "message Simple {\n"
          + "  optional int32 f = 1;\n"
          + "  optional int32 g = 1;\n"
          + "}\n");
      fail();
    } catch (SchemaException e) {
      assertThat(e).hasMessage("multiple fields share tag 1:\n"
          + "  1. f (/source/test.proto at 3:3)\n"
          + "  2. g (/source/test.proto at 4:3)\n"
          + "  for message com.squareup.protos.test.Simple (/source/test.proto at 2:1)");
    }
  }

  @Test public void testEnumNamespace() throws Exception {
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
          + "  1. com.squareup.protos.test.Foo.Bar.QUIX (/source/test.proto at 4:7)\n"
          + "  2. com.squareup.protos.test.Foo.Bar2.QUIX (/source/test.proto at 10:7)\n"
          + "  for message com.squareup.protos.test.Foo (/source/test.proto at 2:3)");
    }
  }

  @Test public void testNoPackageNameIsLegal() throws Exception {
    compile("message Simple { optional int32 f = 1; }");
    // Output should not have a 'package' declaration.
    assertThat(readFile("/target/Simple.java")).doesNotContain("package");
  }

  private String readFile(String path) throws IOException {
    try (Source source = Okio.source(fileSystem.getPath(path))) {
      return Okio.buffer(source).readUtf8();
    }
  }
}
