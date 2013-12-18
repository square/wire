package com.squareup.wire.maven;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.squareup.wire.WireCompiler;
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
    List<String> args = Lists.newLinkedList();
    args.add("--proto_path=" + protoSourceDirectory);
    args.add("--java_out=" + generatedSourceDirectory);
    args.add(Joiner.on(" ").join(protoFiles));

    getLog().info("Invoking wire compiler with options:");
    getLog().info(args.toString());
    try {
      // TODO(shawn) we don't have a great programatic interface to the compiler.
      // Not all exceptions should result in MojoFailureExceptions (i.e. bugs in this plugin that
      // invoke the compiler incorrectly).
      String[] strArgs = new String[args.size()];
      WireCompiler.main(args.toArray(strArgs));
    } catch (Exception e) {
      throw new MojoExecutionException("Wire Plugin: Failure compiling proto sources.", e);
    }
  }

  public void setProtoSourceDirectory(String protoSourceDirectory) {
    this.protoSourceDirectory = protoSourceDirectory;
  }

  public void setProtoFiles(String[] protoFiles) {
    this.protoFiles = protoFiles;
  }

  public void setGeneratedSourceDirectory(String generatedSourceDirectory) {
    this.generatedSourceDirectory = generatedSourceDirectory;
  }

  public void setProject(MavenProject project) {
    this.project = project;
  }
}
