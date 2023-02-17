/*
 * Copyright 2023 Block Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
