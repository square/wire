/*
 * Copyright 2018 Square Inc.
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

import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.FileSpec;
import com.squareup.kotlinpoet.TypeSpec;
import com.squareup.wire.kotlin.KotlinGenerator;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.Type;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.squareup.wire.WireCompiler.CODE_GENERATED_BY_WIRE;

class KotlinFileWriter implements Callable<Void> {
  private final String destination;
  private final KotlinGenerator kotlinGenerator;
  private final ConcurrentLinkedQueue<Type> queue;
  private final FileSystem fs;
  private final WireLogger log;
  private final boolean dryRun;

  KotlinFileWriter(String destination, KotlinGenerator kotlinGenerator,
      ConcurrentLinkedQueue<Type> queue, FileSystem fs, WireLogger log, boolean dryRun) {
    this.destination = destination;
    this.kotlinGenerator = kotlinGenerator;
    this.queue = queue;
    this.fs = fs;
    this.log = log;
    this.dryRun = dryRun;
  }

  @Override
  public Void call() throws IOException {
    while (true) {
      Type type = queue.poll();
      if (type == null) {
        return null;
      }

      TypeSpec typeSpec = kotlinGenerator.generateType(type);
      ClassName className = kotlinGenerator.generatedTypeName(type);
      Location location = type.location();

      FileSpec.Builder builder = FileSpec.builder(className.getPackageName(), typeSpec.getName())
          .addComment(CODE_GENERATED_BY_WIRE);
      if (location != null) {
        builder.addComment("\nSource file: %L", location.withPathOnly());
      }

      FileSpec kotlinFile = builder.addType(typeSpec)
          .build();

      Path path = fs.getPath(destination);
      log.artifact(path, kotlinFile);

      try {
        if (!dryRun) {
          kotlinFile.writeTo(path);
        }
      } catch (IOException e) {
        throw new IOException("Error emitting " + kotlinFile.getPackageName() + "."
            + className.getCanonicalName() + " to " + destination, e);
      }
    }
  }
}
