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
package com.squareup.wire.java

import com.palantir.javapoet.JavaFile
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Profile
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.internal.ProfileParser
import java.io.IOException

/** Helper class to run Java code generation. */
internal class JavaGeneratorHelper(private val schema: Schema) {
  private val profiles = mutableMapOf<String, Profile>()

  /**
   * Load a [Profile] on the file system.
   * @param name The qualified name of the file. This can contain slashes.
   * @param profileFile The content of the file.
   */
  fun withProfile(name: String, profileFile: String): JavaGeneratorHelper {
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

  @Throws(IOException::class)
  @JvmOverloads
  fun generateJava(typeName: String, profileName: String? = null): String {
    val javaGenerator = JavaGenerator.get(schema)
      .withProfile(profile(profileName))
    val type = schema.getType(typeName)
    val typeSpec = javaGenerator.generateType(type)
    val packageName = javaGenerator.generatedTypeName(type).packageName()
    val javaFile = JavaFile.builder(packageName, typeSpec)
      .build()
    return javaFile.toString()
  }
}
