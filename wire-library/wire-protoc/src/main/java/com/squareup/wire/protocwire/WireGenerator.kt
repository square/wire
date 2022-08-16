package com.squareup.wire.protocwire

import com.google.protobuf.Descriptors

class WireGenerator : CodeGenerator {
    override fun generate(fileToGenerate: Descriptors.FileDescriptor, parameter: String, context: CodeGenerator.Context) {
      println("++++++++++++++++++++++++++++")
      context.addFile("hello.txt", "hello world")
      println("++++++++++++++++++++++++++++")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Plugin.run(WireGenerator())
        }
    }
}
