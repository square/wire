/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.sample;

import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.java.JavaGenerator;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;
import com.squareup.wire.schema.Service;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import okio.BufferedSink;
import okio.Okio;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public final class ServiceGeneratorTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test public void service() throws IOException {
    Schema schema = schema(ImmutableMap.of(
        "sample.proto", ""
            + "syntax = \"proto2\";\n"
            + "package squareup.wire.sample;\n"
            + "\n"
            + "message SampleMessage {\n"
            + "  repeated string array = 1;\n"
            + "}\n"
            + "\n"
            + "message SampleRequest {\n"
            + "  optional string name = 1;\n"
            + "  optional SampleMessage sample_message = 2;\n"
            + "}\n"
            + "\n"
            + "message SampleResponse {\n"
            + "  optional int32 age = 1;\n"
            + "}\n"
            + "\n"
            + "// This is it. A really fantastic service interface.\n"
            + "service SampleApi {\n"
            + "  // Call this RPC. You'll be glad you did!\n"
            + "  rpc FirstRpc (SampleRequest) returns (SampleResponse);\n"
            + "  rpc OtherOne (SampleRequest) returns (SampleResponse);\n"
            + "}\n"));
    Service service = schema.getService("squareup.wire.sample.SampleApi");

    JavaGenerator javaGenerator = JavaGenerator.get(schema);
    ServiceGenerator generator = new ServiceGenerator(javaGenerator);

    TypeSpec typeSpec = generator.api(service);
    assertThat(toString(typeSpec)).isEqualTo(""
        + "package squareup.wire.sample;\n"
        + "\n"
        + "/**\n"
        + " * This is it. A really fantastic service interface.\n"
        + " */\n"
        + "public interface SampleApi {\n"
        + "  /**\n"
        + "   * Call this RPC. You'll be glad you did!\n"
        + "   */\n"
        + "  SampleResponse FirstRpc(SampleRequest request);\n"
        + "\n"
        + "  SampleResponse OtherOne(SampleRequest request);\n"
        + "}\n");
  }

  private Schema schema(Map<String, String> fileToProto) throws IOException {
    SchemaLoader schemaLoader = new SchemaLoader();
    schemaLoader.addSource(temporaryFolder.getRoot());
    for (Map.Entry<String, String> entry : fileToProto.entrySet()) {
      File file = new File(temporaryFolder.getRoot(), entry.getKey());
      file.getParentFile().mkdirs();
      try (BufferedSink out = Okio.buffer(Okio.sink(file))) {
        out.writeUtf8(entry.getValue());
      }
      schemaLoader.addProto(entry.getKey());
    }
    return schemaLoader.load();
  }

  private String toString(TypeSpec typeSpec) throws IOException {
    StringBuilder result = new StringBuilder();
    JavaFile javaFile = JavaFile.builder("squareup.wire.sample", typeSpec)
        .build();
    javaFile.writeTo(result);
    return result.toString();
  }
}
