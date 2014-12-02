package com.squareup.wire.mojo;

import com.squareup.wire.WireCompiler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * A maven mojo that triggers the Wire compiler.
 */
@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class WireGenerateSourcesMojo extends AbstractMojo {

  /**
   * The root of the proto source directory.
   */
  @Parameter(
      property = "wire.protoSourceDirectory",
      defaultValue = "${project.basedir}/src/main/proto")
  private String protoSourceDirectory;

  @Parameter(property = "wire.noOptions")
  private boolean noOptions;

  @Parameter(property = "wire.enumOptions")
  private String[] enumOptions;

  @Parameter(property = "wire.roots")
  private String[] roots;

  @Parameter(property = "wire.serviceWriter")
  private String serviceWriter;

  @Parameter(property = "wire.registryClass")
  private String registryClass;

  /**
   * List of proto files to compile relative to ${protoSourceDirectory}.
   */
  @Parameter(property = "wire.protoFiles", required = true)
  private String[] protoFiles;

  @Parameter(
      property = "wire.generatedSourceDirectory",
      defaultValue = "${project.build.directory}/generated-sources/wire")
  private String generatedSourceDirectory;

  @Parameter(
      defaultValue = "${project}",
      required = true,
      readonly = true)
  private MavenProject project;

  @Override public void execute() throws MojoExecutionException, MojoFailureException {
    project.addCompileSourceRoot(generatedSourceDirectory);

    //TODO(shawn) only compile when things have changed.
    compileProtos();
  }

  private void compileProtos() throws MojoExecutionException {
    WireCompiler.Builder builder = new WireCompiler.Builder()
            .protoPath(protoSourceDirectory)
            .addSourceFileNames(protoFiles)
            .outputDirectory(generatedSourceDirectory)
            .emitOptions(!noOptions)
            .registryClass(registryClass);
    if (enumOptions != null && enumOptions.length > 0) {
      builder.addEnumOptions(enumOptions);
    }
    if (registryClass != null) {
      builder.registryClass(registryClass);
    }
    if (roots != null && roots.length > 0) {
      builder.addTypesToEmit(roots);
    }
    if (serviceWriter != null) {
      builder.serviceWriter(serviceWriter);
    }

    getLog().info("Invoking wire compiler");
    try {
      // Not all exceptions should result in MojoFailureExceptions (i.e. bugs in this plugin that
      // invoke the compiler incorrectly).
      builder.build().compile();

      // Add the directory into which generated sources are placed as a compiled source root.
      project.addCompileSourceRoot(generatedSourceDirectory);
    } catch (Exception e) {
      throw new MojoExecutionException("Wire Plugin: Failure compiling proto sources.", e);
    }
  }
}
