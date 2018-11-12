/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema;

import com.squareup.wire.schema.internal.parser.ProtoParser;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;

/** Recursively traverse a directory and attempt to parse all of its proto files. */
public final class ParsingTester {
  /** Directory under which to search for protos. Change as needed. */
  private static final Path ROOT = Paths.get("/path/to/protos");

  public static void main(String... args) throws IOException {
    final AtomicLong total = new AtomicLong();
    final AtomicLong failed = new AtomicLong();

    Files.walkFileTree(ROOT, new SimpleFileVisitor<Path>() {
      @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
          throws IOException {
        if (file.getFileName().toString().endsWith(".proto")) {
          total.incrementAndGet();

          String data = new String(Files.readAllBytes(file), UTF_8);
          Location location = Location.get(ROOT.toString(), file.toString());
          try {
            ProtoParser.parse(location, data);
          } catch (Exception e) {
            e.printStackTrace();
            failed.incrementAndGet();
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });

    System.out.println("\nTotal: " + total.get() + "  Failed: " + failed.get());

    if (failed.get() == 0) {
      new SchemaLoader().addSource(ROOT).load();
      System.out.println("All files linked successfully.");
    }
  }
}
