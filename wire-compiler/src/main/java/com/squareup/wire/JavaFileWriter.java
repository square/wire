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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.java.JavaGenerator;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.Type;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

final class JavaFileWriter implements Callable<Void> {
  private final String destination;
  private final JavaGenerator javaGenerator;
  private final ConcurrentLinkedQueue<Type> queue;
  private final boolean dryRun;
  private final FileSystem fs;
  private final WireLogger log;

  JavaFileWriter(String destination, JavaGenerator javaGenerator, ConcurrentLinkedQueue<Type> queue,
      boolean dryRun, FileSystem fs, WireLogger log) {
    this.destination = destination;
    this.javaGenerator = javaGenerator;
    this.queue = queue;
    this.dryRun = dryRun;
    this.fs = fs;
    this.log = log;
  }

  @Override public Void call() throws IOException {
    while (true) {
      Type type = queue.poll();
      if (type == null) {
        return null;
      }

      TypeSpec typeSpec = javaGenerator.generateType(type);
      ClassName javaTypeName = javaGenerator.generatedTypeName(type);
      Location location = type.location();

      JavaFile.Builder builder = JavaFile.builder(javaTypeName.packageName(), typeSpec)
          .addFileComment("$L", WireCompiler.CODE_GENERATED_BY_WIRE);
      if (location != null) {
        builder.addFileComment("\nSource file: $L", location.withPathOnly());
      }
      JavaFile javaFile = builder.build();

      Path path = fs.getPath(destination);
      log.artifact(path, javaFile);

      try {
        if (!dryRun) {
          javaFile.writeTo(path);
        }
      } catch (IOException e) {
        throw new IOException("Error emitting " + javaFile.packageName + "."
            + javaFile.typeSpec.name + " to " + destination, e);
      }
    }
  }
}
