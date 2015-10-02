package com.squareup.wire;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class CommandLineOptionsTest {

  @Test public void unknownArgumentFails() throws WireException {
    try {
      new CommandLineOptions("--do-work");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Unknown argument '--do-work'.");
    }
  }

  @Test public void protoPaths() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertThat(options.protoPaths).isEmpty();

    options = new CommandLineOptions("--proto_path=foo/bar");
    assertThat(options.protoPaths).containsOnly("foo/bar");

    List<String> paths = Arrays.asList("foo/bar", "one/two", "three/four");
    String[] args = Lists.transform(paths, new com.google.common.base.Function<String, String>() {
      @Override
      public String apply(String s) {
        return "--proto_path=" + s;
      }
    }).toArray(new String[0]);
    options = new CommandLineOptions(args);
    assertThat(options.protoPaths).containsExactlyElementsOf(paths);
  }

  @Test public void javaOut() throws Exception{
    CommandLineOptions options = new CommandLineOptions();
    assertThat(options.javaOut).isNull();

    options = new CommandLineOptions("--java_out=baz/qux");
    assertThat(options.javaOut).isEqualTo("baz/qux");
  }

  @Test public void sourceFileNames() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertThat(options.sourceFileNames).isEmpty();

    List<String> expected = new ArrayList<>();
    options = new CommandLineOptions("baz", "qux");
    expected.add("baz");
    expected.add("qux");
    assertThat(options.sourceFileNames).isEqualTo(expected);
  }

  @Test public void sourceFileNamesFromInclude() throws Exception {
    File tmpFile = File.createTempFile("proto", ".include");
    try {
      PrintWriter out = new PrintWriter(new FileOutputStream(tmpFile));
      out.println("foo");
      out.println("bar");
      out.close();

      CommandLineOptions options = new CommandLineOptions("--files=" + tmpFile.getAbsolutePath());
      List<String> expected = new ArrayList<>();
      expected.add("foo");
      expected.add("bar");
      assertThat(options.sourceFileNames).isEqualTo(expected);

      // Test both --files and bare filenames together
      options = new CommandLineOptions("--files=" + tmpFile.getAbsolutePath(), "baz");
      expected.add("baz");
      assertThat(options.sourceFileNames).isEqualTo(expected);
    } finally {
      tmpFile.delete();
    }
  }

  @Test public void roots() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertThat(options.roots).isEmpty();

    options = new CommandLineOptions("--roots=com.example.foo");
    List<String> expected = new ArrayList<>();
    expected.add("com.example.foo");
    assertThat(options.roots).isEqualTo(expected);
    options = new CommandLineOptions("--roots=com.example.foo,com.example.bar");
    expected.add("com.example.bar");
    assertThat(options.roots).isEqualTo(expected);
  }

  @Test public void  emitOptions() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertThat(options.emitOptions).isTrue();

    options = new CommandLineOptions("--no_options");
    assertThat(options.emitOptions).isFalse();
  }

  @Test public void enumOptions() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertThat(options.enumOptions).isEmpty();

    options = new CommandLineOptions("--enum_options=foo");
    Set<String> expected = new HashSet<>();
    expected.add("foo");
    assertThat(options.enumOptions).isEqualTo(expected);
    options = new CommandLineOptions("--enum_options=foo,bar");
    expected.add("bar");
    assertThat(options.enumOptions).isEqualTo(expected);
  }
}
