package com.squareup.wire.protocwire

import com.google.protobuf.compiler.PluginProtos
import java.io.File
import java.io.InputStream

private val devPath = "/Users/buildbreaker/development/"
// Absolute path is used because IJ and terminal has different home directories.
val stubbedRequestFile = "$devPath/wire/wire-library/request.binary"

class StubbedRequestDebugging {
  companion object {
    fun debug(request: PluginProtos.CodeGeneratorRequest, context: CodeGenerator.Context) {
      val directory = File(devPath)
      if (!directory.exists()) {
        throw RuntimeException("change the devPath in this file.")
      }
      val f = File(stubbedRequestFile)
      f.createNewFile()
      f.writeBytes(request.toByteArray())
    }

    @JvmStatic
    fun main(args: Array<String>) {
      Plugin.run(WireGenerator(), StubbedTestEnvironment())
    }
  }
}

class StubbedTestEnvironment() : Plugin.DefaultEnvironment() {
  override fun getInputStream(): InputStream {
    return File(stubbedRequestFile).inputStream()
  }
}
