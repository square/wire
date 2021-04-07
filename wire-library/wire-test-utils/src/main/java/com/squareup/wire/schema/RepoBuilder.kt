/*
 * Copyright (C) 2015 Square, Inc.
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
import com.squareup.kotlinpoet.FileSpec
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.java.JavaGenerator
import com.squareup.wire.java.Profile
import com.squareup.wire.kotlin.KotlinGenerator
import com.squareup.wire.kotlin.RpcCallStyle
import com.squareup.wire.kotlin.RpcRole
import java.io.File
import java.io.IOException
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.source

/**
 * Builds a repository of `.proto` and `.wire` files to create schemas, profiles, and adapters for
 * testing.
 */
class RepoBuilder {
  private val fs = FakeFileSystem()
  private val root = "/source".toPath()
  private val schemaLoader = SchemaLoader(fs)
  private var schema: Schema? = null

  fun add(name: String, protoFile: String): RepoBuilder {
    require(name.endsWith(".proto") || name.endsWith(".wire")) {
      "unexpected file extension: $name"
    }

    val relativePath = name.toPath()
    try {
      val resolvedPath = root / relativePath
      val parent = resolvedPath.parent
      if (parent != null) {
        fs.createDirectories(parent)
      }
      fs.write(resolvedPath) {
        writeUtf8(protoFile)
      }
    } catch (e: IOException) {
      throw AssertionError(e)
    }

    return this
  }

  @Throws(IOException::class)
  fun add(path: String): RepoBuilder {
    val file = File("../wire-tests/src/commonTest/proto/java/$path")
    file.source().use { source ->
      val protoFile = source.buffer().readUtf8()
      return add(path, protoFile)
    }
  }

  @Throws(IOException::class)
  fun addLocal(path: String): RepoBuilder {
    val file = File(path)
    file.source().use { source ->
      val protoFile = source.buffer().readUtf8()
      return add(path, protoFile)
    }
  }

  @Throws(IOException::class)
  fun schema(): Schema {
    var result = schema
    if (result == null) {
      schemaLoader.initRoots(sourcePath = listOf(Location.get("/source")))
      result = schemaLoader.loadSchema()
      schema = result
    }
    return result
  }

  @Throws(IOException::class)
  private fun profile(profileName: String?): Profile {
    if (profileName == null) return Profile()
    return schemaLoader.loadProfile(profileName, schema())
  }

  @Throws(IOException::class)
  fun protoAdapter(messageTypeName: String): ProtoAdapter<Any> {
    return schema().protoAdapter(messageTypeName, true)
  }

  @Throws(IOException::class)
  @JvmOverloads fun generateCode(typeName: String, profileName: String? = null): String {
    val schema = schema()
    val javaGenerator = JavaGenerator.get(schema)
      .withProfile(profile(profileName))
    val type = schema.getType(typeName)
    val typeSpec = javaGenerator.generateType(type)
    val packageName = javaGenerator.generatedTypeName(type).packageName()
    val javaFile = JavaFile.builder(packageName, typeSpec)
        .build()
    return javaFile.toString()
  }

  fun generateKotlin(
    typeName: String,
    emitKotlinSerialization: Boolean = false,
    profileName: String? = null
  ): String {
    val schema = schema()
    val kotlinGenerator = KotlinGenerator(
        schema,
        profile = profile(profileName),
        emitKotlinSerialization = emitKotlinSerialization
    )
    val type = schema.getType(typeName)!!
    val typeSpec = kotlinGenerator.generateType(type)
    val packageName = kotlinGenerator.generatedTypeName(type).packageName
    val fileSpec = FileSpec.builder(packageName, "_")
        .addType(typeSpec)
        .addImport("com.squareup.wire.kotlin", "decodeMessage")
        .build()
    return fileSpec.toString()
  }

  fun generateGrpcKotlin(
    serviceName: String,
    rpcName: String? = null,
    rpcCallStyle: RpcCallStyle = RpcCallStyle.SUSPENDING,
    rpcRole: RpcRole = RpcRole.CLIENT,
    profileName: String? = null
  ): List<String> {
    if (rpcRole === RpcRole.NONE) return emptyList()

    val schema = schema()
    val grpcGenerator = KotlinGenerator(
        schema = schema,
        profile = profile(profileName),
        emitAndroid = false,
        javaInterop = false,
        rpcCallStyle = rpcCallStyle,
        rpcRole = rpcRole
    )
    val service = schema.getService(serviceName)!!
    val rpc = rpcName?.let { service.rpc(rpcName)!! }
    return grpcGenerator.generateServiceTypeSpecs(service, rpc)
        .map { (typeName, typeSpec) ->
          FileSpec.builder(typeName.packageName, "_")
              .addType(typeSpec)
              .build()
        }
        .map(FileSpec::toString)
  }
}
