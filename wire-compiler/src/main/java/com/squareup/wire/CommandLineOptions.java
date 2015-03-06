package com.squareup.wire;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public final class CommandLineOptions {
  public static final String PROTO_PATH_FLAG = "--proto_path=";
  public static final String JAVA_OUT_FLAG = "--java_out=";
  public static final String FILES_FLAG = "--files=";
  public static final String ROOTS_FLAG = "--roots=";
  public static final String REGISTRY_CLASS_FLAG = "--registry_class=";
  public static final String NO_OPTIONS_FLAG = "--no_options";
  public static final String ENUM_OPTIONS_FLAG = "--enum_options=";
  public static final String SERVICE_WRITER_FLAG = "--service_writer=";
  public static final String SERVICE_WRITER_OPT_FLAG = "--service_writer_opt=";
  public static final String QUIET_FLAG = "--quiet";
  public static final String DRY_RUN_FLAG = "--dry_run";

  final String protoPath;
  final String javaOut;
  final List<String> sourceFileNames;
  final List<String> roots;
  final String registryClass;
  final boolean emitOptions;
  final Set<String> enumOptions;
  final String serviceWriter;
  final List<String> serviceWriterOptions;
  final boolean quiet;
  final boolean dryRun;

  CommandLineOptions(String protoPath, String javaOut,
      List<String> sourceFileNames, List<String> roots,
      String registryClass, boolean emitOptions,
      Set<String> enumOptions,
      String serviceWriter,
      List<String> serviceWriterOptions,
      boolean quiet,
      boolean dryRun) {
    this.protoPath = protoPath;
    this.javaOut = javaOut;
    this.sourceFileNames = sourceFileNames;
    this.roots = roots;
    this.registryClass = registryClass;
    this.emitOptions = emitOptions;
    this.enumOptions = enumOptions;
    this.serviceWriter = serviceWriter;
    this.serviceWriterOptions = serviceWriterOptions;
    this.quiet = quiet;
    this.dryRun = dryRun;
  }

  /**
   * Usage:
   *
   * <pre>
   * java WireCompiler --proto_path=&lt;path&gt; --java_out=&lt;path&gt;
   *     [--files=&lt;protos.include&gt;] [--roots=&lt;message_name&gt;[,&lt;message_name&gt;...]]
   *     [--registry_class=&lt;class_name&gt;] [--no_options]
   *     [--enum_options=&lt;option_name&gt;[,&lt;option_name&gt;...]]
   *     [--service_writer=&lt;class_name&gt;]
   *     [--service_writer_opt=&lt;value&gt;] [--service_writer_opt=&lt;value&gt;]...]
   *     [--quiet] [--dry_run]
   *     [file [file...]]
   * </pre>
   *
   * If the {@code --roots} flag is present, its argument must be a comma-separated list
   * of fully-qualified message or enum names. The output will be limited to those messages
   * and enums that are (transitive) dependencies of the listed names.  If you are using
   * {@code --service_writer} to generate an interface for a Service, your roots can also take the
   * form 'fully.qualified.Service#MethodName` to limit what endpoints are generated.
   * <p>
   * If the {@code --registry_class} flag is present, its argument must be a Java class name. A
   * class with the given name will be generated, containing a constant list of all extension
   * classes generated during the compile. This list is suitable for passing to Wire's constructor
   * at runtime for constructing its internal extension registry.
   * <p>
   * Unless the {@code --no_options} flag is supplied, code will be emitted for options on messages
   * and fields.  The presence of options on a message will result in a static member named
   * "MESSAGE_OPTIONS", initialized with the options and their values.   The presence of options on
   * a field (other than the standard options "default", "deprecated", and "packed") will result in
   * a static member named "FIELD_OPTIONS_&lt;field name&gt;" in the generated code, initialized
   * with the field option values.
   * <p>
   * Regardless of the value of the {@code --no_options} flag, code will be emitted for all
   * enum value options listed in the {@code --enum_options} flag. The resulting code will contain
   * a public static field for each option used within a particular enum type.
   * </p>
   * <p>
   * If {@code --quiet} is specified, diagnostic messages to stdout are suppressed.
   * </p>
   * <p>
   * The {@code --dry_run} flag causes the compile to just emit the names of the source files that
   * would be generated to stdout.
   */
  public CommandLineOptions(String... args) throws WireException {
    int index = 0;
    Builder builder = new Builder();

    while (index < args.length) {
      if (args[index].startsWith(PROTO_PATH_FLAG)) {
        builder.protoPath(args[index].substring(PROTO_PATH_FLAG.length()));
      } else if (args[index].startsWith(JAVA_OUT_FLAG)) {
        builder.javaOut(args[index].substring(JAVA_OUT_FLAG.length()));
      } else if (args[index].startsWith(FILES_FLAG)) {
        File files = new File(args[index].substring(FILES_FLAG.length()));
        String[] fileNames;
        try {
          fileNames = new Scanner(files, "UTF-8").useDelimiter("\\A").next().split("\n");
        } catch (FileNotFoundException ex) {
          throw new WireException("Error processing argument " + args[index], ex);
        }
        builder.addSourceFileNames(Arrays.asList(fileNames));
      } else if (args[index].startsWith(ROOTS_FLAG)) {
        builder.addRoots(splitArg(args[index], ROOTS_FLAG.length()));
      } else if (args[index].startsWith(REGISTRY_CLASS_FLAG)) {
        builder.registryClass(args[index].substring(REGISTRY_CLASS_FLAG.length()));
      } else if (args[index].equals(NO_OPTIONS_FLAG)) {
        builder.emitOptions(false);
      } else if (args[index].startsWith(ENUM_OPTIONS_FLAG)) {
        builder.addEnumOptions(splitArg(args[index], ENUM_OPTIONS_FLAG.length()));
      } else if (args[index].startsWith(SERVICE_WRITER_FLAG)) {
        builder.serviceWriter(args[index].substring(SERVICE_WRITER_FLAG.length()));
      } else if (args[index].startsWith(SERVICE_WRITER_OPT_FLAG)) {
        builder.addServiceWriterOption(args[index].substring(SERVICE_WRITER_OPT_FLAG.length()));
      } else if (args[index].startsWith(QUIET_FLAG)) {
        builder.quiet(true);
      } else if (args[index].startsWith(DRY_RUN_FLAG)) {
        builder.dryRun(true);
      } else {
        builder.addSourceFileName(args[index]);
      }
      index++;
    }

    this.protoPath = builder.protoPath;
    this.javaOut = builder.javaOut;
    this.sourceFileNames = builder.sourceFileNames;
    this.roots = builder.roots;
    this.registryClass = builder.registryClass;
    this.emitOptions = builder.emitOptions;
    this.enumOptions = builder.enumOptions;
    this.serviceWriter = builder.serviceWriter;
    this.serviceWriterOptions = builder.serviceWriterOptions;
    this.quiet = builder.quiet;
    this.dryRun = builder.dryRun;
  }

  private static List<String> splitArg(String arg, int flagLength) {
    return Arrays.asList(arg.substring(flagLength).split(","));
  }

  public static class Builder {
    private String protoPath = null;
    private String javaOut = null;
    private List<String> sourceFileNames = new ArrayList<String>();
    private List<String> roots = new ArrayList<String>();
    private String registryClass = null;
    private boolean emitOptions = true;
    private Set<String> enumOptions = new LinkedHashSet<String>();
    private String serviceWriter = null;
    private List<String> serviceWriterOptions = new ArrayList<String>();
    private boolean quiet = false;
    private boolean dryRun = false;

    public Builder() {
    }

    public Builder protoPath(String protoPath) {
      this.protoPath = protoPath;
      return this;
    }

    public Builder javaOut(String javaOut) {
      this.javaOut = javaOut;
      return this;
    }

    public Builder addSourceFileName(String sourceFileName) {
      sourceFileNames.add(sourceFileName);
      return this;
    }

    public Builder addSourceFileNames(String... sourceFileNames) {
      Collections.addAll(this.sourceFileNames, sourceFileNames);
      return this;
    }

    public Builder addSourceFileNames(Collection<String> sourceFileNames) {
      sourceFileNames.addAll(sourceFileNames);
      return this;
    }

    public Builder addRoot(String root) {
      roots.add(root);
      return this;
    }

    public Builder addRoots(String... roots) {
      Collections.addAll(this.roots, roots);
      return this;
    }

    public Builder addRoots(Collection<String> roots) {
      this.roots.addAll(roots);
      return this;
    }

    public Builder registryClass(String registryClass) {
      this.registryClass = registryClass;
      return this;
    }

    public Builder emitOptions(boolean emitOptions) {
      this.emitOptions = emitOptions;
      return this;
    }

    public Builder addEnumOption(String enumOption) {
      enumOptions.add(enumOption);
      return this;
    }

    public Builder addEnumOptions(String... enumOptions) {
      Collections.addAll(this.enumOptions, enumOptions);
      return this;
    }

    public Builder addEnumOptions(Collection<String> enumOptions) {
      this.enumOptions.addAll(enumOptions);
      return this;
    }

    public Builder serviceWriter(String serviceWriter) {
      this.serviceWriter = serviceWriter;
      return this;
    }

    public Builder addServiceWriterOption(String serviceWriterOption) {
      this.serviceWriterOptions.add(serviceWriterOption);
      return this;
    }

    public Builder addServiceWriterOptions(String... serviceWriterOptions) {
      Collections.addAll(this.serviceWriterOptions, serviceWriterOptions);
      return this;
    }

    public Builder addServiceWriterOptions(Collection<String> serviceWriterOptions) {
      this.serviceWriterOptions.addAll(serviceWriterOptions);
      return this;
    }

    public Builder quiet(boolean quiet) {
      this.quiet = quiet;
      return this;
    }

    public Builder dryRun(boolean dryRun) {
      this.dryRun = dryRun;
      return this;
    }

    public CommandLineOptions build() {
      return new CommandLineOptions(protoPath, javaOut, sourceFileNames, roots, registryClass,
          emitOptions, enumOptions, serviceWriter, serviceWriterOptions, quiet, dryRun);
    }
  }
}
