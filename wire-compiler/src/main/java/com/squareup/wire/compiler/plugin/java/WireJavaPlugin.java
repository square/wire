package com.squareup.wire.compiler.plugin.java;

import com.google.auto.service.AutoService;
import com.squareup.protoparser.ProtoFile;
import com.squareup.wire.compiler.plugin.WirePlugin;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@AutoService(WirePlugin.class)
public class WireJavaPlugin implements WirePlugin {
  private Path outputDirectory;
  private String registryClass;
  private boolean generateOptions = true;

  /** Directory into which generated code will be placed. */
  public WireJavaPlugin setOutputDirectory(Path outputDirectory) {
    checkNotNull(outputDirectory, "Output directory must not be null.");
    this.outputDirectory = outputDirectory;
    return this;
  }

  /**
   * A fully-qualified Java class name which will be generated containing a constant list of all
   * extension classes. This list is suitable for passing to
   * {@link com.squareup.wire.Wire#Wire(List) Wire's constructor} at runtime for constructing its
   * internal extension registry.
   */
  public WireJavaPlugin setRegistryClass(String registryClass) {
    checkNotNull(registryClass, "Registry class must not be null.");
    checkArgument(!registryClass.trim().isEmpty(), "Registry class must not be blank.");
    this.registryClass = registryClass;
    return this;
  }

  /**
   * Control whether code will be emitted for options on messages and fields. The presence of
   * options on a message will result in a static member named {@code MESSAGE_OPTIONS}, initialized
   * with the options and their values. The presence of options on a field (other than the standard
   * options "default", "deprecated", and "packed") will result in a static member named
   * {@code FIELD_OPTIONS_&lt;field name>} in the generated code, initialized with the field option
   * values.
   */
  public WireJavaPlugin setGenerateOptions(boolean generateOptions) {
    this.generateOptions = generateOptions;
    return this;
  }

  @Override public void generate(FileSystem fs, Set<ProtoFile> protoFiles) {
    throw new UnsupportedOperationException("Not implemented."); // TODO
  }
}
