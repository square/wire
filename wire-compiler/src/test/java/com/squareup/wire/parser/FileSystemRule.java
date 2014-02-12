package com.squareup.wire.parser;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import java.nio.file.FileSystem;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class FileSystemRule implements TestRule {
  private FileSystem fs;

  public FileSystem get() {
    return fs;
  }

  @Override public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override public void evaluate() throws Throwable {
        fs = MemoryFileSystemBuilder.newEmpty().build("name");
        try {
          base.evaluate();
        } finally {
          fs.close();
        }
      }
    };
  }
}
