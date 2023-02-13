package com.squareup.wire.protocwire

import com.squareup.wire.protocwire.Plugin.DescriptorSource
import com.google.protobuf.compiler.PluginProtos

/**
 * Interface to be implemented by a code generator.
 */
interface CodeGenerator {
    /**
     * Generates code for the given proto file, generating one or more files to
     * the given response.
     */
    fun generate(
        request: PluginProtos.CodeGeneratorRequest,
        descriptorSource: DescriptorSource,
        response: Plugin.Response
    )
}
