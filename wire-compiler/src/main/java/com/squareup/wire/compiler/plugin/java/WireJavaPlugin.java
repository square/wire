package com.squareup.wire.compiler.plugin.java;

import com.squareup.protoparser.ProtoFile;
import com.squareup.wire.compiler.plugin.WirePlugin;
import java.nio.file.FileSystem;
import java.util.Set;

public class WireJavaPlugin implements WirePlugin {
  @Override public void generate(FileSystem fs, Set<ProtoFile> protoFiles) {
    throw new UnsupportedOperationException("Not implemented."); // TODO
  }
}
