/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.sample

import com.squareup.wire.schema.IdentifierSet
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.IOException

/** Maven plugin interface to CodegenSample.  */
@Mojo(
    name = "generate-sources",
    defaultPhase = GENERATE_SOURCES
)
class CodegenSampleMojo : AbstractMojo(), CodegenSample.Log {
  @Parameter(property = "codgenSample.protoPaths")
  private lateinit var protoPaths: Array<String>

  /** List of proto files to compile relative to ${protoPaths}.  */
  @Parameter(property = "codgenSample.protoFiles", required = true)
  private lateinit var protoFiles: Array<String>

  @Parameter(property = "codgenSample.includes")
  private val includes: Array<String>? = null

  @Parameter(property = "codgenSample.excludes")
  private val excludes: Array<String>? = null

  @Parameter(
      property = "codgenSample.generatedSourceDirectory",
      defaultValue = "\${project.build.directory}/generated-sources/codgenSample"
  )
  private lateinit var generatedSourceDirectory: String

  @Parameter(defaultValue = "\${project}", required = true, readonly = true)
  private lateinit var project: MavenProject

  @Throws(
      MojoExecutionException::class,
      MojoFailureException::class
  )
  override fun execute() {
    project.addCompileSourceRoot(generatedSourceDirectory)

    val protoPathsSet = protoPaths.toSet()
    val protoFilesSet = protoFiles.toSet()
    val identifierSet = identifierSet()

    try {
      val codeGenerator =
        CodegenSample(this, protoPathsSet, protoFilesSet, generatedSourceDirectory, identifierSet)
      codeGenerator.execute()
    } catch (e: IOException) {
      throw MojoExecutionException("failed to generate sources", e)
    }
  }

  private fun identifierSet(): IdentifierSet {
    val identifierSetBuilder = IdentifierSet.Builder()
    if (includes != null) {
      for (identifier in includes) {
        identifierSetBuilder.include(identifier)
      }
    }
    if (excludes != null) {
      for (identifier in excludes) {
        identifierSetBuilder.exclude(identifier)
      }
    }
    return identifierSetBuilder.build()
  }

  override fun info(
    format: String,
    vararg args: Any
  ) = log.info(String.format(format, *args))
}
