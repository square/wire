package com.squareup.wire.mojo;

import com.google.common.base.Stopwatch;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.java.JavaGenerator;
import com.squareup.wire.schema.IdentifierSet;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;
import com.squareup.wire.schema.Type;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/** A maven mojo that executes Wire's JavaGenerator. */
@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class WireGenerateSourcesMojo extends AbstractMojo {
  /** The root of the proto source directory. */
  @Parameter(
      property = "wire.protoSourceDirectory",
      defaultValue = "${project.basedir}/src/main/proto")
  private String protoSourceDirectory;

  @Parameter(property = "wire.protoPaths")
  private String[] protoPaths;

  @Parameter(property = "wire.android")
  private boolean emitAndroid;

  @Parameter(property = "wire.compact")
  private boolean emitCompact;

  @Parameter(property = "wire.includes")
  private String[] includes;

  @Parameter(property = "wire.excludes")
  private String[] excludes;

  @Parameter(property = "wire.serviceFactory")
  private String serviceFactory;

  /** List of proto files to compile relative to ${protoPaths}. */
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
    // Add the directory into which generated sources are placed as a compiled source root.
    project.addCompileSourceRoot(generatedSourceDirectory);

    try {
      List<String> directories = protoPaths != null && protoPaths.length > 0
          ? Arrays.asList(protoPaths)
          : Collections.singletonList(protoSourceDirectory);
      List<String> protoFilesList = Arrays.asList(protoFiles);
      Schema schema = loadSchema(directories, protoFilesList);

      IdentifierSet identifierSet = identifierSet();
      if (!identifierSet.isEmpty()) {
        schema = retainRoots(identifierSet, schema);
      }

      JavaGenerator javaGenerator = JavaGenerator.get(schema)
          .withAndroid(emitAndroid)
          .withCompact(emitCompact);

      for (ProtoFile protoFile : schema.protoFiles()) {
        if (!protoFilesList.contains(protoFile.location().path())) {
          continue; // Don't emit anything for files not explicitly compiled.
        }

        for (Type type : protoFile.types()) {
          Stopwatch stopwatch = Stopwatch.createStarted();
          TypeSpec typeSpec = javaGenerator.generateType(type);
          ClassName javaTypeName = (ClassName) javaGenerator.typeName(type.type());
          writeJavaFile(javaTypeName, typeSpec, type.location().withoutBase());
          getLog().info(String.format("Generated %s in %s", javaTypeName, stopwatch));
        }
      }
    } catch (Exception e) {
      throw new MojoExecutionException("Wire Plugin: Failure compiling proto sources.", e);
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

  private Schema retainRoots(IdentifierSet identifierSet, Schema schema) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    int oldSize = countTypes(schema);

    Schema prunedSchema = schema.prune(identifierSet);
    int newSize = countTypes(prunedSchema);

    for (String rule : identifierSet.unusedIncludes()) {
      getLog().warn(String.format("Unused include: %s", rule));
    }
    for (String rule : identifierSet.unusedExcludes()) {
      getLog().warn(String.format("Unused exclude: %s", rule));
    }

    getLog().info(String.format("Pruned schema from %s types to %s types in %s",
        oldSize, newSize, stopwatch));

    return prunedSchema;
  }

  private int countTypes(Schema prunedSchema) {
    int result = 0;
    for (ProtoFile protoFile : prunedSchema.protoFiles()) {
      result += protoFile.types().size();
    }
    return result;
  }

  private Schema loadSchema(List<String> directories, List<String> protos) throws IOException {
    Stopwatch stopwatch = Stopwatch.createStarted();

    SchemaLoader schemaLoader = new SchemaLoader();
    for (String directory : directories) {
      schemaLoader.addSource(new File(directory));
    }
    for (String proto : protos) {
      schemaLoader.addProto(proto);
    }
    Schema schema = schemaLoader.load();

    getLog().info(String.format("Loaded %s proto files in %s",
        schema.protoFiles().size(), stopwatch));

    return schema;
  }

  private void writeJavaFile(ClassName javaTypeName, TypeSpec typeSpec, Location location)
      throws IOException {
    JavaFile.Builder builder = JavaFile.builder(javaTypeName.packageName(), typeSpec)
        .addFileComment("$L", "Code generated by Wire protocol buffer compiler, do not edit.");
    if (location != null) {
      builder.addFileComment("\nSource file: $L", location);
    }
    JavaFile javaFile = builder.build();
    try {
      javaFile.writeTo(new File(generatedSourceDirectory));
    } catch (IOException e) {
      throw new IOException("Failed to write " + javaFile.packageName + "."
          + javaFile.typeSpec.name + " to " + generatedSourceDirectory, e);
    }
  }
}
