package com.squareup.wire.mojo;

import com.google.common.base.Stopwatch;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.java.JavaGenerator;
import com.squareup.wire.java.Profile;
import com.squareup.wire.java.ProfileLoader;
import com.squareup.wire.schema.PruningRules;
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
      Profile profile = loadProfile(schema);

      PruningRules pruningRules = identifierSet();
      if (!pruningRules.isEmpty()) {
        schema = retainRoots(pruningRules, schema);
      }

      JavaGenerator javaGenerator = JavaGenerator.get(schema)
          .withAndroid(emitAndroid)
          .withCompact(emitCompact)
          .withProfile(profile);

      for (ProtoFile protoFile : schema.getProtoFiles()) {
        if (!protoFilesList.isEmpty()
            && !protoFilesList.contains(protoFile.getLocation().getPath())) {
          continue; // Don't emit anything for files not explicitly compiled.
        }

        for (Type type : protoFile.getTypes()) {
          Stopwatch stopwatch = Stopwatch.createStarted();
          TypeSpec typeSpec = javaGenerator.generateType(type);
          ClassName javaTypeName = javaGenerator.generatedTypeName(type);
          writeJavaFile(javaTypeName, typeSpec, type.getLocation().withPathOnly());
          getLog().info(String.format("Generated %s in %s", javaTypeName, stopwatch));
        }
      }
    } catch (Exception e) {
      throw new MojoExecutionException("Wire Plugin: Failure compiling proto sources.", e);
    }
  }

  private PruningRules identifierSet() {
    PruningRules.Builder identifierSetBuilder = new PruningRules.Builder();
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

  private Schema retainRoots(PruningRules pruningRules, Schema schema) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    int oldSize = countTypes(schema);

    Schema prunedSchema = schema.prune(pruningRules);
    int newSize = countTypes(prunedSchema);

    for (String rule : pruningRules.unusedIncludes()) {
      getLog().warn(String.format("Unused include: %s", rule));
    }
    for (String rule : pruningRules.unusedExcludes()) {
      getLog().warn(String.format("Unused exclude: %s", rule));
    }

    getLog().info(String.format("Pruned schema from %s types to %s types in %s",
        oldSize, newSize, stopwatch));

    return prunedSchema;
  }

  private int countTypes(Schema prunedSchema) {
    int result = 0;
    for (ProtoFile protoFile : prunedSchema.getProtoFiles()) {
      result += protoFile.getTypes().size();
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
        schema.getProtoFiles().size(), stopwatch));

    return schema;
  }

  private Profile loadProfile(Schema schema) throws IOException {
    String profileName = emitAndroid ? "android" : "java";
    return  new ProfileLoader(profileName)
        .schema(schema)
        .load();
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
