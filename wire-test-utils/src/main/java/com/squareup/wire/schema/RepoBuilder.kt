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

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.java.JavaGenerator
import com.squareup.wire.java.Profile
import com.squareup.wire.java.ProfileLoader
import com.squareup.wire.kotlin.KotlinGenerator
import com.squareup.wire.kotlin.RpcCallStyle
import com.squareup.wire.kotlin.RpcRole
import okio.buffer
import okio.source
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

/**
 * Builds a repository of `.proto` and `.wire` files to create schemas, profiles, and adapters for
 * testing.
 */
class RepoBuilder {
  private val fs = Jimfs.newFileSystem(Configuration.unix())
  private val root = fs.getPath("/source")
  private val schemaLoader = SchemaLoader().addSource(root)

  fun add(name: String, protoFile: String): RepoBuilder {
    if (name.endsWith(".proto")) {
      schemaLoader.addProto(name)
    } else if (!name.endsWith(".wire")) {
      throw IllegalArgumentException("unexpected file extension: $name")
    }

    val relativePath = fs.getPath(name)
    try {
      val resolvedPath = root.resolve(relativePath)
      val parent = resolvedPath.parent
      if (parent != null) {
        Files.createDirectories(parent)
      }
      Files.write(resolvedPath, protoFile.toByteArray(UTF_8))
    } catch (e: IOException) {
      throw AssertionError(e)
    }

    return this
  }

  @Throws(IOException::class)
  fun add(path: String): RepoBuilder {
    val file = File("../wire-tests/src/commonTest/proto/$path")
    file.source().use { source ->
      val protoFile = source.buffer().readUtf8()
      return add(path, protoFile)
    }
  }

  fun schema(): Schema {
    try {
      return schemaLoader.load()
    } catch (e: IOException) {
      throw RuntimeException(e)
    }

  }

  @Throws(IOException::class)
  fun profile(name: String): Profile {
    return ProfileLoader(fs, name)
        .schema(schema())
        .load()
  }

  @Throws(IOException::class)
  fun protoAdapter(messageTypeName: String): ProtoAdapter<Any> {
    return schema().protoAdapter(messageTypeName, true)
  }

  @Throws(IOException::class)
  @JvmOverloads fun generateCode(typeName: String, profile: String? = null): String {
    val schema = schema()
    var javaGenerator = JavaGenerator.get(schema)
    if (profile != null) {
      javaGenerator = javaGenerator.withProfile(profile(profile))
    }
    val type = schema.getType(typeName)
    val typeSpec = javaGenerator.generateType(type)
    val packageName = javaGenerator.generatedTypeName(type).packageName()
    val javaFile = JavaFile.builder(packageName, typeSpec)
        .build()
    return javaFile.toString()
  }

  fun generateKotlin(typeName: String): String {
    val schema = schema()
    val kotlinGenerator = KotlinGenerator(schema)
    val type = schema.getType(typeName)
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
    rpcRole: RpcRole = RpcRole.CLIENT
  ): String {
    val schema = schema()
    val grpcGenerator = KotlinGenerator(
        schema = schema,
        emitAndroid = false,
        javaInterop = false,
        rpcCallStyle = rpcCallStyle,
        rpcRole = rpcRole
    )
    val service = schema.getService(serviceName)
    val rpc = rpcName?.let { service.rpc(rpcName)!! }
    val typeSpec = grpcGenerator.generateService(service, rpc)
    val packageName = grpcGenerator.generatedServiceName(service).packageName
    val fileSpec = FileSpec.builder(packageName, "_")
        .addType(typeSpec)
        .build()
    return fileSpec.toString()
  }
}
