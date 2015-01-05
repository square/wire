package com.squareup.wire;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CommandLineOptionsTest {

  @Test public void testProtoPath() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertNull(options.protoPath);

    options = new CommandLineOptions("--proto_path=foo/bar");
    assertEquals("foo/bar", options.protoPath);
  }

  @Test public void testJavaOut() throws Exception{
    CommandLineOptions options = new CommandLineOptions();
    assertNull(options.javaOut);

    options = new CommandLineOptions("--java_out=baz/qux");
    assertEquals("baz/qux", options.javaOut);
  }

  @Test public void testSourceFileNames() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertTrue(options.sourceFileNames.isEmpty());

    List<String> expected = new ArrayList<String>();
    options = new CommandLineOptions("baz", "qux");
    expected.add("baz");
    expected.add("qux");
    assertEquals(expected, options.sourceFileNames);
  }

  @Test public void testSourceFileNamesFromInclude() throws Exception {
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
      assertEquals(expected, options.sourceFileNames);

      // Test both --files and bare filenames together
      options = new CommandLineOptions("--files=" + tmpFile.getAbsolutePath(), "baz");
      expected.add("baz");
      assertEquals(expected, options.sourceFileNames);
    } finally {
      tmpFile.delete();
    }
  }

  @Test public void testRoots() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertTrue(options.roots.isEmpty());

    options = new CommandLineOptions("--roots=com.example.foo");
    List<String> expected = new ArrayList<String>();
    expected.add("com.example.foo");
    assertEquals(expected, options.roots);
    options = new CommandLineOptions("--roots=com.example.foo,com.example.bar");
    expected.add("com.example.bar");
    assertEquals(expected, options.roots);
  }

  @Test public void testRegistryClass() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertNull(options.registryClass);
    options = new CommandLineOptions("--registry_class=com.example.RegistryClass");
    assertEquals("com.example.RegistryClass", options.registryClass);
  }

  @Test public void  testEmitOptions() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertTrue(options.emitOptions);

    options = new CommandLineOptions("--no_options");
    assertFalse(options.emitOptions);
  }

  @Test public void testEnumOptions() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertTrue(options.enumOptions.isEmpty());

    options = new CommandLineOptions("--enum_options=foo");
    Set<String> expected = new HashSet<String>();
    expected.add("foo");
    assertEquals(expected, options.enumOptions);
    options = new CommandLineOptions("--enum_options=foo,bar");
    expected.add("bar");
    assertEquals(expected, options.enumOptions);
  }

  @Test public void testServiceWriter() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertNull(options.serviceWriter);

    String name = SimpleServiceWriter.class.getName();
    options = new CommandLineOptions("--service_writer=" + name);
    assertEquals("com.squareup.wire.SimpleServiceWriter", options.serviceWriter);
  }

  @Test public void testServiceWriterOptions() throws Exception {
    CommandLineOptions options = new CommandLineOptions();
    assertTrue(options.serviceWriterOptions.isEmpty());

    options = new CommandLineOptions("--service_writer_opt=foo");
    List<String> expected = new ArrayList<String>();
    expected.add("foo");
    assertEquals(expected, options.serviceWriterOptions);
    options = new CommandLineOptions("--service_writer_opt=foo", "--service_writer_opt=bar");
    expected.add("bar");
    assertEquals(expected, options.serviceWriterOptions);
  }

}
