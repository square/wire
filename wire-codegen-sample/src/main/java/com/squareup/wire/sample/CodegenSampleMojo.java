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
package com.squareup.wire.sample;

import com.google.common.collect.ImmutableSet;
import com.squareup.wire.schema.IdentifierSet;
import java.io.IOException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/** Maven plugin interface to CodegenSample. */
@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodegenSampleMojo extends AbstractMojo implements CodegenSample.Log {
  @Parameter(property = "codgenSample.protoPaths")
  private String[] protoPaths;

  /** List of proto files to compile relative to ${protoPaths}. */
  @Parameter(property = "codgenSample.protoFiles", required = true)
  private String[] protoFiles;

  @Parameter(property = "codgenSample.includes")
  private String[] includes;

  @Parameter(property = "codgenSample.excludes")
  private String[] excludes;

  @Parameter(
      property = "codgenSample.generatedSourceDirectory",
      defaultValue = "${project.build.directory}/generated-sources/codgenSample")
  private String generatedSourceDirectory;

  @Parameter(
      defaultValue = "${project}",
      required = true,
      readonly = true)
  private MavenProject project;

  @Override public void execute() throws MojoExecutionException, MojoFailureException {
    project.addCompileSourceRoot(generatedSourceDirectory);

    ImmutableSet<String> protoPathsSet = ImmutableSet.copyOf(protoPaths);
    ImmutableSet<String> protoFilesSet = ImmutableSet.copyOf(protoFiles);
    IdentifierSet identifierSet = identifierSet();

    try {
      CodegenSample codeGenerator = new CodegenSample(
          this, protoPathsSet, protoFilesSet, generatedSourceDirectory, identifierSet);
      codeGenerator.execute();
    } catch (IOException e) {
      throw new MojoExecutionException("failed to generate sources", e);
    }
  }

  private IdentifierSet identifierSet() {
    IdentifierSet.Builder identifierSetBuilder = new IdentifierSet.Builder();
    if (includes != null) {
      for (String identifier : includes) {
        identifierSetBuilder.include(identifier);
      }
    }
    if (excludes != null) {
      for (String identifier : excludes) {
        identifierSetBuilder.exclude(identifier);
      }
    }
    return identifierSetBuilder.build();
  }

  @Override public void info(String format, Object... args) {
    getLog().info(String.format(format, args));
  }
}
