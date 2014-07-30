// Copyright 2013 Square, Inc.
package com.squareup.wire.compiler.plugin.java.services;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.Service;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * A pluggable interface for generating interfaces for rps services defined in a .proto
 * file. The Wire compiler constructs instances of this interface according to the class
 * name passed into the '-Dservice_writer=' flag.  Implementing classes must have a constructor
 * that takes the same argument list as the constructor in this abstract superclass,
 * namely a JavaWriter and a list of String options.
 */
public abstract class ServiceWriter {

  protected final JavaWriter writer;
  protected final List<String> options;

  protected ServiceWriter(JavaWriter writer, List<String> options) {
    this.writer = writer;
    this.options = options;
  }

  /**
   * Emits all necessary code to produce a suitable Java interface for the given service.
   * The caller is responsible for emitting a Wire class comment and a 'package' declaration.
   * The importedTypes parameter is initialized to contain all the request and response classes.
   * This method is responsible for emitting imports for any ancillary dependencies
   * such as exceptions and annotations required by the interface definitions, and for emitting
   * an outer class or interface and all of its members.
   *
   * @param service the service being generated.
   * @param importedTypes a set of strings containing known imports.
   */
  public abstract void emitService(Service service, Set<String> importedTypes) throws IOException;
}
