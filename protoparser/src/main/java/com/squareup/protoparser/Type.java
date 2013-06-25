// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.List;

/** A message type or enum type declaration. */
public interface Type {
  String getName();
  String getFullyQualifiedName();

  List<Type> getNestedTypes();
}
