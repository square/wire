package com.squareup.wire.plugin;

import com.squareup.protoparser.ProtoFile;
import java.nio.file.FileSystem;
import java.util.Set;

public interface WirePlugin {
  /**
   * TODO describe me!
   *
   * @param fs The file system to use for writing generated files.
   * @param protoFiles The filtered types to generate.
   */
  void generate(FileSystem fs, Set<ProtoFile> protoFiles);
}
