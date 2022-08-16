package com.squareup.wire.protocwire.cmd

import com.squareup.wire.protocwire.CodeGenerator
import com.squareup.wire.protocwire.Plugin
import com.google.protobuf.compiler.PluginProtos
import com.squareup.wire.protocwire.JavaTargetConfig
import com.squareup.wire.protocwire.KotlinTargetConfig
import com.squareup.wire.protocwire.ParameterParser
import com.squareup.wire.protocwire.WireGenerator

class JavaGenerator : CodeGenerator {

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      Plugin.run(JavaGenerator())
    }
  }

  override fun generate(
    request: PluginProtos.CodeGeneratorRequest,
    descriptorSource: Plugin.DescriptorSource,
    response: Plugin.Response
  ) {
    val parsedMap = ParameterParser.parse(request.parameter)
    val target = JavaTargetConfig.parse(parsedMap)
    val generator = WireGenerator(target)
    generator.generate(request, descriptorSource, response)
  }
}

class KotlinGenerator: CodeGenerator {

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      Plugin.run(KotlinGenerator())
    }
  }

  override fun generate(
    request: PluginProtos.CodeGeneratorRequest,
    descriptorSource: Plugin.DescriptorSource,
    response: Plugin.Response
  ) {
    val parsedMap = ParameterParser.parse(request.parameter)
    val target = KotlinTargetConfig.parse(parsedMap)
    val generator = WireGenerator(target)
    generator.generate(request, descriptorSource, response)
  }
}
