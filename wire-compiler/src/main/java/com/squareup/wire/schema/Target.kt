/*
 * Copyright 2018 Square Inc.
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
package com.squareup.wire.schema

import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.WireCompiler
import com.squareup.wire.WireLogger
import com.squareup.wire.java.JavaGenerator
import com.squareup.wire.java.ProfileLoader
import com.squareup.wire.kotlin.KotlinGenerator
import com.squareup.wire.kotlin.RpcCallStyle
import com.squareup.wire.kotlin.RpcRole
import java.io.IOException
import java.nio.file.FileSystem

sealed class Target {
  /**
   * Proto types to include generated sources for. Types listed here will be generated for this
   * target and not for subsequent targets in the task.
   *
   * This list should contain package names (suffixed with `.*`) and type names only. It should
   * not contain member names.
   */
  abstract val includes: List<String>

  /**
   * Proto types to excluded generated sources for. Types listed here will not be generated for this
   * target.
   *
   * This list should contain package names (suffixed with `.*`) and type names only. It should
   * not contain member names.
   */
  abstract val excludes: List<String>

  /**
   * True if types emitted for this target should not also be emitted for other targets. Use this
   * to cause multiple outputs to be emitted for the same input type.
   */
  abstract val exclusive: Boolean

  internal abstract fun newHandler(
    schema: Schema,
    fs: FileSystem,
    logger: WireLogger
  ): SchemaHandler

  /** Generate `.java` sources. */
  data class JavaTarget(
    override val includes: List<String> = listOf("*"),
    override val excludes: List<String> = listOf(),

    override val exclusive: Boolean = true,

    val outDirectory: String,

    /** True for emitted types to implement `android.os.Parcelable`. */
    val android: Boolean = false,

    /** True to enable the `androidx.annotation.Nullable` annotation where applicable. */
    val androidAnnotations: Boolean = false,

    /**
     * True to emit code that uses reflection for reading, writing, and toString methods which are
     * normally implemented with generated code.
     */
    val compact: Boolean = false
  ) : Target() {
    override fun newHandler(schema: Schema, fs: FileSystem, logger: WireLogger): SchemaHandler {
      val profileName = if (android) "android" else "java"
      val profile = ProfileLoader(fs, profileName)
          .schema(schema)
          .load()

      val javaGenerator = JavaGenerator.get(schema)
          .withProfile(profile)
          .withAndroid(android)
          .withAndroidAnnotations(androidAnnotations)
          .withCompact(compact)

      return object : SchemaHandler {
        override fun handle(type: Type) {
          val typeSpec = javaGenerator.generateType(type)
          val javaTypeName = javaGenerator.generatedTypeName(type)
          val javaFile = JavaFile.builder(javaTypeName.packageName(), typeSpec)
              .addFileComment("\$L",
                  WireCompiler.CODE_GENERATED_BY_WIRE)
              .apply {
                val location = type.location()
                if (location != null) {
                  addFileComment("\nSource file: \$L", location.withPathOnly())
                }
              }.build()

          val path = fs.getPath(outDirectory)
          logger.artifact(path, javaFile)
          try {
            javaFile.writeTo(path)
          } catch (e: IOException) {
            throw IOException("Error emitting ${javaFile.packageName}.${javaFile.typeSpec.name} " +
                "to $outDirectory", e)
          }
        }

        override fun handle(service: Service) {
          // Service handling isn't supporting in Java.
        }
      }
    }
  }

  /** Generate `.kt` sources. */
  data class KotlinTarget(
    override val includes: List<String> = listOf("*"),
    override val excludes: List<String> = listOf(),

    override val exclusive: Boolean = true,

    val outDirectory: String,

    /** True for emitted types to implement `android.os.Parcelable`. */
    val android: Boolean = false,

    /** True for emitted types to implement APIs for easier migration from the Java target. */
    val javaInterop: Boolean = false,

    /** True for emitted services to provide blocking APIs only. */
    val blockingServices: Boolean = false,

    /** True for emitted services to implement one interface per RPC. */
    val singleMethodServices: Boolean = false
  ) : Target() {
    override fun newHandler(schema: Schema, fs: FileSystem, logger: WireLogger): SchemaHandler {
      val rpcCallStyle = if (blockingServices) RpcCallStyle.BLOCKING else RpcCallStyle.SUSPENDING
      val rpcRole = if (blockingServices) RpcRole.SERVER else RpcRole.CLIENT

      val kotlinGenerator = KotlinGenerator(schema, android, javaInterop, rpcCallStyle, rpcRole)

      return object : SchemaHandler {
        override fun handle(type: Type) {
          val typeSpec = kotlinGenerator.generateType(type)
          val className = kotlinGenerator.generatedTypeName(type)
          val kotlinFile = FileSpec.builder(className.packageName, typeSpec.name!!)
              .addComment(WireCompiler.CODE_GENERATED_BY_WIRE)
              .apply {
                val location = type.location()
                if (location != null) {
                  addComment("\nSource file: %L", location.withPathOnly())
                }
              }
              .addType(typeSpec)
              .build()

          val path = fs.getPath(outDirectory)
          logger.artifact(path, kotlinFile)

          try {
            kotlinFile.writeTo(path)
          } catch (e: IOException) {
            throw IOException("Error emitting " +
                "${kotlinFile.packageName}.${className.canonicalName} to $outDirectory", e)
          }
        }

        override fun handle(service: Service) {
          if (singleMethodServices) {
            service.rpcs().forEach { rpc ->
              write(
                  service,
                  kotlinGenerator.generatedServiceName(service, rpc),
                  kotlinGenerator.generateService(service, rpc)
              )
            }
          } else {
            write(
                service,
                kotlinGenerator.generatedServiceName(service),
                kotlinGenerator.generateService(service)
            )
          }
        }

        private fun write(service: Service, name: ClassName, typeSpec: TypeSpec) {
          val kotlinFile = FileSpec.builder(name.packageName, name.simpleName)
              .addComment(WireCompiler.CODE_GENERATED_BY_WIRE)
              .apply {
                service.location()?.let { addComment("\nSource file: %L", it.withPathOnly()) }
              }
              .addType(typeSpec)
              .build()

          val path = fs.getPath(outDirectory)
          logger.artifact(path, kotlinFile)

          try {
            kotlinFile.writeTo(path)
          } catch (e: IOException) {
            throw IOException("Error emitting " +
                "${kotlinFile.packageName}.${service.type()} to $outDirectory", e)
          }
        }
      }
    }
  }

  /** Omit code generation for these sources. Use this for a dry-run. */
  data class NullTarget(
    override val includes: List<String> = listOf("*"),
    override val excludes: List<String> = listOf()
  ) : Target() {
    override val exclusive: Boolean = true

    override fun newHandler(schema: Schema, fs: FileSystem, logger: WireLogger): SchemaHandler {
      return object : SchemaHandler {
        override fun handle(type: Type) {
          logger.artifactSkipped(type.type())
        }

        override fun handle(service: Service) {
          logger.artifactSkipped(service.type())
        }
      }
    }
  }

  interface SchemaHandler {
    fun handle(type: Type)
    fun handle(service: Service)
  }
}
