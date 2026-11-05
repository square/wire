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
import assertk.assertions.isEqualTo
import java.io.File
import java.net.URLClassLoader
import okio.ByteString.Companion.decodeHex
import org.junit.Test

/**
 * Class initialization order matters here, so each test loads the generated classes in a new class
 * loader.
 */
class SealedEnumIdentityTest {
  @Test fun identityWhenAdapterIsTouchedFirst() {
    val loader = freshClassLoader()
    assertThat(identityOf(loader).toString()).isEqualTo("UNKNOWN")
  }

  @Test fun identityWhenConstantIsTouchedFirst() {
    val loader = freshClassLoader()
    Class.forName("$NESTED_ENUM\$UNKNOWN", true, loader)
    assertThat(identityOf(loader).toString()).isEqualTo("UNKNOWN")
  }

  @Test fun decodeMapEntryWithoutValueWhenConstantIsTouchedFirst() {
    val loader = freshClassLoader()
    Class.forName("$NESTED_ENUM\$UNKNOWN", true, loader)
    // map_string_enum (tag 504) with an entry that holds only the key "a".
    val bytes = "c21f030a0161".decodeHex().toByteArray()
    val adapter = adapterOf(loader, ALL_TYPES)
    val message = adapter.javaClass.getMethod("decode", ByteArray::class.java).invoke(adapter, bytes)
    assertThat(message.toString()).contains("map_string_enum={a=UNKNOWN}")
  }

  private fun freshClassLoader(): ClassLoader {
    val urls = System.getProperty("java.class.path")
      .split(File.pathSeparator)
      .map { File(it).toURI().toURL() }
    return URLClassLoader(urls.toTypedArray(), ClassLoader.getPlatformClassLoader())
  }

  private fun adapterOf(loader: ClassLoader, className: String): Any = Class.forName(className, true, loader).getField("ADAPTER").get(null)

  private fun identityOf(loader: ClassLoader): Any? {
    val adapter = adapterOf(loader, NESTED_ENUM)
    return adapter.javaClass.getMethod("getIdentity").invoke(adapter)
  }

  private companion object {
    const val ALL_TYPES = "squareup.proto3.kotlin.alltypes.AllTypes"
    const val NESTED_ENUM = "$ALL_TYPES\$NestedEnum"
  }
}
