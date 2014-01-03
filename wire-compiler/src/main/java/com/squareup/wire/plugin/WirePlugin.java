package com.squareup.wire.plugin;

import com.squareup.protoparser.ProtoFile;
import java.util.List;
import java.util.Set;

public interface WirePlugin {
  /** A short argument prefix for the command line. Should match {@code [a-z]+}. */
  String getArgumentPrefix();

  /**
   * TODO describe me!
   *
   * @param args List of arguments for this plugin. The {@link #getArgumentPrefix() plugin prefix}
   * is not included in these arguments. (e.g., for the prefix "foo", {@code --foo-bar=baz} will
   * appear in this list as {@code bar=baz}).
   * @param protoFiles The filtered types to generate.
   */
  void generate(List<String> args, Set<ProtoFile> protoFiles);
}
