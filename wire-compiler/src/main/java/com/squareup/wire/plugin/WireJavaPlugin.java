package com.squareup.wire.plugin;

import com.squareup.protoparser.ProtoFile;
import java.nio.file.FileSystem;
import java.util.Set;

public class WireJavaPlugin implements WirePlugin {
  @Override public void generate(FileSystem fs, Set<ProtoFile> protoFiles) {
    throw new UnsupportedOperationException("Not implemented."); // TODO
  }
}
