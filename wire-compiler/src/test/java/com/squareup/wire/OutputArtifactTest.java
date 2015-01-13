package com.squareup.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OutputArtifactTest {

  @Test public void testOutputArtifact() {
    OutputArtifact artifact = new OutputArtifact("foo/bar", "com.company", "Foo");
    assertEquals("foo/bar", artifact.outputDirectory());
    assertEquals("com.company", artifact.javaPackage());
    assertEquals("Foo", artifact.className());
    assertEquals("foo/bar/com/company", artifact.dir().toString());
    assertEquals("foo/bar/com/company/Foo.java", artifact.file().toString());
    assertEquals("com.company.Foo", artifact.fullClassName());
  }
}
