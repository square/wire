/*
 * Copyright (C) 2022 Square, Inc.
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
package com.squareup.wire.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.wire.kotlin.EnumMode.ENUM_CLASS
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Profile
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.internal.ProfileParser

/** Helper class to run Kotlin code generation. */
internal class KotlinWithProfilesGenerator(private val schema: Schema) {
  private val profiles = mutableMapOf<String, Profile>()

  /**
   * Load a [Profile] on the file system.
   * @param name The qualified name of the file. This can contain slashes.
   * @param profileFile The content of the file.
   */
  fun withProfile(name: String, profileFile: String): KotlinWithProfilesGenerator {
    require(name.endsWith(".wire")) {
      "unexpected file extension for $name. Profile files should use the '.wire' extension"
    }

    val profile = Profile(listOf(ProfileParser(Location.get(""), profileFile).read()))
    val existingEntry = profiles.putIfAbsent(name, profile)
    check(existingEntry == null) { "A profile was already set: $name" }

    return this
  }

  private fun profile(profileName: String?): Profile {
    return if (profileName == null) Profile() else profiles["$profileName.wire"]!!
  }

  fun generateKotlin(
    typeName: String,
    profileName: String? = null,
    boxOneOfsMinSize: Int = 5_000,
    buildersOnly: Boolean = false,
    javaInterop: Boolean = false,
    enumMode: EnumMode = EnumMode.ENUM_CLASS,
    mutableTypes: Boolean = false,
  ): String {
    val kotlinGenerator = KotlinGenerator(
      schema,
      profile = profile(profileName),
      boxOneOfsMinSize = boxOneOfsMinSize,
      buildersOnly = buildersOnly,
      javaInterop = javaInterop,
      enumMode = enumMode,
      mutableTypes = mutableTypes,
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
    profileName: String? = null,
    nameSuffix: String? = null,
  ): List<String> {
    if (rpcRole === RpcRole.NONE) return emptyList()

    val grpcGenerator = KotlinGenerator(
      schema = schema,
      profile = profile(profileName),
      emitAndroid = false,
      javaInterop = false,
      rpcCallStyle = rpcCallStyle,
      rpcRole = rpcRole,
      nameSuffix = nameSuffix,
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
