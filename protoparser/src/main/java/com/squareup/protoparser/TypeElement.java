// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.List;

/** A message type or enum type declaration. */
public interface TypeElement {
  String name();
  String qualifiedName();
  String documentation();
  List<OptionElement> options();
  List<TypeElement> nestedElements();
}
