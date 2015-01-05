package com.squareup.wire;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;


final class CommandLineOptions {
  public static final String PROTO_PATH_FLAG = "--proto_path=";
  public static final String JAVA_OUT_FLAG = "--java_out=";
  public static final String FILES_FLAG = "--files=";
  public static final String REGISTRY_CLASS_FLAG = "--registry_class=";
  public static final String ROOTS_FLAG = "--roots=";
  public static final String NO_OPTIONS_FLAG = "--no_options";
  public static final String ENUM_OPTIONS_FLAG = "--enum_options=";
  public static final String SERVICE_WRITER_FLAG = "--service_writer=";
  public static final String SERVICE_WRITER_OPT_FLAG = "--service_writer_opt=";

  final String protoPath;
  final List<String> sourceFileNames;
  final List<String> roots;
  final String javaOut;
  final String registryClass;
  final boolean emitOptions;
  final Set<String> enumOptions;
  final String serviceWriter;
  final List<String> serviceWriterOptions;

  CommandLineOptions(String protoPath, List<String> sourceFileNames, List<String> roots,
      String javaOut, String registryClass, boolean emitOptions, Set<String> enumOptions,
      String serviceWriter, List<String> serviceWriterOptions) {
    this.protoPath = protoPath;
    this.sourceFileNames = sourceFileNames;
    this.roots = roots;
    this.javaOut = javaOut;
    this.registryClass = registryClass;
    this.emitOptions = emitOptions;
    this.enumOptions = enumOptions;
    this.serviceWriter = serviceWriter;
    this.serviceWriterOptions = serviceWriterOptions;
  }

  /**
   *
   * <pre>
   * java WireCompiler --proto_path=&lt;path&gt; --java_out=&lt;path&gt;
   *     [--files=&lt;protos.include&gt;] [--roots=&lt;message_name&gt;[,&lt;message_name&gt;...]]
   *     [--registry_class=&lt;class_name&gt;] [--no_options]
   *     [--enum_options=&lt;option_name&gt;[,&lt;option_name&gt;...]]
   *     [--service_writer=&lt;class_name&gt;]
   *     [--service_writer_opt=&lt;value&gt;] [--service_writer_opt=&lt;value&gt;]...]
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
   */
  CommandLineOptions(String... args) throws FileNotFoundException {
    int index = 0;

    List<String> sourceFileNames = new ArrayList<String>();
    List<String> serviceWriterOptions = new ArrayList<String>();
    List<String> roots = new ArrayList<String>();
    boolean emitOptions = true;
    String protoPath = null;
    String javaOut = null;
    String registryClass = null;
    List<String> enumOptionsList = new ArrayList<String>();
    String serviceWriter = null;

    while (index < args.length) {
      if (args[index].startsWith(PROTO_PATH_FLAG)) {
        protoPath = args[index].substring(PROTO_PATH_FLAG.length());
      } else if (args[index].startsWith(JAVA_OUT_FLAG)) {
        javaOut = args[index].substring(JAVA_OUT_FLAG.length());
      } else if (args[index].startsWith(FILES_FLAG)) {
        File files = new File(args[index].substring(FILES_FLAG.length()));
        String[] fileNames = new Scanner(files, "UTF-8").useDelimiter("\\A").next().split("\n");
        sourceFileNames.addAll(Arrays.asList(fileNames));
      } else if (args[index].startsWith(ROOTS_FLAG)) {
        roots.addAll(splitArg(args[index], ROOTS_FLAG.length()));
      } else if (args[index].startsWith(REGISTRY_CLASS_FLAG)) {
        registryClass = args[index].substring(REGISTRY_CLASS_FLAG.length());
      } else if (args[index].equals(NO_OPTIONS_FLAG)) {
        emitOptions = false;
      } else if (args[index].startsWith(ENUM_OPTIONS_FLAG)) {
        enumOptionsList.addAll(splitArg(args[index], ENUM_OPTIONS_FLAG.length()));
      } else if (args[index].startsWith(SERVICE_WRITER_FLAG)) {
        serviceWriter = args[index].substring(SERVICE_WRITER_FLAG.length());
      } else if (args[index].startsWith(SERVICE_WRITER_OPT_FLAG)) {
        serviceWriterOptions.add(args[index].substring(SERVICE_WRITER_OPT_FLAG.length()));
      } else {
        sourceFileNames.add(args[index]);
      }
      index++;
    }

    this.protoPath = protoPath;
    this.sourceFileNames = sourceFileNames;
    this.roots = roots;
    this.javaOut = javaOut;
    this.registryClass = registryClass;
    this.emitOptions = emitOptions;
    this.enumOptions = new LinkedHashSet<String>(enumOptionsList);
    this.serviceWriter = serviceWriter;
    this.serviceWriterOptions = serviceWriterOptions;
  }

  private static List<String> splitArg(String arg, int flagLength) {
    return Arrays.asList(arg.substring(flagLength).split(","));
  }
}
