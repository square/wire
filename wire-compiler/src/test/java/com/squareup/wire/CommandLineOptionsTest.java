package com.squareup.wire;

import com.google.common.collect.Lists;
import com.squareup.wire.java.SimpleServiceFactory;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandLineOptionsTest {

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
    assertThat(options.javaOut).isEqualTo(new File("baz/qux"));
  }

  @Test public void sourceFileNames() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertThat(options.sourceFileNames).isEmpty();

    List<String> expected = new ArrayList<String>();
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
      List<String> expected = new ArrayList<String>();
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
    List<String> expected = new ArrayList<String>();
    expected.add("com.example.foo");
    assertThat(options.roots).isEqualTo(expected);
    options = new CommandLineOptions("--roots=com.example.foo,com.example.bar");
    expected.add("com.example.bar");
    assertThat(options.roots).isEqualTo(expected);
  }

  @Test public void registryClass() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertThat(options.registryClass).isNull();
    options = new CommandLineOptions("--registry_class=com.example.RegistryClass");
    assertThat(options.registryClass).isEqualTo("com.example.RegistryClass");
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
    Set<String> expected = new HashSet<String>();
    expected.add("foo");
    assertThat(options.enumOptions).isEqualTo(expected);
    options = new CommandLineOptions("--enum_options=foo,bar");
    expected.add("bar");
    assertThat(options.enumOptions).isEqualTo(expected);
  }

  @Test public void serviceFactory() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertThat(options.serviceFactory).isNull();

    String name = SimpleServiceFactory.class.getName();
    options = new CommandLineOptions("--service_factory=" + name);
    assertThat(options.serviceFactory).isInstanceOf(SimpleServiceFactory.class);
  }

  @Test public void serviceFactoryOptions() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertThat(options.serviceFactoryOptions).isEmpty();

    options = new CommandLineOptions("--service_factory_opt=foo");
    List<String> expected = new ArrayList<String>();
    expected.add("foo");
    assertThat(options.serviceFactoryOptions).isEqualTo(expected);
    options = new CommandLineOptions("--service_factory_opt=foo", "--service_factory_opt=bar");
    expected.add("bar");
    assertThat(options.serviceFactoryOptions).isEqualTo(expected);
  }

}
