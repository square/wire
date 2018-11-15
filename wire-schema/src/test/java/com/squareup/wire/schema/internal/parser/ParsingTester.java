/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.schema.internal.parser;

import com.squareup.wire.schema.Location;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import okio.BufferedSource;
import okio.Okio;

/** Recursively traverse a directory and attempt to parse all of its proto files. */
public class ParsingTester {
  /** Directory under which to search for protos. Change as needed. */
  private static final File ROOT = new File("/path/to/protos");

  public static void main(String... args) {
    int total = 0;
    int failed = 0;

    Deque<File> fileQueue = new ArrayDeque<>();
    fileQueue.add(ROOT);
    while (!fileQueue.isEmpty()) {
      File file = fileQueue.removeFirst();
      if (file.isDirectory()) {
        Collections.addAll(fileQueue, file.listFiles());
      } else if (file.getName().endsWith(".proto")) {
        System.out.println("Parsing " + file.getPath());
        total += 1;

        try (BufferedSource in = Okio.buffer(Okio.source(file))) {
          String data = in.readUtf8();
          ProtoParser.parse(Location.get(file.getPath()), data);
        } catch (Exception e) {
          e.printStackTrace();
          failed += 1;
        }
      }
    }

    System.out.println("\nTotal: " + total + "  Failed: " + failed);
  }
}
