package com.squareup.wire.plugin.java;

import com.squareup.protoparser.ProtoFile;
import com.squareup.wire.plugin.WirePlugin;
import java.io.File;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * TODO doc me!
 * <p>
 * The API of this class is meant to mimic the builder pattern and should be used as such.
 */
public class WireJavaPlugin implements WirePlugin {
  private static final String ARG_OUT = "--out=";
  private static final String ARG_REGISTRY = "--registry=";
  private static final String ARG_NO_OPTIONS = "--no-options";

  private File outputDirectory;
  private String registryClass;
  private boolean generateOptions = true;

  /** Directory into which generated code will be placed. */
  public WireJavaPlugin setOutputDirectory(File outputDirectory) {
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

  @Override public void parseArgs(List<String> args) {
    for (String arg : args) {
      if (arg.equals(ARG_NO_OPTIONS)) {
        setGenerateOptions(false);
      } else if (arg.startsWith(ARG_REGISTRY)) {
        String registryArg = arg.substring(ARG_REGISTRY.length());
        setRegistryClass(registryArg);
      } else if (arg.startsWith(ARG_OUT)) {
        String outArg = arg.substring(ARG_OUT.length());
        setOutputDirectory(new File(outArg));
      } else {
        throw new IllegalArgumentException("Unknown argument: " + arg);
      }
    }
  }

  @Override public void run(Set<ProtoFile> protoFiles) {
    throw new UnsupportedOperationException("Not implemented."); // TODO
  }
}
