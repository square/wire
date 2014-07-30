package com.squareup.wire.compiler.plugin;

import com.squareup.wire.compiler.parser.WireParser;
import java.nio.file.FileSystem;

public interface WirePlugin {
  /**
   * TODO describe me!
   *
   * @param fs The file system to use for writing generated files.
   * @param parsedInput The parsed input to the code generation phase.
   */
  void generate(FileSystem fs, WireParser.ParsedInput parsedInput);
}
