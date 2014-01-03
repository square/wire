package com.squareup.wire.plugin;

import com.squareup.protoparser.ProtoFile;
import java.util.List;
import java.util.Set;

public class WireJavaPlugin implements WirePlugin {
  @Override public String getArgumentPrefix() {
    return "java";
  }

  @Override public void generate(List<String> args, Set<ProtoFile> protoFiles) {
    throw new UnsupportedOperationException("Not implemented."); // TODO
  }
}
