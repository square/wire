package com.squareup.wire.compiler.parser;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
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
        fs = Jimfs.newFileSystem(Configuration.unix());
        try {
          base.evaluate();
        } finally {
          fs.close();
        }
      }
    };
  }
}
