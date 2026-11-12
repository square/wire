/*
 * Copyright (C) 2026 Square, Inc.
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
package com.squareup.wire

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import kotlin.test.Test

/**
 * Kotlin mangles the JVM module name into the accessors of internal members. Reflection-based
 * serializers such as Jackson can store those accessor names in persisted payloads, so a module
 * name change breaks users who read old payloads. Kotlin 2.4 changed the default module name, and
 * `WireBuildPlugin` pins it back to the project name. This test fails if that pin stops working.
 */
class JvmModuleNameTest {
  @Test fun internalAccessorsKeepTheWireRuntimeModuleName() {
    val methodNames = Message::class.java.declaredMethods.map { it.name }
    assertThat(methodNames).contains("getCachedSerializedSize\$wire_runtime")
    assertThat(methodNames).contains("setCachedSerializedSize\$wire_runtime")
  }

  @Test fun kotlinModuleFileUsesTheProjectName() {
    val moduleFile = Message::class.java.classLoader.getResource("META-INF/wire-runtime.kotlin_module")
    assertThat(moduleFile).isNotNull()
  }
}
