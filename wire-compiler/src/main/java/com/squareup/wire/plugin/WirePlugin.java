package com.squareup.wire.plugin;

import com.squareup.protoparser.ProtoFile;
import java.util.List;
import java.util.Set;

public interface WirePlugin {
  /** Called with a list of arguments for this plugin when invoked from the command line. */
  void parseArgs(List<String> args);

  /** TODO describe me! */
  void run(Set<ProtoFile> protoFiles);
}
