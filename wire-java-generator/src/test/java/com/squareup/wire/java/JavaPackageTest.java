package com.squareup.wire.java;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class JavaPackageTest {

  @Test(expected = NullPointerException.class)
  public void notNull() {
    JavaPackage.parse(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void notBlank() {
    JavaPackage.parse("\t");
  }

  @Test(expected = IllegalArgumentException.class)
  public void notTrimmed() {
    JavaPackage.parse("  valid.but.leading.space");
  }

  @Test(expected = IllegalArgumentException.class)
  public void mayNotStartWithDot() {
    JavaPackage.parse(".a.b.c");
  }

  @Test(expected = IllegalArgumentException.class)
  public void mayNotEndWithDot() {
    JavaPackage.parse("a.b.c.");
  }

  @Test
  public void someThingsAreOk() {
    assertEquals("", JavaPackage.parse("").asString());
    assertEquals("a", JavaPackage.parse("a").asString());
    assertEquals("a.b", JavaPackage.parse("a.b").asString());
  }

  @Test
  public void plussing() {
    assertEquals("", JavaPackage.parse("").plus("").asString());
    assertEquals("a.b", JavaPackage.parse("a.b").plus("").asString());
    assertEquals("a.b", JavaPackage.parse("").plus("a.b").asString());
    assertEquals("a.b", JavaPackage.parse("a").plus("b").asString());
  }

  @Test(expected = NullPointerException.class)
  public void plusStringNull() {
    JavaPackage.parse("").plus((String)null);
  }

  @Test(expected = NullPointerException.class)
  public void plusJavaPackageNull() {
    JavaPackage.parse("").plus((JavaPackage)null);
  }

  @Test
  public void emptyStringIsRoot() {
    assertEquals(JavaPackage.ROOT, JavaPackage.parse(""));
  }

}
