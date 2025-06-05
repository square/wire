/*
 * Copyright (C) 2025 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.binarycompatibility.kotlin

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class WireBinaryCompatibilityKotlinPluginTest {
  @Test
  fun rewriteConstructorCallToBuilderCall() {
    val result = compile(
      sourceFile = SourceFile.kotlin(
        "Sample.kt",
        """
        package com.squareup.wire

        val log = mutableListOf<String>()

        fun callConstructor() {
            log += "${'$'}{Money(5, "USD")}"
          }

        fun callConstructorWithDefaultParameters() {
            log += "${'$'}{Money(amount = 5)}"
            log += "${'$'}{Money(currencyCode = "USD")}"
          }

        data class Money(
          val amount: Long? = null,
          val currencyCode: String? = null,
        ) : Message {

          class Builder : Message.Builder {
            var amount: Long? = null
            var currencyCode: String? = null

            fun amount(amount: Long) : Builder {
              log += "calling amount()!"
              this.amount = amount
              return this
            }

            fun currencyCode(currencyCode: String) : Builder {
              log += "calling currencyCode()!"
              this.currencyCode = currencyCode
              return this
            }
            fun build() : Money = Money(amount, currencyCode)
          }
        }

        interface Message {
          interface Builder
        }
        """,
      ),
    )
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

    val testClass = result.classLoader.loadClass("com.squareup.wire.SampleKt")
    val log = testClass.getMethod("getLog")
      .invoke(null) as MutableList<String>

    testClass.getMethod("callConstructor").invoke(null)
    assertThat(log).containsExactly(
      "calling amount()!",
      "calling currencyCode()!",
      "Money(amount=5, currencyCode=USD)",
    )
    log.clear()

    testClass.getMethod("callConstructorWithDefaultParameters").invoke(null)
    assertThat(log).containsExactly(
      "calling amount()!",
      "Money(amount=5, currencyCode=null)",
      "calling currencyCode()!",
      "Money(amount=null, currencyCode=USD)",
    )
    log.clear()
  }
}

@ExperimentalCompilerApi
fun compile(
  sourceFiles: List<SourceFile>,
  plugin: CompilerPluginRegistrar = WireBinaryCompatibilityCompilerPluginRegistrar(),
): JvmCompilationResult {
  return KotlinCompilation().apply {
    sources = sourceFiles
    compilerPluginRegistrars = listOf(plugin)
    inheritClassPath = true
    kotlincArguments += "-Xverify-ir=error"
    kotlincArguments += "-Xverify-ir-visibility"
  }.compile()
}

@ExperimentalCompilerApi
fun compile(
  sourceFile: SourceFile,
  plugin: CompilerPluginRegistrar = WireBinaryCompatibilityCompilerPluginRegistrar(),
): JvmCompilationResult {
  return compile(listOf(sourceFile), plugin)
}
