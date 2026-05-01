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
package com.squareup.wire.gradle

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.squareup.wire.schema.CustomTarget
import com.squareup.wire.schema.Extend
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.SchemaHandler
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import okio.Path
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class WireOutputProviderTest {
  @Test
  fun customOutputResolvesProviderBackedProperties() {
    val project = ProjectBuilder.builder().build()
    val output = project.objects.newInstance(CustomOutput::class.java)

    output.out.set(project.providers.provider { "build/generated/custom-wire" })
    output.includes.set(project.providers.provider { listOf("squareup.dinosaurs.Dinosaur") })
    output.excludes.set(project.providers.provider { listOf("squareup.geology.Period") })
    output.exclusive.set(project.providers.provider { false })
    output.options.set(project.providers.provider { mapOf("a" to "one", "b" to "two") })
    output.schemaHandlerFactory.set(TestSchemaHandlerFactory())

    assertThat(output.out.orNull).isEqualTo("build/generated/custom-wire")

    val target = output.toTarget(output.out.get()) as CustomTarget

    assertThat(target.outDirectory).isEqualTo("build/generated/custom-wire")
    assertThat(target.includes).containsExactly("squareup.dinosaurs.Dinosaur")
    assertThat(target.excludes).containsExactly("squareup.geology.Period")
    assertThat(target.exclusive).isFalse()
    assertThat(target.options).isEqualTo(mapOf("a" to "one", "b" to "two"))
    assertThat(target.newHandler()::class.java).isEqualTo(TestSchemaHandler::class.java)
  }

  @Test
  fun customOutputExclusiveFallsBackToConventionWhenProviderIsAbsent() {
    val project = ProjectBuilder.builder().build()
    val output = project.objects.newInstance(CustomOutput::class.java)

    output.exclusive.set(false)
    output.exclusive.set(project.providers.gradleProperty("missing-exclusive").map(String::toBoolean))
    output.schemaHandlerFactory.set(TestSchemaHandlerFactory())

    val target = output.toTarget("build/generated/custom-wire") as CustomTarget

    assertThat(target.exclusive).isEqualTo(true)
  }
}

private class TestSchemaHandlerFactory : SchemaHandler.Factory {
  override fun create(
    includes: List<String>,
    excludes: List<String>,
    exclusive: Boolean,
    outDirectory: String,
    options: Map<String, String>,
  ): SchemaHandler = TestSchemaHandler()
}

private class TestSchemaHandler : SchemaHandler() {
  override fun handle(type: Type, context: Context): Path? = null

  override fun handle(service: Service, context: Context): List<Path> = listOf()

  override fun handle(extend: Extend, field: Field, context: Context): Path? = null
}
