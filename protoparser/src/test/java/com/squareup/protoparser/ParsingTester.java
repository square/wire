package com.squareup.protoparser;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;

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

        try {
          ProtoParser.parseUtf8(file);
        } catch (Exception e) {
          e.printStackTrace();
          failed += 1;
        }
      }
    }

    System.out.println("\nTotal: " + total + "  Failed: " + failed);
  }
}
