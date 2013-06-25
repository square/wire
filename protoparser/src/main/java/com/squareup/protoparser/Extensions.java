// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

public final class Extensions {
  private final String documentation;
  private final int start;
  private final int end;

  public Extensions(String documentation, int start, int end) {
    this.documentation = documentation;
    this.start = start;
    this.end = end;
  }

  public String getDocumentation() {
    return documentation;
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }
}
