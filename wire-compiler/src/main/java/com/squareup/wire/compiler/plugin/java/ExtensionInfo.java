// Copyright 2013 Square, Inc.
package com.squareup.wire.compiler.plugin.java;

import com.squareup.protoparser.MessageType;

final class ExtensionInfo {
  public final String type;
  public final String fqType;
  public final String location;
  public final String fqLocation;
  public final MessageType.Label label;

  ExtensionInfo(String type, String fqType, String location, String fqLocation,
      MessageType.Label label) {
    this.type = type;
    this.fqType = fqType;
    this.location = location;
    this.fqLocation = fqLocation;
    this.label = label;
  }
}
