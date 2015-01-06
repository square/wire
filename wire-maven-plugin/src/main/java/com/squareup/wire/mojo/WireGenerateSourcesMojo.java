package com.squareup.wire.mojo;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.squareup.wire.WireCompiler;
import java.util.Collections;
import java.util.List;
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
    List<String> args = Lists.newArrayList();
    args.add("--proto_path=" + protoSourceDirectory);
    args.add("--java_out=" + generatedSourceDirectory);
    if (noOptions) {
      args.add("--no_options");
    }
    if (enumOptions != null && enumOptions.length > 0) {
      args.add("--enum_options=" + Joiner.on(',').join(enumOptions));
    }
    if (registryClass != null) {
      args.add("--registry_class=" + registryClass);
    }
    if (roots != null && roots.length > 0) {
      args.add("--roots=" + Joiner.on(',').join(roots));
    }
    if (serviceWriter != null) {
      args.add("--service_writer=" + serviceWriter);
    }
    Collections.addAll(args, protoFiles);

    getLog().info("Invoking wire compiler with arguments:");
    getLog().info(Joiner.on('\n').join(args));
    try {
      // TODO(shawn) we don't have a great programatic interface to the compiler.
      // Not all exceptions should result in MojoFailureExceptions (i.e. bugs in this plugin that
      // invoke the compiler incorrectly).
      WireCompiler.main(args.toArray(new String[args.size()]));

      // Add the directory into which generated sources are placed as a compiled source root.
      project.addCompileSourceRoot(generatedSourceDirectory);
    } catch (Exception e) {
      throw new MojoExecutionException("Wire Plugin: Failure compiling proto sources.", e);
    }
  }
}
