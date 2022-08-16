package com.squareup.wire.protocwire

import com.google.protobuf.Descriptors
import com.squareup.wire.schema.ProtoFile

class WireGenerator : CodeGenerator {
    override fun generate(fileToGenerate: Descriptors.FileDescriptor, parameter: String, context: CodeGenerator.Context) {
      // Call wire code?
      ProtoFile
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Plugin.run(WireGenerator())
        }
    }
}
