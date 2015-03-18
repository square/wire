// Copyright 2013 Square, Inc.
package com.squareup.wire;

import com.squareup.protoparser.FieldElement;

final class ExtensionInfo {
  public final String type;
  public final String fqType;
  public final String location;
  public final String fqLocation;
  public final FieldElement.Label label;

  ExtensionInfo(String type, String fqType, String location, String fqLocation,
      FieldElement.Label label) {
    this.type = type;
    this.fqType = fqType;
    this.location = location;
    this.fqLocation = fqLocation;
    this.label = label;
  }
}
