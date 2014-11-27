// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.List;

/** A message type or enum type declaration. */
public interface TypeElement {
  String getName();
  String getFullyQualifiedName();
  String getDocumentation();
  List<OptionElement> getOptions();
  List<TypeElement> getNestedElements();
}
