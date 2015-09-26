package com.squareup.wire.mojo;

import com.google.common.base.Stopwatch;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.java.JavaGenerator;
import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.MessageType;
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

  @Parameter(property = "wire.noOptions")
  private boolean noOptions;

  @Parameter(property = "wire.enumOptions")
  private String[] enumOptions;

  @Parameter(property = "wire.roots")
  private String[] roots;

  @Parameter(property = "wire.serviceFactory")
  private String serviceFactory;

  @Parameter(property = "wire.registryClass")
  private String registryClass;

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

      if (roots != null && roots.length > 0) {
        schema = retainRoots(schema);
      }

      List<String> enumOptionsList = enumOptions != null
          ? Arrays.asList(enumOptions)
          : Collections.<String>emptyList();
      JavaGenerator javaGenerator = JavaGenerator.get(schema)
          .withOptions(!noOptions, enumOptionsList)
          .withAndroid(emitAndroid);

      for (ProtoFile protoFile : schema.protoFiles()) {
        if (!protoFilesList.contains(protoFile.location().path())) {
          continue; // Don't emit anything for files not explicitly compiled.
        }

        for (Type type : protoFile.types()) {
          Stopwatch stopwatch = Stopwatch.createStarted();
          ClassName javaTypeName = (ClassName) javaGenerator.typeName(type.name());
          TypeSpec typeSpec = type instanceof MessageType
              ? javaGenerator.generateMessage((MessageType) type)
              : javaGenerator.generateEnum((EnumType) type);
          writeJavaFile(javaTypeName, typeSpec, type.location());
          getLog().info(String.format("Generated %s in %s", javaTypeName, stopwatch));
        }

        if (!protoFile.extendList().isEmpty()) {
          Stopwatch stopwatch = Stopwatch.createStarted();
          ClassName javaTypeName = javaGenerator.extensionsClass(protoFile);
          TypeSpec typeSpec = javaGenerator.generateExtensionsClass(javaTypeName, protoFile);
          writeJavaFile(javaTypeName, typeSpec, protoFile.location());
          getLog().info(String.format("Generated extensions %s in %s", javaTypeName, stopwatch));
        }
      }

      if (registryClass != null) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ClassName className = ClassName.bestGuess(registryClass);
        TypeSpec typeSpec = javaGenerator.generateRegistry(className);
        writeJavaFile(className, typeSpec, null);
        getLog().info(String.format("Generated registry %s in %s", className, stopwatch));
      }
    } catch (Exception e) {
      throw new MojoExecutionException("Wire Plugin: Failure compiling proto sources.", e);
    }
  }

  private Schema retainRoots(Schema schema) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    int oldSize = countTypes(schema);

    Schema prunedSchema = schema.retainRoots(Arrays.asList(roots));
    int newSize = countTypes(prunedSchema);

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
