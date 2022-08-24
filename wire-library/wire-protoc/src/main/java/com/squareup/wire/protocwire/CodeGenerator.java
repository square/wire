package com.squareup.wire.protocwire;

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;

/**
 * Interface to be implemented by a code generator.
 */
public interface CodeGenerator {
  /**
   * Generates code for the given proto file, generating one or more files to
   * the given response.
   */
  void generate(CodeGeneratorRequest request, Plugin.DescriptorSource descriptorSource,
                Plugin.Response response);
}
