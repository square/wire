// Copyright 2013 Square, Inc.
package com.squareup.wire;

import com.squareup.protoparser.MessageType;

final class FieldInfo {
  final String name;
  final MessageType.Label label;

  FieldInfo(String name, MessageType.Label label) {
    this.name = name;
    this.label = label;
  }
}
