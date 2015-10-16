package com.squareup.wire;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class CommandLineOptionsTest {

  @Test public void unknownArgumentFails() throws WireException {
    try {
      WireCompiler.forArgs("--do-work");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Unknown argument '--do-work'.");
    }
  }

  @Test public void protoPaths() throws Exception {
    WireCompiler compiler = WireCompiler.forArgs("--java_out=.");
    assertThat(compiler.protoPaths).isEmpty();

    compiler = WireCompiler.forArgs("--java_out=.", "--proto_path=foo/bar");
    assertThat(compiler.protoPaths).containsOnly("foo/bar");

    compiler = WireCompiler.forArgs(
        "--java_out=.", "--proto_path=foo/bar", "--proto_path=one/two", "--proto_path=three/four");
    assertThat(compiler.protoPaths).containsExactly("foo/bar", "one/two", "three/four");
  }

  @Test public void javaOut() throws Exception{
    try {
      WireCompiler.forArgs();
      fail();
    } catch (WireException expected) {
    }

    WireCompiler compiler = WireCompiler.forArgs("--java_out=baz/qux");
    assertThat(compiler.javaOut).isEqualTo("baz/qux");
  }

  @Test public void sourceFileNames() throws Exception {
    WireCompiler compiler = WireCompiler.forArgs("--java_out=.");
    assertThat(compiler.sourceFileNames).isEmpty();

    List<String> expected = new ArrayList<>();
    compiler = WireCompiler.forArgs("--java_out=.", "baz", "qux");
    expected.add("baz");
    expected.add("qux");
    assertThat(compiler.sourceFileNames).isEqualTo(expected);
  }

  @Test public void sourceFileNamesFromInclude() throws Exception {
    File tmpFile = File.createTempFile("proto", ".include");
    try {
      PrintWriter out = new PrintWriter(new FileOutputStream(tmpFile));
      out.println("foo");
      out.println("bar");
      out.close();

      WireCompiler compiler = WireCompiler.forArgs(
          "--java_out=.", "--files=" + tmpFile.getAbsolutePath());
      List<String> expected = new ArrayList<>();
      expected.add("foo");
      expected.add("bar");
      assertThat(compiler.sourceFileNames).isEqualTo(expected);

      // Test both --files and bare filenames together
      compiler = WireCompiler.forArgs(
          "--java_out=.", "--files=" + tmpFile.getAbsolutePath(), "baz");
      expected.add("baz");
      assertThat(compiler.sourceFileNames).isEqualTo(expected);
    } finally {
      tmpFile.delete();
    }
  }

  @Test public void roots() throws Exception {
    WireCompiler compiler = WireCompiler.forArgs("--java_out=.");
    assertThat(compiler.identifierSet.includesEverything()).isTrue();
    assertThat(compiler.identifierSet.excludesNothing()).isTrue();

    compiler = WireCompiler.forArgs("--java_out=.", "--roots=com.example.foo");
    assertThat(compiler.identifierSet.includes).containsExactly("com.example.foo");

    compiler = WireCompiler.forArgs("--java_out=.", "--roots=com.example.foo,com.example.bar");
    assertThat(compiler.identifierSet.includes)
        .containsExactly("com.example.foo", "com.example.bar");
  }

  @Test public void  emitOptions() throws Exception {
    WireCompiler compiler = WireCompiler.forArgs("--java_out=.");
    assertThat(compiler.emitOptions).isTrue();

    compiler = WireCompiler.forArgs("--java_out=.", "--no_options");
    assertThat(compiler.emitOptions).isFalse();
  }
}
